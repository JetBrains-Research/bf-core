package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.FileInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo

class BusFactorProvider(
  repositoryName: String,
  botFilter: BotFilter? = null,
  mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
) : BusFactor(repositoryName, botFilter, mergedUsers) {

  private fun proceedCommits(commitsInfo: Iterable<CommitInfo>) {
    val lastCommit = commitsInfo.first()
    commitProcessor.setLastCommit(lastCommit.committerTimestamp)
    for (commit in commitsInfo) {
      processCommit(commit)
    }
  }

  fun calculate(
    commitsInfo: Iterable<CommitInfo>,
    filePathsToBytes: Iterable<FileInfo>,
  ): Tree {
    proceedCommits(commitsInfo)
    return getBusFactorForTree(filePathsToBytes)
  }
}
