package org.jetbrains.research.ictl.riskypatterns.calculation

object BusFactorConstants {

  const val SHIFT: Double = 3.293
  const val OWNERSHIP_SLOPE: Double = 1.098
  const val COMMITS_SLOPE: Double = 0.164
  const val OTHER_COMMITS_SLOPE: Double = 0.321
  const val REVIEWS_SLOPE: Double = 0.082
  const val OTHER_REVIEWS_SLOPE: Double = 0.160
  const val MEETINGS_SLOPE: Double = 0.164

  const val DISCUSSION_START: Int =
    604800000 // One week (in milliseconds) before the commit the author starts telling people what they are writing
  const val DISCUSSION_END: Int =
    604800000 // One week (in milliseconds) after the commit the author stops telling people what they are writing
  const val MILLISECONDS_IN_MINUTE: Int = 60000
  const val MAX_EFFECTIVE_TIME_AT_MEETINGS: Double =
    240.0 // no matter how much time one spends at meetings, one probably won't learn about the commit more than the original commit author

  const val AUTHORSHIP_THRESHOLD: Double = 3.293 // threshold #1 for author to be considered significant contributor
  const val NORMALIZED_AUTHORSHIP_THRESHOLD: Double =
    0.75 // threshold #2 for author to be considered significant contributor
  const val AUTHORSHIP_THRESHOLD_NEW: Double = 0.001
  const val DECAY_CHARACTERISTIC_TIME: Int = 90

  const val AUTHORSHIP_SLOPE_NEW: Double = 3.0
  const val REVIEWS_SLOPE_NEW: Double = 0.5
  const val MEETINGS_SLOPE_NEW: Double = 1.0
  const val COMMITS_SLOPE_NEW: Double = 1.0
  const val OTHER_COMMITS_SLOPE_NEW: Double = 2.4
  const val OTHER_REVIEWS_SLOPE_NEW: Double = 1.2

  const val NEW_FORMULA: Boolean = true
  const val DAYS_GAP: Long = 547
}
