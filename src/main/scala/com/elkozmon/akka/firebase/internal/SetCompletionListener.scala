/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import com.google.firebase.database.DatabaseReference.CompletionListener
import com.google.firebase.database._

private[firebase] trait SetCompletionListener
  extends CompletionListener
    with Logging {

  override final def onComplete(
    databaseError: DatabaseError,
    databaseReference: DatabaseReference
  ): Unit =
    if (databaseError != null) {
      val exception = databaseError.toException
      log.warn(s"Set failed at path '${databaseReference.getPath}'", exception)
      onSetError(exception)
    } else {
      log.debug(s"Set succeeded at path '${databaseReference.getPath}'")
      onSetSuccess(databaseReference.getKey)
    }

  protected def onSetSuccess(key: String): Unit

  protected def onSetError(throwable: Throwable): Unit
}
