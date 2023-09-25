package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.FileInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.UserMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitProcessor
class BusFactorConsumer(
  botFilter: BotFilter? = null,
  mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
) : BusFactor() {

  private val userMapper = UserMapper(botFilter, mergedUsers)
  private val context = BusFactorComputationContext(userMapper)
  private val commitProcessor = CommitProcessor(context)
  private var lastCommitTimestamp = -1L

  fun processCommit(commitInfo: CommitInfo) {
    val timestamp = commitInfo.committerTimestamp
    if (timestamp > lastCommitTimestamp) {
      lastCommitTimestamp = timestamp
      commitProcessor.setLastCommit(timestamp)
    }
    processCommit(commitInfo, commitProcessor)
  }

  fun calculate(repositoryName: String, filePathsToBytes: Iterable<FileInfo>): Tree {
    val busFactorCalculation = BusFactorCalculation(context)
    val root = buildTree(repositoryName, filePathsToBytes)
    calculateBusFactorForTree(root, busFactorCalculation)

    return root
  }
}
