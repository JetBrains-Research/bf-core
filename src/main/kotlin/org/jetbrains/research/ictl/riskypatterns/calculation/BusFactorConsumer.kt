package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.FileInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
class BusFactorConsumer(
  repositoryName: String,
  botFilter: BotFilter? = null,
  mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
) : BusFactor(repositoryName, botFilter, mergedUsers) {

  private var lastCommitTimestamp = -1L

  fun consumeCommit(commitInfo: CommitInfo) {
    val timestamp = commitInfo.committerTimestamp
    if (timestamp > lastCommitTimestamp) {
      lastCommitTimestamp = timestamp
      commitProcessor.setLastCommit(timestamp)
    }
    processCommit(commitInfo)
  }

  fun calculate(filePathsToBytes: Iterable<FileInfo>): Tree {
    return getBusFactorForTree(filePathsToBytes)
  }
}
