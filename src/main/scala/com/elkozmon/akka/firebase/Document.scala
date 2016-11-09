/*
 * Copyright (C) 2016 Lubos Kozmon <https://elkozmon.com>
 */

package com.elkozmon.akka.firebase

final case class Document(
  key: String,
  value: AnyRef
)
