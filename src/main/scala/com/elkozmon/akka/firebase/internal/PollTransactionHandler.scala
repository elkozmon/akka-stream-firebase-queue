/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import com.google.firebase.database.Transaction.Result
import com.google.firebase.database._

private[firebase] trait PollTransactionHandler
  extends Transaction.Handler
    with Logging {

  private var data: AnyRef = _

  override final def doTransaction(mutableData: MutableData): Result = {
    data = mutableData.getValue

    if (data == null) {
      onPollAbort()
      Transaction.abort()
    } else {
      mutableData.setValue(null)
      Transaction.success(mutableData)
    }
  }

  override final def onComplete(
    databaseError: DatabaseError,
    committed: Boolean,
    dataSnapshot: DataSnapshot
  ): Unit =
    if (databaseError != null) {
      val exception = databaseError.toException
      log.warn(s"Poll failed at path '${dataSnapshot.getRef.getPath}'.", exception)
      onPollError(exception)
    } else if (committed) {
      log.debug(s"Poll committed at path '${dataSnapshot.getRef.getPath}'.")
      onPollSuccess(dataSnapshot.getKey, data)
    } else {
      log.debug(s"Poll aborted at path '${dataSnapshot.getRef.getPath}'.")
    }

  protected def onPollAbort(): Unit

  protected def onPollSuccess(key: String, value: AnyRef): Unit

  protected def onPollError(throwable: Throwable): Unit
}

object PollTransactionHandler {

  sealed trait Action
  case object Start extends Action
  case object Abort extends Action
}
