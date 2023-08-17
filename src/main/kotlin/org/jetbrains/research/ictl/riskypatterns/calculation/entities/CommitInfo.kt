package org.jetbrains.research.ictl.riskypatterns.calculation.entities

data class CommitInfo(
  val authorUserInfo: UserInfo,
  val committerUserInfo: UserInfo,
  val authorCommitTimestamp: Long,
  val committerTimestamp: Long,
  val diffEntries: Collection<DiffEntry>,
  val numOfParents: Int,
  val fullMessage: String,
)
