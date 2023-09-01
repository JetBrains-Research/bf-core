package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactorConstants.NEW_FORMULA
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.BusFactorCalculationResult
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.BusFactorStatus
import kotlin.math.ln

typealias OwnershipPerUser = MutableMap<Int, ContributionsByUser>

class BusFactorCalculation(
  private val context: BusFactorComputationContext,
) {

  init {
    computeAuthorship(context.configSnapshot.weightedAuthorship)
  }

  private fun getMajor(fileIds: Collection<Int>): Map<Int, MutableSet<Int>> {
    val majorContributors: HashMap<Int, MutableSet<Int>> = HashMap()
    for (fileId in fileIds) {
      val ownershipPerUser = context.filesOwnership[fileId] ?: continue
      majorContributors[fileId] = ownershipPerUser.filter { it.value.isMajor() }.keys.toMutableSet()
    }
    return majorContributors
  }

  private fun developersSortedByContribution(filesData: Map<Int, Set<Int>>): List<Int> {
    val userMajorContributions: HashMap<Int, Int> = HashMap()
    for ((_, users) in filesData) {
      for (userId in users) {
        userMajorContributions.compute(userId) { _, v -> if (v == null) 1 else v + 1 }
      }
    }
    return userMajorContributions.entries.sortedByDescending { it.value }.map { it.key }
  }

  /** Here we compute the bus factor. extendedComputation is a flag for whether
   * we use the baseline algorithm of Avelino et al. (extendedComputation == false)
   * or use the new algorithm that uses code reviews etc. (extendedComputation == true)
   * The algorithm is iterative: at each step we look, how many files are orphaned
   * (every major contributor for the file is already listed in the "keyDevelopers" list).
   * If less than a half of files are orphan, we find top contributor for each of the files,
   * ignoring the developers from the keyDeveloper list. The person who is the top contributor
   * for the most of the files is then added to the keyDeveloper list and the iteration is repeated.
   * If more than half of the files are orphaned, the algorithm stops. The members of keyDevelopers list
   * are then the core developers removing whom results in bus factor scenario, and busFactor is the bus factor
   * of the project.
   **/

  private fun countOrphan(majorFileData: Map<Int, Set<Int>>): Int {
    var result = 0
    for (v in majorFileData.values) {
      if (v.isEmpty()) result++
    }
    return result
  }

  fun computeBusFactorForFiles(fileNames: Collection<String>): BusFactorCalculationResult {
    // ignored
    val filteredFileNames = fileNames.filter { BusFactor.isValidFilePath(it, context.configSnapshot.ignoreExtensions) }
    if (filteredFileNames.isEmpty()) {
      return BusFactorCalculationResult(BusFactorStatus(ignored = true), emptySet())
    }

    // no data, too old
    val notOld = fileNames.mapNotNull { context.fileMapper.getOrNull(it) }
    if (notOld.isEmpty()) {
      return BusFactorCalculationResult(BusFactorStatus(old = true), emptySet())
    }

    // filter move cases
    val fileIds = notOld.mapNotNull { if (context.filesOwnership[it] != null) it else null }
    if (fileIds.isEmpty()) {
      return BusFactorCalculationResult(BusFactorStatus(old = true), emptySet())
    }

    val majorFileData = getMajor(fileIds)

    var busFactor = 0
    val developers = developersSortedByContribution(majorFileData)
    val filesCount = fileIds.size
    var orphanFiles = countOrphan(majorFileData)
    for (mainAuthor in developers) {
      if (filesCount >= 2 * orphanFiles) {
        busFactor++
      } else {
        break
      }

      orphanFiles = 0
      // Each time we delete 1 main author from major contributors and count files without authors
      for (v in majorFileData.values) {
        v.remove(mainAuthor)
        if (v.isEmpty()) orphanFiles++
      }
    }
    return BusFactorCalculationResult(
      BusFactorStatus(busFactor = busFactor),
      developers.map { context.userMapper.getOrNull(it)!! }.toSet(),
    )
  }

  fun userStats(fileNames: List<String>) = context.filesUsersStats(fileNames)

  private fun calcUserToFileAuthorship(
    ownership: Int,
    userCommits: Int,
    otherUsersCommits: Int,
    userReviews: Int = 0,
    otherUsersReviews: Int = 0,
    timeAtMeetings: Double = 0.0,
  ): Double {
    return BusFactorConstants.SHIFT + BusFactorConstants.OWNERSHIP_SLOPE * ownership +
      BusFactorConstants.COMMITS_SLOPE * userCommits - BusFactorConstants.OTHER_COMMITS_SLOPE * ln(1.0 + otherUsersCommits) +
      BusFactorConstants.REVIEWS_SLOPE * userReviews - BusFactorConstants.OTHER_REVIEWS_SLOPE * ln(1.0 + otherUsersReviews) +
      BusFactorConstants.MEETINGS_SLOPE * timeAtMeetings
  }

  private fun calcWeightedUserToFileAuthorship(
    weightedOwnership: Double,
    weightedCommits: Double,
    weightedOtherCommits: Double,
    weightedReviews: Double = 0.0,
    weightedOtherReviews: Double = 0.0,
    weightedTimeAtMeetings: Double = 0.0,
  ): Double {
    return BusFactorConstants.SHIFT + BusFactorConstants.OWNERSHIP_SLOPE * weightedOwnership +
      BusFactorConstants.COMMITS_SLOPE * weightedCommits - BusFactorConstants.OTHER_COMMITS_SLOPE * ln(1.0 + weightedOtherCommits) +
      BusFactorConstants.REVIEWS_SLOPE * weightedReviews - BusFactorConstants.OTHER_REVIEWS_SLOPE * ln(1.0 + weightedOtherReviews) +
      BusFactorConstants.MEETINGS_SLOPE * weightedTimeAtMeetings
  }

  private fun calcNewUserToFileAuthorship(
    weightedOwnership: Double,
    weightedCommits: Double,
    weightedAllCommits: Double,
    weightedOtherCommits: Double,
    weightedReviews: Double = 0.0,
    weightedAllReviews: Double = 0.0,
    weightedOtherReviews: Double = 0.0,
    weightedTimeAtMeetings: Double = 0.0,
  ): Double {
    return BusFactorConstants.AUTHORSHIP_SLOPE_NEW * weightedOwnership + BusFactorConstants.COMMITS_SLOPE_NEW * weightedCommits +
      BusFactorConstants.REVIEWS_SLOPE_NEW * weightedReviews + BusFactorConstants.MEETINGS_SLOPE_NEW * weightedTimeAtMeetings +
      BusFactorConstants.OTHER_COMMITS_SLOPE_NEW * ln(1 + weightedAllCommits) + BusFactorConstants.OTHER_REVIEWS_SLOPE_NEW * ln(
      1 + weightedAllReviews,
    ) -
      BusFactorConstants.OTHER_COMMITS_SLOPE_NEW * ln(1 + weightedOtherCommits) - BusFactorConstants.OTHER_REVIEWS_SLOPE_NEW * ln(
      1 + weightedOtherReviews,
    )
  }

  private fun computeAuthorship(
    weightedAuthorship: Boolean,
  ) {
    // Our equation for authorship computation; the extended version of the equation by Avelino et al.
    for ((fileId, contributionsOfUsers) in context.filesOwnership) {
      val (ownerId, ownerWeightedOwnership) = context.weightedOwnership[fileId] ?: (-1 to 0.0)
      if (weightedAuthorship) {
        val totalWeightedCommits = contributionsOfUsers.map { it.value }.sumOf { it.weightedCommits }
        val totalWeightedReviews = contributionsOfUsers.map { it.value }.sumOf { it.weightedReviews }
        for ((userId, userContribution) in contributionsOfUsers) {
          val weightedOwnership = if (userId == ownerId) ownerWeightedOwnership else 0.0
          val weightedCommits = userContribution.weightedCommits
          val weightedOtherCommits = totalWeightedCommits - userContribution.weightedCommits
          val weightedReviews = userContribution.weightedReviews
          val weightedOtherReviews = totalWeightedReviews - userContribution.weightedReviews

          if (!NEW_FORMULA) {
            userContribution.authorship = calcWeightedUserToFileAuthorship(
              weightedOwnership,
              weightedCommits,
              weightedOtherCommits,
              weightedReviews,
              weightedOtherReviews,
            )
          } else {
            userContribution.authorship = calcNewUserToFileAuthorship(
              weightedOwnership,
              weightedCommits,
              totalWeightedCommits,
              weightedOtherCommits,
              weightedReviews,
              totalWeightedReviews,
              weightedOtherReviews,
            )
          }
        }
      } else {
        val totalCommits = contributionsOfUsers.map { it.value }.sumOf { it.commits }
        val totalReviews = contributionsOfUsers.map { it.value }.sumOf { it.reviews }
        for ((userId, userContribution) in contributionsOfUsers) {
          userContribution.authorship = calcUserToFileAuthorship(
            ownership = if (ownerId == userId) 1 else 0,
            userCommits = userContribution.commits,
            otherUsersCommits = totalCommits - userContribution.commits,
            userReviews = userContribution.reviews,
            otherUsersReviews = totalReviews - userContribution.reviews,
          )
        }
      }

      val maxAuthorshipToFile = contributionsOfUsers.maxOfOrNull { it.value.authorship } ?: return
      for ((_, userContribution) in contributionsOfUsers) {
        userContribution.normalizedAuthorship = userContribution.authorship / maxAuthorshipToFile
      }
    }
  }
}
