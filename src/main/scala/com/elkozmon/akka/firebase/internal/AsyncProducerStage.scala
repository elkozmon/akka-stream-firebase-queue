/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import akka.stream._
import akka.stream.stage._
import com.elkozmon.akka.firebase.Document
import com.google.firebase.database.DatabaseReference

import scala.concurrent.{Future, Promise}

private[firebase] class AsyncProducerStage(
  targetNode: DatabaseReference
) extends GraphStage[FlowShape[Document, Future[Document]]] {

  private val in = Inlet[Document]("in")
  private val out = Outlet[Future[Document]]("out")

  override def shape: FlowShape[Document, Future[Document]] =
    FlowShape.of(in, out)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape)
      with InHandler
      with OutHandler {

      this.setHandler(in, this)
      this.setHandler(out, this)

      @scala.throws[Exception](classOf[Exception])
      override def onPull(): Unit = pull(in)

      @scala.throws[Exception](classOf[Exception])
      override def onPush(): Unit = {
        val document = grab(in)

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

        if (document.key != null) {
          targetNode
            .child(document.key)
            .setValue(document.value, listener)
        } else {
          targetNode
            .push()
            .setValue(document.value, listener)
        }

        push(out, promise.future)
        ()
      }
    }
}
