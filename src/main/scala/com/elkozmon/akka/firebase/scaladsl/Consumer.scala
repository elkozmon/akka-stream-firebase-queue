/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.scaladsl

import akka.Done
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.internal.AsyncSourceStage
import com.google.firebase.database.DatabaseReference

import scala.concurrent.Future

/**
  * Akka Firebase connector for consuming documents from Firebase database
  * in a message queue manner, removing consumed documents from the database.
  */
object Consumer {

  /**
    * Materialized value of the consumer `Source`.
    */
  trait Control {

    /**
      * Fail the consumer `Source` with given `throwable`.
      *
      * Call [[shutdown]] to close consumer normally.
      */
    def abort(throwable: Throwable): Future[Done]

    /**
      * Completes the consumer `Source`.
      */
    def shutdown(): Future[Done]
  }

  /**
    * The [[bufferedSource]] consumes `sourceNode` child nodes and stores
    * them in buffer of given size, until it becomes full.
    *
    * Child nodes are emitted in an ascending lexicographical order of their keys.
    *
    * Each consumed child node is removed from the database.
    */
  def bufferedSource(
      sourceNode: DatabaseReference,
      bufferSize: Int
  ): Source[Document, Control] =
    Source
      .fromGraph(new AsyncSourceStage(sourceNode, bufferSize))
      .mapAsync(bufferSize)(identity)
      .collect { case Some(doc) => doc }
      .buffer(bufferSize, OverflowStrategy.backpressure)
}
