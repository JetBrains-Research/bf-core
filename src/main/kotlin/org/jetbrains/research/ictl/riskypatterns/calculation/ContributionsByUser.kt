package org.jetbrains.research.ictl.riskypatterns.calculation

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.exp

/**
 * Contributions of users to a single file
 */

@Serializable
class ContributionsByUser {

  var reviews: Int = 0
  var weightedReviews: Double = 0.0

  var commits: Int = 0
  var weightedCommits: Double = 0.0

  var authorship: Double = 0.0
  var normalizedAuthorship: Double = 0.0

  private fun getWeight(localDate: LocalDate, latestCommitLocalDate: LocalDate): Double {
    if (localDate > latestCommitLocalDate) throw Exception("Current commit date is older than provided latest commit date.")
    val passedDays = localDate.until(latestCommitLocalDate, ChronoUnit.DAYS)
    return exp(-1.0 * passedDays / BusFactorConstants.DECAY_CHARACTERISTIC_TIME)
  }

  fun addFileChange(localDate: LocalDate, latestCommitLocalDate: LocalDate): Double {
    val weight = getWeight(localDate, latestCommitLocalDate)
    commits += 1
    weightedCommits += weight
    return weight
  }

  fun addReview(localDate: LocalDate, latestCommitLocalDate: LocalDate) {
    reviews += 1
    weightedReviews += getWeight(localDate, latestCommitLocalDate)
  }

  fun isMajor(): Boolean {
    if (BusFactorConstants.NEW_FORMULA) {
      return authorship >= BusFactorConstants.AUTHORSHIP_THRESHOLD_NEW &&
        normalizedAuthorship > BusFactorConstants.NORMALIZED_AUTHORSHIP_THRESHOLD
    }

    return authorship >= BusFactorConstants.AUTHORSHIP_THRESHOLD &&
      normalizedAuthorship > BusFactorConstants.NORMALIZED_AUTHORSHIP_THRESHOLD
  }
}
