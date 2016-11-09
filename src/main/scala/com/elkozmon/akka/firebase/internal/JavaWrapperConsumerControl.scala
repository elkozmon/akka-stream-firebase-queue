/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import java.util.concurrent.CompletionStage

import akka.Done
import com.elkozmon.akka.firebase.{javadsl, scaladsl}

import scala.compat.java8.FutureConverters.FutureOps

private[firebase] final class JavaWrapperConsumerControl(underlying: scaladsl.Consumer.Control)
  extends javadsl.Consumer.Control {

  override def abort(throwable: Throwable): CompletionStage[Done] =
    underlying.abort(throwable).toJava

  override def shutdown(): CompletionStage[Done] =
    underlying.shutdown().toJava
}
