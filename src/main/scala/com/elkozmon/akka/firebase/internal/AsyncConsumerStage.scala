/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.scaladsl.Consumer.Control
import com.google.firebase.database._

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

private[firebase] class AsyncConsumerStage(
  sourceNode: DatabaseReference,
  bufferSize: Int
) extends GraphStageWithMaterializedValue[SourceShape[Future[Document]], Control] {

  require(bufferSize > 0, "bufferSize must be greater than 0.")

  private val out = Outlet[Future[Document]]("out")

  override def shape: SourceShape[Future[Document]] = SourceShape.of(out)

  @scala.throws[Exception](classOf[Exception])
  override def createLogicAndMaterializedValue(
    inheritedAttributes: Attributes
  ): (GraphStageLogic, Control) = {
    val logic = new GraphStageLogicWithLogging(shape)
      with OutHandler
      with ChildEventListener
      with PromiseConsumerControl {

      this.setHandler(out, this)

      private val query = sourceNode.orderByKey().limitToFirst(bufferSize)

      private val snapshotKeys = new mutable.TreeSet[String]()

      private val snapshotMap = new mutable.AnyRefMap[String, DataSnapshot](bufferSize)

      private val onChildAddedCallback = getAsyncCallback[DataSnapshot] {
        dataSnapshot =>
          enqueueSnapshot(dataSnapshot)

          if (isAvailable(out)) {
            pollDocument().foreach(nullifyDocumentAndPush)
          }
      }

      private val onChildRemovedCallback = getAsyncCallback[DataSnapshot] {
        dataSnapshot =>
          removeSnapshot(dataSnapshot.getKey)
          ()
      }

      private val onFailureCallback = getAsyncCallback[Throwable](failStage)

      override protected def doAbort(throwable: Throwable): Unit =
        failStage(throwable)

      override protected def doShutdown(): Unit =
        completeStage()

      override def onCancelled(databaseError: DatabaseError): Unit =
        onFailureCallback.invoke(databaseError.toException)

      override def onChildRemoved(dataSnapshot: DataSnapshot): Unit =
        onChildRemovedCallback.invoke(dataSnapshot)

      override def onChildAdded(dataSnapshot: DataSnapshot, previous: String): Unit =
        onChildAddedCallback.invoke(dataSnapshot)

      override def onChildMoved(dataSnapshot: DataSnapshot, previous: String): Unit = ()

      override def onChildChanged(dataSnapshot: DataSnapshot, previous: String): Unit = ()

      @scala.throws[Exception](classOf[Exception])
      override def preStart(): Unit = {
        super.preStart()

        startConsumer()
      }

      @scala.throws[Exception](classOf[Exception])
      override def postStop(): Unit = {
        super.postStop()

        stopConsumer()
        onStop()
      }

      @scala.throws[Exception](classOf[Exception])
      override def onPull(): Unit =
        pollDocument().foreach(nullifyDocumentAndPush)

      private def nullifyDocumentAndPush(document: Document): Unit =
        push(out, nullifyDocument(document))

      private def nullifyDocument(document: Document): Future[Document] = {
        val promise = Promise[Document]()

        val listener = new SetCompletionListener {
          override protected def onSetSuccess(key: String): Unit = {
            promise.success(document)
            ()
          }

          override protected def onSetError(throwable: Throwable): Unit = {
            promise.failure(throwable)
            ()
          }
        }

        sourceNode
          .child(document.key)
          .setValue(null, listener)

        promise.future
      }

      private def pollDocument(): Option[Document] =
        snapshotKeys
          .headOption
          .flatMap(removeSnapshot)
          .map {
            dataSnapshot =>
              Document(
                key = dataSnapshot.getKey,
                value = dataSnapshot.getValue
              )
          }

      private def enqueueSnapshot(dataSnapshot: DataSnapshot): Unit = {
        snapshotKeys.add(dataSnapshot.getKey)
        snapshotMap.put(dataSnapshot.getKey, dataSnapshot)
        ()
      }

      private def removeSnapshot(key: String): Option[DataSnapshot] = {
        snapshotKeys.remove(key)
        snapshotMap.remove(key)
      }

      private def stopConsumer(): Unit = {
        log.debug("Stopping consumer.")
        query.removeEventListener(this: ChildEventListener)
      }

      private def startConsumer(): Unit = {
        log.debug(s"Starting consumer.")
        query.addChildEventListener(this)
        ()
      }
    }

    (logic, logic)
  }
}
