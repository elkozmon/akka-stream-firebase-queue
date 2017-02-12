/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.javadsl

import java.util.concurrent.CompletionStage

import akka.NotUsed
import akka.stream.javadsl.Flow
import com.elkozmon.akka.firebase.{Document, scaladsl}
import com.google.firebase.database.DatabaseReference

import scala.compat.java8.FutureConverters._

/**
  * Akka Firebase connector for publishing documents to Firebase database.
  */
object Producer {

  /**
    * The [[asyncFlow]] pushes documents into `targetNode` asynchronously.
    *
    * Child node is stored in `targetNode` under key of received [[Document]],
    * unless it's null in which case auto-generated Firebase id is used.
    */
  def asyncFlow(
    targetNode: DatabaseReference
  ): Flow[Document, CompletionStage[Document], NotUsed] =
    scaladsl.Producer
      .asyncFlow(targetNode)
      .map(_.toJava)
      .asJava
}
