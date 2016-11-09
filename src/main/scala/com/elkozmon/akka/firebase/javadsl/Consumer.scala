/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.javadsl

import java.util.concurrent.CompletionStage

import akka.Done
import akka.stream.javadsl.Source
import com.elkozmon.akka.firebase.internal.JavaWrapperConsumerControl
import com.elkozmon.akka.firebase.{Document, scaladsl}
import com.google.firebase.database.DatabaseReference

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
    def abort(throwable: Throwable): CompletionStage[Done]

    /**
      * Completes the consumer `Source`.
      */
    def shutdown(): CompletionStage[Done]
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
    scaladsl.Consumer
      .bufferedSource(sourceNode, bufferSize)
      .mapMaterializedValue(new JavaWrapperConsumerControl(_))
      .asJava
}
