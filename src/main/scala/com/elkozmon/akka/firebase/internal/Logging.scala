/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import org.slf4j.LoggerFactory

private[firebase] trait Logging {

  protected val log = LoggerFactory.getLogger(getClass)
}
