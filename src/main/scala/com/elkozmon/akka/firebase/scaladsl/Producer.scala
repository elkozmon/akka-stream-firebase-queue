/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.scaladsl

import akka.Done
import akka.stream.scaladsl.Sink
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.internal.AsyncSinkStage
import com.google.firebase.database.DatabaseReference

import scala.concurrent.Future

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
  ): Sink[Document, Future[Done]] =
    Sink.fromGraph(new AsyncSinkStage(targetNode))
}
