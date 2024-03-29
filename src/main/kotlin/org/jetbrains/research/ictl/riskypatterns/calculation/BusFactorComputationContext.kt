package org.jetbrains.research.ictl.riskypatterns.calculation

import kotlinx.serialization.Serializable
import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactor.Companion.isMainContributor
import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactor.Companion.isMinorContributor
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserStats
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.FileMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.UserMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class BusFactorComputationContext(
  override val configSnapshot: BusFactorConfigSnapshot = BusFactorConfigSnapshot.getDefault(),
  override val userMapper: UserMapper = UserMapper(),
  override val fileMapper: FileMapper = FileMapper(),
) : BFContext {

  // [fileId] = ownership
  override val filesOwnership: MutableMap<Int, OwnershipPerUser> = HashMap()

  //  [fileId] = (userId, weightedOwnership)
  override val weightedOwnership: MutableMap<Int, Pair<Int, Double>> = HashMap()

  @Serializable(with = LocalDateSerializer::class)
  var lastCommitCommitterLocalDate: LocalDate? = null
  var lastCommitHash: String? = null

  override fun checkData(fileNames: List<String>): Boolean {
    for (fileName in fileNames) {
      val fileId = fileMapper.getOrNull(fileName) ?: continue
      val fileInfo = filesOwnership[fileId] ?: continue
      if (fileInfo.isNotEmpty()) return true
    }
    return false
  }

  override fun userToAuthorship(fileNames: List<String>): Map<Int, Double> {
    val userToOwnership = HashMap<Int, Double>()
    for (fileName in fileNames) {
      val fileId = fileMapper.getOrNull(fileName) ?: continue
      val fileInfo = filesOwnership[fileId] ?: continue
      for ((userId, info) in fileInfo) {
        val authorship = info.authorship
        userToOwnership.compute(userId) { _, v -> if (v == null) authorship else v + authorship }
      }
    }
    return userToOwnership
  }

  // TODO: remove trains
  override fun filesUsersStats(fileNames: List<String>): Map<String, UserStats> {
    val userToContribution = HashMap<String, UserStats>()
    var sumAuthorship = 0.0
    val isFile = fileNames.size == 1

    for (fileName in fileNames) {
      val fileId = fileMapper.getOrNull(fileName) ?: continue
      val fileInfo = filesOwnership[fileId] ?: continue
      for ((userId, info) in fileInfo) {
        val user = userMapper.getOrNull(userId)!!
        val contributionsByUser = userToContribution.computeIfAbsent(user) { UserStats() }.contributionsByUser
        contributionsByUser.reviews += info.reviews
        contributionsByUser.commits += info.commits
        contributionsByUser.authorship += info.authorship
        if (isFile) {
          sumAuthorship += info.authorship
        }
      }
    }

    if (userToContribution.isNotEmpty()) {
      val max = userToContribution.maxByOrNull { it.value.contributionsByUser.authorship }!!
        .value.contributionsByUser.authorship

      for ((_, stats) in userToContribution) {
        val authorship = stats.contributionsByUser.authorship
        val normalizedAuthorship = authorship / max
        stats.contributionsByUser.normalizedAuthorship = normalizedAuthorship

        val isMinorContributor = isFile && isMinorContributor(authorship, sumAuthorship)
        val isMainContributor = isMainContributor(authorship, normalizedAuthorship)

        stats.isMinorContributor = isMinorContributor
        stats.isMainContributor = isMainContributor && !isMinorContributor
      }
    }

    return userToContribution
  }
}
