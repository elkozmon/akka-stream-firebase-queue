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

private[firebase] class AsyncSourceStage(
  sourceNode: DatabaseReference,
  bufferSize: Int
) extends GraphStageWithMaterializedValue[SourceShape[Document], Control]
    with Logging {

  require(bufferSize > 0, "bufferSize must be greater than 0.")

  private val out = Outlet[Document]("out")

  override def shape: SourceShape[Document] = SourceShape.of(out)

  @scala.throws[Exception](classOf[Exception])
  override def createLogicAndMaterializedValue(
    inheritedAttributes: Attributes
  ): (GraphStageLogic, Control) = {
    val logic = new GraphStageLogic(shape)
      with OutHandler
      with ChildEventListener
      with SetCompletionListener
      with PromiseConsumerControl {

      this.setHandler(out, this)

      private var shuttingDown = false

      private var awaitingNullifyAcks = 0

      private val query = sourceNode.orderByKey().limitToFirst(bufferSize)

      private val snapshotKeys = new mutable.TreeSet[String]()

      private val snapshotMap = new mutable.AnyRefMap[String, DataSnapshot](bufferSize)

      private val failureQueue = mutable.Queue.empty[Throwable]

      private val onChildAddedCallback = getAsyncCallback[DataSnapshot] {
        dataSnapshot =>
          enqueueSnapshot(dataSnapshot)

          if (isAvailable(out)) {
            pollDocument().foreach(pushAndNullifyDocument)
          }
      }

      private val onChildRemovedCallback = getAsyncCallback[DataSnapshot] {
        dataSnapshot =>
          removeSnapshot(dataSnapshot.getKey)
          ()
      }

      private val onSetErrorCallback = getAsyncCallback[Throwable] {
        throwable =>
          awaitingNullifyAcks -= 1
          shuttingDown = true

          failureQueue.enqueue(throwable)

          if (awaitingNullifyAcks == 0) {
            shutdownStage()
          }
      }

      private val onSetSuccessCallback = getAsyncCallback[String] {
        _ =>
          awaitingNullifyAcks -= 1

          if (shuttingDown && awaitingNullifyAcks == 0) {
            shutdownStage()
          }
      }

      private val onFailureCallback = getAsyncCallback[Throwable](failStage)

      override protected def doAbort(throwable: Throwable): Unit = {
        log.info(s"Aborting consumer with $awaitingNullifyAcks outstanding acks.")
        failStage(throwable)
      }

      override protected def doShutdown(): Unit = {
        shuttingDown = true

        if (awaitingNullifyAcks == 0) {
          completeStage()
        }
      }

      override protected def onSetSuccess(key: String): Unit =
        onSetSuccessCallback.invoke(key)

      override protected def onSetError(throwable: Throwable): Unit =
        onSetErrorCallback.invoke(throwable)

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
      override def onDownstreamFinish(): Unit =
        if (awaitingNullifyAcks > 0) {
          shuttingDown = true
          setKeepGoing(true)
        } else {
          completeStage()
        }

      @scala.throws[Exception](classOf[Exception])
      override def onPull(): Unit =
        pollDocument().foreach(pushAndNullifyDocument)

      private def pushAndNullifyDocument(document: Document): Unit = {
        push(out, document)
        nullifyDocument(document)
      }

      private def nullifyDocument(document: Document): Unit = {
        sourceNode
          .child(document.key)
          .setValue(null, this)

        awaitingNullifyAcks += 1
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

      private def shutdownStage(): Unit =
        failureQueue.headOption match {
          case Some(throwable) =>
            failStage(throwable)

          case None =>
            completeStage()
        }
    }

    (logic, logic)
  }
}
