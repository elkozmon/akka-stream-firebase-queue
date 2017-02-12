/*
 * Copyright (C) 2017 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase.internal

import org.slf4j.{Logger, LoggerFactory}

private[firebase] trait Logging {
  protected val log: Logger = LoggerFactory.getLogger(getClass)
}
