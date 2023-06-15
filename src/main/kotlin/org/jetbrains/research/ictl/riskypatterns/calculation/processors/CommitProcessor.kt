package org.jetbrains.research.ictl.riskypatterns.calculation.processors

import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactorComputationContext
import org.jetbrains.research.ictl.riskypatterns.calculation.ContributionsByUser
import java.util.concurrent.ConcurrentHashMap


data class DiffEntry(val oldPath: String, val newPath: String, val changeType: ChangeType) {
  enum class ChangeType {
    ADD, MODIFY, COPY, RENAME, DELETE,
  }
}

data class CommitInfo(
  val authorEmail: String,
  val committerEmail: String,
  val authorCommitTimestamp: Long,
  val committerTimestamp: Long,
  val diffEntries: Collection<DiffEntry>,
  val numOfParents: Int,
  val fullMessage: String
)

class CommitProcessor(private val context: BusFactorComputationContext) {

  companion object {
    private const val reviewStartToken = "Reviewed-by: "
    private const val coAuthorStartToken = "Co-authored-by: "
    private const val reviewersSplit = ", "

    fun getFilePath(diffEntry: DiffEntry): String {
      return when (diffEntry.changeType) {
        DiffEntry.ChangeType.DELETE -> diffEntry.oldPath
        else -> diffEntry.newPath
      }
    }
  }

  fun setLastCommit(lastCommitTimestamp: Long) {
    context.lastCommitCommitterTimestamp = lastCommitTimestamp
  }

  private fun getReviewers(message: String): List<String> {
    val idx = message.indexOf(reviewStartToken)
    if (idx == -1) return emptyList()

    val line = message.substring(idx + reviewStartToken.length)
    val newLineIdx = line.indexOf("\n")
    val reviewersLine = if (newLineIdx == -1) line else line.substring(0, newLineIdx)
    return reviewersLine.split(reviewersSplit)
  }

  /**
   * This function corresponds to addition of a new file: we want to add this file to FileMapper,
   * track it in CommitMapper (for code reviews) and save its author in filesOwnershipPrototypes
   */
  private fun addDiff(diffEntry: DiffEntry, authorCommitTimestamp: Long, userIds: Set<Int>) {
    val filePath = getFilePath(diffEntry)
    val fileId = context.fileMapper.add(filePath)
    userIds.forEach {
      addDiff(fileId, it, authorCommitTimestamp)
    }
  }

  private fun addDiff(fileId: Int, userId: Int, commitTimestamp: Long) {
    val weight = context.filesOwnership.computeIfAbsent(fileId) { HashMap() }
      .computeIfAbsent(userId) { ContributionsByUser() }
      .addFileChange(commitTimestamp, context.lastCommitCommitterTimestamp)
    context.weightedOwnership.compute(fileId) { _, v ->
      val pair = userId to weight
      if (v == null) {
        pair
      } else {
        if (v.second > weight) v else pair
      }
    }
  }

  /**
   * This function is for tracking file modification: we want to get the file ID,
   * track the file for code reviews in the CommitMapper, and save data about change in filesOwnershipPrototypes
   */
  private fun modifyDiff(diffEntry: DiffEntry, authorCommitTimestamp: Long, userIds: Set<Int>) {
    val filePath = getFilePath(diffEntry)
    val fileId = context.fileMapper.getOrNull(filePath)

    if (fileId == null) {
      addDiff(diffEntry, authorCommitTimestamp, userIds)
    } else {
      userIds.forEach {
        context.filesOwnership.computeIfAbsent(fileId) { ConcurrentHashMap() }
          .computeIfAbsent(it) { ContributionsByUser() }
          .addFileChange(authorCommitTimestamp, context.lastCommitCommitterTimestamp)
      }
    }
  }

  /**
   * Here we track renamed files, add them to the CommitMapper for code reviews processing and save data
   * about changes in file. Renamed file should be considered the same file for the authorship etc. purposes
   */
  private fun moveDiff(diffEntry: DiffEntry) {
    val oldFilePath = diffEntry.oldPath
    val newFilePath = diffEntry.newPath
    val (oldId, newId) = context.fileMapper.trackMove(
      oldFilePath,
      newFilePath,
    )

    if (oldId != newId) {
      context.filesOwnership[oldId]?.let { oldOwnership ->
        val fileOwnership = context.filesOwnership[newId]
        if (fileOwnership == null) {
          context.filesOwnership[newId] = oldOwnership
        } else {
          for ((userId, contribution) in oldOwnership) {
            val newContribution = fileOwnership[userId]
            if (newContribution == null) {
              fileOwnership[userId] = contribution
            } else {
              newContribution.commits += contribution.commits
              newContribution.weightedCommits += contribution.weightedCommits
            }
          }
        }
      }

      context.weightedOwnership[oldId]?.let {
        context.weightedOwnership[newId] = it
      }

      context.weightedOwnership.remove(oldId)
      context.filesOwnership.remove(oldId)
    }
  }

  private fun deleteDiff(diffEntry: DiffEntry) {
    val filePath = getFilePath(diffEntry)
    val fileId = context.fileMapper.getOrNull(filePath)!!
  }

  fun processCommit(commitInfo: CommitInfo): Boolean {
    if (commitInfo.numOfParents > 1) return false

    val authors = getAuthors(commitInfo).filter { !context.userMapper.isBot(it) }
    if (authors.isEmpty()) {
      return false
    }
    val userIds = authors.map { context.userMapper.addEmail(it) }.toSet()
    val authorCommitTimestamp = commitInfo.authorCommitTimestamp

    for (diffEntry in commitInfo.diffEntries) {
      when (diffEntry.changeType) {
        DiffEntry.ChangeType.DELETE -> {
//          deleteDiff(diffEntry, projectPath)
        }

        DiffEntry.ChangeType.RENAME -> {
          moveDiff(diffEntry)
        }

        else -> {
//          ADD, MODIFY, COPY
          addDiff(diffEntry, authorCommitTimestamp, userIds)
        }
      }
      if (context.configSnapshot.useReviewers && commitInfo.fullMessage != null) {
        val reviewers = getReviewers(commitInfo.fullMessage)
        addReviewForFile(diffEntry, reviewers, authorCommitTimestamp)
      }
    }

    return true
  }

  // TODO: replace commitStamp with smth better. Review time is needed. Mb use committer time  instead author
  private fun addReviewForFile(diffEntry: DiffEntry, reviewers: List<String>, authorCommitTimestamp: Long) {
    val filePath = getFilePath(diffEntry)
    val fileId = context.fileMapper.getOrNull(filePath)!!
    for (reviewer in reviewers) {
      val userId = context.userMapper.addName(reviewer)
      context.filesOwnership.computeIfAbsent(fileId) { HashMap() }
        .computeIfAbsent(userId) { ContributionsByUser() }
        .addReview(authorCommitTimestamp, context.lastCommitCommitterTimestamp)
    }
  }

  private fun getAuthors(commit: CommitInfo): Set<String> {
    val result = mutableSetOf<String>()
    val msg = commit.fullMessage
    for (line in msg.split("\n")) {
      if (line.startsWith(coAuthorStartToken)) {
        val email = line.trim().split(" ").last().removePrefix("<").removeSuffix(">")
        result.add(email)
      }
    }
    val author = commit.authorEmail
    result.add(author)
    return result
  }
}
