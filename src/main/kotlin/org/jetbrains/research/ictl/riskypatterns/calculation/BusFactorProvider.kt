package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.FileInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.UserMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitProcessor

class BusFactorProvider: BusFactor() {

  fun calculate(
    repositoryName: String,
    commitsInfo: Iterable<CommitInfo>,
    filePathsToBytes: Iterable<FileInfo>,
    botFilter: BotFilter? = null,
    mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
  ): Tree {
    val userMapper = UserMapper(botFilter, mergedUsers)
    val context = BusFactorComputationContext(userMapper)
    val commitProcessor = CommitProcessor(context)

    proceedCommits(commitProcessor, commitsInfo)

    val busFactorCalculation = BusFactorCalculation(context)

    val root = buildTree(repositoryName, filePathsToBytes)
    calculateBusFactorForTree(root, busFactorCalculation)
    return root
  }

}
