package org.jetbrains.research.ictl.riskypatterns.calculation

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Util {
  private fun toSystemZonedDateTime(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())

  fun toLocalDateUTC(timestamp: Long): LocalDate = toSystemZonedDateTime(timestamp).withZoneSameInstant(ZoneId.of("UTC")).toLocalDate()
}
