package org.jetbrains.research.ictl.riskypatterns.calculation

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Util {
  fun toLocalDate(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}
