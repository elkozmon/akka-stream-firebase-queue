/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.scaladsl

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.internal.AsyncProducerStage
import com.google.firebase.database.DatabaseReference

import scala.concurrent.Future

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
  ): Flow[Document, Future[Document], NotUsed] =
    Flow.fromGraph(new AsyncProducerStage(targetNode))
}
