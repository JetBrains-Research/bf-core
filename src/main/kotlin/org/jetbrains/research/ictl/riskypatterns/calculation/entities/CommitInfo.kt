package org.jetbrains.research.ictl.riskypatterns.calculation.entities

data class CommitInfo(
  val authorEmail: String,
  val committerEmail: String,
  val authorCommitTimestamp: Long,
  val committerTimestamp: Long,
  val diffEntries: Collection<DiffEntry>,
  val numOfParents: Int,
  val fullMessage: String,
  val authorName: String = "",
  val committerName: String = ""
)
