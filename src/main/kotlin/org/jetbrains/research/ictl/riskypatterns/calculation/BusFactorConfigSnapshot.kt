package org.jetbrains.research.ictl.riskypatterns.calculation

import kotlinx.serialization.Serializable

@Serializable
data class BusFactorConfigSnapshot(
  val useReviewers: Boolean,
  val weightedAuthorship: Boolean,
  val ignoreExtensions: Set<String>,
) {
  companion object {
    private const val DEFAULT_USE_REVIEWERS = false
    private const val DEFAULT_WEIGHTED_AUTHORSHIP = true

    fun getDefault() = BusFactorConfigSnapshot(
      DEFAULT_USE_REVIEWERS,
      DEFAULT_WEIGHTED_AUTHORSHIP,
      setOf(),
    )
  }
}
