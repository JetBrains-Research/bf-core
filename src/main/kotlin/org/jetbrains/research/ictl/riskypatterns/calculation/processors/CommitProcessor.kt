package org.jetbrains.research.ictl.riskypatterns.calculation.processors

import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactorComputationContext
import org.jetbrains.research.ictl.riskypatterns.calculation.ContributionsByUser
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.DiffEntry
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo

class CommitProcessor(private val context: BusFactorComputationContext) {

  companion object {
    private const val REVIEW_START_TOKEN = "Reviewed-by: "
    private const val CO_AUTHOR_START_TOKEN = "Co-authored-by: "
    private const val REVIEWERS_SPLIT = ", "

    fun getFilePath(diffEntry: DiffEntry): String {
      return when (diffEntry.changeType) {
        DiffEntry.ChangeType.DELETE -> diffEntry.oldPath
        else -> diffEntry.newPath
      }
    }

    fun getCoAuthorsFromMSG(message: String): Set<UserInfo> {
      val result = mutableSetOf<UserInfo>()
      for (line in message.split("\n")) {
        if (line.startsWith(CO_AUTHOR_START_TOKEN)) {
          val nameEmail = line.trim().removePrefix(CO_AUTHOR_START_TOKEN)
          val emailStart = nameEmail.indexOfLast { it == '<' }
          val email = nameEmail.substring(emailStart + 1, nameEmail.lastIndex).trim()
          val name = nameEmail.substring(0, emailStart).trim()
          result.add(UserInfo(name, email))
        }
      }
      return result
    }
  }

  fun setLastCommit(lastCommitTimestamp: Long) {
    context.lastCommitCommitterTimestamp = lastCommitTimestamp
  }

  private fun getReviewers(message: String): List<String> {
    val idx = message.indexOf(REVIEW_START_TOKEN)
    if (idx == -1) return emptyList()

    val line = message.substring(idx + REVIEW_START_TOKEN.length)
    val newLineIdx = line.indexOf("\n")
    val reviewersLine = if (newLineIdx == -1) line else line.substring(0, newLineIdx)
    return reviewersLine.split(REVIEWERS_SPLIT)
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

  fun processCommit(commitInfo: CommitInfo): Boolean {
    if (commitInfo.numOfParents > 1) return false

    val authors = getAuthorsNameEmailPairs(commitInfo).filter {
      val (name, email) = it
      !context.userMapper.isBot(email, name)
    }
    if (authors.isEmpty()) {
      return false
    }
    val userIds = authors.map {
      val (name, email) = it
      context.userMapper.addUser(name, email)
    }.toSet()
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
      val userId = context.userMapper.addReviewerName(reviewer)
      context.filesOwnership.computeIfAbsent(fileId) { HashMap() }
        .computeIfAbsent(userId) { ContributionsByUser() }
        .addReview(authorCommitTimestamp, context.lastCommitCommitterTimestamp)
    }
  }

  private fun getAuthorsNameEmailPairs(commit: CommitInfo): Set<Pair<String, String>> {
    val result = mutableSetOf<Pair<String, String>>()
    result.addAll(getCoAuthorsFromMSG(commit.fullMessage).map { it.userName to it.userEmail })
    val authorName = commit.authorUserInfo.userName
    val authorEmail = commit.authorUserInfo.userEmail
    result.add(authorName to authorEmail)
    return result
  }
}
