package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import kotlinx.serialization.Serializable

@Serializable
data class CompactCommitData(val userId: Int, val files: Set<Int>, val type: Type) {
  enum class Type { COMMIT, REVIEW }
}

