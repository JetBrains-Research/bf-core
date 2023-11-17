package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import kotlinx.serialization.Serializable

@Serializable
data class CompactCommitData(val usersIds: Set<Int>, val files: Map<Int, Int>, val type: Type) {
  enum class Type { COMMIT, REVIEW }
}
