/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.javadsl

import java.util.concurrent.CompletionStage

import akka.Done
import akka.stream.javadsl.Sink
import com.elkozmon.akka.firebase.{Document, scaladsl}
import com.google.firebase.database.DatabaseReference

import scala.compat.java8.FutureConverters.FutureOps

/**
  * Akka Firebase connector for publishing documents to Firebase database.
  */
object Producer {

  /**
    * The [[asyncSink]] pushes documents into `targetNode` asynchronously
    * without backpressure.
    *
    * Child node is stored in `targetNode` under key of received [[Document]],
    * unless it's null in which case auto-generated Firebase id is used.
    */
  def asyncSink(
    targetNode: DatabaseReference
  ): Sink[Document, CompletionStage[Done]] =
    scaladsl.Producer
      .asyncSink(targetNode)
      .mapMaterializedValue(_.toJava)
      .asJava
}
