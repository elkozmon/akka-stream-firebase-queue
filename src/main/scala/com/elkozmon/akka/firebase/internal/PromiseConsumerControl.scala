/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import akka.Done
import akka.stream.stage.GraphStageLogic
import com.elkozmon.akka.firebase.scaladsl.Consumer.Control

import scala.concurrent.{Future, Promise}

private[firebase] trait PromiseConsumerControl extends Control {
  this: GraphStageLogic =>

  private val stopPromise = Promise[Done]()

  private val doAbortCallback = getAsyncCallback[Throwable](doAbort)

  private val doShutdownCallback = getAsyncCallback[Unit](_ => doShutdown())

  protected def doAbort(throwable: Throwable): Unit

  protected def doShutdown(): Unit

  final protected def onStop(): Unit = {
    stopPromise.trySuccess(Done)
    ()
  }

  override final def abort(throwable: Throwable): Future[Done] = {
    doAbortCallback.invoke(throwable)
    stopPromise.future
  }

  override final def shutdown(): Future[Done] = {
    doShutdownCallback.invoke(())
    stopPromise.future
  }
}
