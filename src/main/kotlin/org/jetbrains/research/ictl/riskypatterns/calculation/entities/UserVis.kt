package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import kotlinx.serialization.Serializable

@Serializable
data class UserVis(val email: String, val authorship: Double, val normalizedAuthorship: Double? = null) {
  companion object {
    private fun formatDouble(value: Double) = String.format("%.4f", value).toDouble()

    fun convert(userStats: Map<String, UserStats>, developersSorted: Set<String>): List<UserVis> {
      val result = mutableListOf<UserVis>()

      for (mainAuthor in developersSorted) {
        result.add(UserVis(mainAuthor, userStats[mainAuthor]!!))
      }

      for ((email, stats) in userStats) {
        if (email in developersSorted) {
          continue
        }
        result.add(UserVis(email, stats))
      }
      return result
    }
  }

  constructor(email: String, stats: UserStats) : this(
    email,
    formatDouble(stats.contributionsByUser.authorship),
    formatDouble(stats.contributionsByUser.normalizedAuthorship),
  )
}
