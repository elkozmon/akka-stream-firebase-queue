/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import akka.Done
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.elkozmon.akka.firebase.Document
import com.google.firebase.database.DatabaseReference

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

private[firebase] class AsyncSinkStage(
  targetNode: DatabaseReference
) extends GraphStageWithMaterializedValue[SinkShape[Document], Future[Done]]
    with Logging {

  private val in = Inlet[Document]("in")

  override def shape: SinkShape[Document] = SinkShape.of(in)

  @scala.throws[Exception](classOf[Exception])
  override def createLogicAndMaterializedValue(
    inheritedAttributes: Attributes
  ): (GraphStageLogic, Future[Done]) = {
    val promiseDone = Promise[Done]()

    val logic = new GraphStageLogic(shape)
      with InHandler
      with SetCompletionListener {

      this.setHandler(in, this)

      private var shuttingDown = false

      private var awaitingSetAcks = 0

      private val failureQueue = mutable.Queue.empty[Throwable]

      private val onSetSuccessCallback =
        getAsyncCallback[Unit] {
          _ =>
            awaitingSetAcks -= 1

            if (shuttingDown && awaitingSetAcks == 0) {
              shutdownStage()
            }
        }

      private val onSetErrorCallback =
        getAsyncCallback[Throwable] {
          throwable =>
            awaitingSetAcks -= 1

            onFailure(throwable)
        }

      override protected def onSetSuccess(key: String): Unit =
        onSetSuccessCallback.invoke(())

      override protected def onSetError(throwable: Throwable): Unit =
        onSetErrorCallback.invoke(throwable)

      @scala.throws[Exception](classOf[Exception])
      override def postStop(): Unit = {
        super.postStop()

        promiseDone.trySuccess(Done)
        ()
      }

      @scala.throws[Exception](classOf[Exception])
      override def preStart(): Unit = {
        super.preStart()

        setKeepGoing(true)
        pull(in)
      }

      @scala.throws[Exception](classOf[Exception])
      override def onUpstreamFinish(): Unit =
        if (awaitingSetAcks == 0) {
          shutdownStage()
        } else {
          shuttingDown = true
        }

      @scala.throws[Exception](classOf[Exception])
      override def onUpstreamFailure(throwable: Throwable): Unit =
        onFailure(throwable)

      @scala.throws[Exception](classOf[Exception])
      override def onPush(): Unit = {
        awaitingSetAcks += 1

        val document = grab(in)

        if (!shuttingDown) {
          pull(in)
        }

        if (document.key != null) {
          targetNode
            .child(document.key)
            .setValue(document.value, this)
        } else {
          targetNode
            .push()
            .setValue(document.value, this)
        }
      }

      private def onFailure(throwable: Throwable): Unit = {
        shuttingDown = true
        failureQueue.enqueue(throwable)

        if (awaitingSetAcks == 0) {
          shutdownStage()
        }
      }

      private def shutdownStage(): Unit =
        failureQueue.headOption match {
          case Some(throwable) =>
            failStage(throwable)

          case None =>
            completeStage()
        }
    }

    (logic, promiseDone.future)
  }
}
