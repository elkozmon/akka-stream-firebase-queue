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

import scala.compat.java8.FutureConverters._

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
    * The [[asyncSource]] consumes `sourceNode` child nodes and stores
    * them in buffer of given size, until it becomes full.
    *
    * This source must be the only consumer of given `sourceNode`, otherwise
    * some child nodes might end up being consumed by multiple consumers.
    *
    * [[Document]]s are emitted in an ascending lexicographical order
    * of their keys.
    *
    * Each emitted [[Document]] is removed from the database.
    */
  def asyncSource(
    sourceNode: DatabaseReference,
    bufferSize: Int
  ): Source[CompletionStage[Document], Control] =
    scaladsl.Consumer
      .asyncSource(sourceNode, bufferSize)
      .map(_.toJava)
      .mapMaterializedValue(new JavaWrapperConsumerControl(_))
      .asJava
}
