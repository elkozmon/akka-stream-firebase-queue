/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import java.util

import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.scaladsl.Consumer.Control
import com.google.firebase.database._

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

private[firebase] class AsyncSourceStage(
  sourceNode: DatabaseReference,
  bufferSize: Int
) extends GraphStageWithMaterializedValue[SourceShape[Future[Option[Document]]], Control]
  with Logging {

  require(bufferSize > 0, "bufferSize must be greater than 0.")

  private val out = Outlet[Future[Option[Document]]]("out")

  override def shape: SourceShape[Future[Option[Document]]] = SourceShape.of(out)

  @scala.throws[Exception](classOf[Exception])
  override def createLogicAndMaterializedValue(
    inheritedAttributes: Attributes
  ): (GraphStageLogic, Control) = {
    val logic = new GraphStageLogic(shape)
      with OutHandler
      with ChildEventListener
      with ValueEventListener
      with PromiseConsumerControl {

      this.setHandler(out, this)

      @volatile private var connected = false

      private val query = sourceNode.orderByKey().limitToFirst(bufferSize)

      private val referenceKeySortedSet = mutable.SortedSet[String]()

      private val referenceMap = new util.HashMap[String, DatabaseReference]

      private val onChildAddedCallback = getAsyncCallback[DataSnapshot] {
        dataSnapshot =>
          enqueueReference(dataSnapshot)

          if (isAvailable(out)) {
            push(out, pollDocument(pollReferenceNullable()))
          }
      }

      private val onChildRemovedCallback = getAsyncCallback[DataSnapshot] {
        dataSnapshot =>
          removeReference(dataSnapshot.getKey)
      }

      private val onFailureCallback = getAsyncCallback[Throwable](failStage)

      override protected def doAbort(throwable: Throwable): Unit =
        failStage(throwable)

      override protected def doShutdown(): Unit =
        completeStage()

      override def onChildMoved(dataSnapshot: DataSnapshot, previous: String): Unit = ()

      override def onChildChanged(dataSnapshot: DataSnapshot, previous: String): Unit = ()

      override def onChildRemoved(dataSnapshot: DataSnapshot): Unit =
        onChildRemovedCallback.invoke(dataSnapshot)

      override def onChildAdded(dataSnapshot: DataSnapshot, previous: String): Unit =
        onChildAddedCallback.invoke(dataSnapshot)

      /**
        * Connection listener callback
        */
      override def onDataChange(dataSnapshot: DataSnapshot): Unit =
      dataSnapshot.getValue match {
        case connected: java.lang.Boolean if connected =>
          log.debug("Source is connected.")
          this.connected = true
          this.resumeConsumer()

        case connected: java.lang.Boolean if !connected =>
          log.debug("Source is disconnected.")
          this.connected = false
          this.pauseConsumer()

        case _ =>
      }

      /**
        * Connection listener callback
        */
      override def onCancelled(dbError: DatabaseError): Unit =
        onFailureCallback.invoke(dbError.toException)

      @scala.throws[Exception](classOf[Exception])
      override def preStart(): Unit = {
        super.preStart()

        sourceNode
          .getDatabase
          .getReference(".info/connected")
          .addValueEventListener(this)
      }

      @scala.throws[Exception](classOf[Exception])
      override def postStop(): Unit = {
        super.postStop()

        sourceNode.removeEventListener(this: ValueEventListener)
        pauseConsumer()
        onStop()
      }

      @scala.throws[Exception](classOf[Exception])
      override def onPull(): Unit = {
        val nullableRef = pollReferenceNullable()

        if (nullableRef != null) {
          push(out, pollDocument(nullableRef))
        }
      }

      private def pollReferenceNullable(): DatabaseReference =
        if (referenceKeySortedSet.nonEmpty) {
          val key = referenceKeySortedSet.head

          removeReference(key)
        } else {
          null
        }

      private def enqueueReference(dataSnapshot: DataSnapshot): Unit = {
        referenceKeySortedSet.add(dataSnapshot.getKey)
        referenceMap.put(dataSnapshot.getKey, dataSnapshot.getRef)
      }

      private def removeReference(key: String): DatabaseReference = {
        referenceKeySortedSet.remove(key)
        referenceMap.remove(key)
      }

      private def pollDocument(databaseReference: DatabaseReference): Future[Option[Document]] = {
        val promiseDocumentOpt = Promise[Option[Document]]()

        val pollTransactionHandler = new PollTransactionHandler {

          override protected def onPollSuccess(key: String, value: AnyRef): Unit =
            promiseDocumentOpt.success(Some(Document(key, value)))

          override protected def onPollError(throwable: Throwable): Unit =
            if (connected) {
              onFailureCallback.invoke(throwable)
            } else {
              promiseDocumentOpt.trySuccess(None)
            }

          override protected def onPollAbort(): Unit =
            promiseDocumentOpt.success(None)
        }

        databaseReference.runTransaction(pollTransactionHandler, false)

        promiseDocumentOpt.future
      }

      private def pauseConsumer(): Unit = {
        log.debug("Pausing consumer.")
        query.removeEventListener(this: ChildEventListener)
      }

      private def resumeConsumer(): Unit = {
        log.debug(s"Resuming consumer.")
        query.addChildEventListener(this)
      }
    }

    (logic, logic)
  }
}
