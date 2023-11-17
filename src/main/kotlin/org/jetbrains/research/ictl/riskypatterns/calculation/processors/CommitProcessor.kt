package org.jetbrains.research.ictl.riskypatterns.calculation.processors

import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactorComputationContext
import org.jetbrains.research.ictl.riskypatterns.calculation.ContributionsByUser
import org.jetbrains.research.ictl.riskypatterns.calculation.Util
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitEntry
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CompactCommitData
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.DiffEntry
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.HashMap

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

  fun getReviewers(message: String): List<String> {
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
  private fun addDiff(diffEntry: DiffEntry, localDate: LocalDate, userIds: Set<Int>): Int {
    val filePath = getFilePath(diffEntry)
    val fileId = context.fileMapper.add(filePath)
    userIds.forEach {
      addDiff(fileId, it, localDate)
    }
    return fileId
  }

  private fun addDiff(fileId: Int, userId: Int, localDate: LocalDate) {
    val weight = context.filesOwnership.computeIfAbsent(fileId) { HashMap() }.computeIfAbsent(userId) { ContributionsByUser() }
      .addFileChange(localDate, context.lastCommitCommitterLocalDate!!)
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

  private fun checkLastCommit() {
    if (context.lastCommitCommitterLocalDate == null || context.lastCommitHash == null) {
      throw Exception("Set last commit. Before running calculation.")
    }
  }

  fun processCommit(commitInfo: CommitInfo): CommitEntry? {
    checkLastCommit()

    if (commitInfo.numOfParents > 1) return null

    val authors = getAuthorsNameEmailPairs(commitInfo).filter {
      !context.userMapper.isBot(it)
    }
    if (authors.isEmpty()) {
      return null
    }

    val usersIds: Set<Int> = authors.mapTo(mutableSetOf()) {
      context.userMapper.addUser(it)
    }
    val localDate = Util.toLocalDate(commitInfo.authorCommitTimestamp)
    val compactCommitData = mutableListOf<CompactCommitData>()
    val fileIds = HashMap<Int, Int>()

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
          val fileId = addDiff(diffEntry, localDate, usersIds)
          fileIds.compute(fileId) { _, v -> if (v == null) 1 else v + 1 }
        }
      }
    }

    compactCommitData.add(CompactCommitData(usersIds, fileIds, CompactCommitData.Type.COMMIT))

    if (context.configSnapshot.useReviewers) {
      val reviewerFileIds = HashMap<Int, Int>()
      val reviewers = getReviewers(commitInfo.fullMessage)
      val reviewersIds: Set<Int> = reviewers.mapTo(mutableSetOf()) { context.userMapper.addReviewerName(it) }
      for (diffEntry in commitInfo.diffEntries) {
        val fileId = addReviewersForFile(diffEntry, reviewersIds, localDate)
        reviewerFileIds.compute(fileId) { _, v -> if (v == null) 1 else v + 1 }
      }
      compactCommitData.add(CompactCommitData(reviewersIds, fileIds, CompactCommitData.Type.COMMIT))
    }

    return CommitEntry(localDate, compactCommitData)
  }

  fun processCompactCommitData(compactCommitData: CompactCommitData, localDate: LocalDate) {
    checkLastCommit()
    val (usersIds, fileIds) = compactCommitData
    val realFileIds = fileIds.mapKeys { (k, _) -> context.fileMapper.getRealFileId(k) }
    usersIds.forEach {
      if (!context.userMapper.contains(it)) throw Exception("Compact commit data user is not related to the previous calculation.")
    }

    for (userId in usersIds) {
      for ((fileId, count) in realFileIds) {
        for (i in 0 until count) {
          when (compactCommitData.type) {
            CompactCommitData.Type.COMMIT -> addDiff(fileId, userId, localDate)
            CompactCommitData.Type.REVIEW -> addReviewer(fileId, userId, localDate)
          }
        }
      }
    }
  }

  // TODO: replace commitStamp with smth better. Review time is needed. Mb use committer time  instead author
  private fun addReviewersForFile(diffEntry: DiffEntry, reviewersIds: Set<Int>, localDate: LocalDate): Int {
    val filePath = getFilePath(diffEntry)
    val fileId = context.fileMapper.getOrNull(filePath)!!
    reviewersIds.forEach { addReviewer(fileId, it, localDate) }
    return fileId
  }

  private fun addReviewer(fileId: Int, reviewerId: Int, localDate: LocalDate) =
    context.filesOwnership.computeIfAbsent(fileId) { HashMap() }.computeIfAbsent(reviewerId) { ContributionsByUser() }
      .addReview(localDate, context.lastCommitCommitterLocalDate!!)

  private fun getAuthorsNameEmailPairs(commit: CommitInfo): Set<UserInfo> {
    val result = mutableSetOf<UserInfo>()
    result.addAll(getCoAuthorsFromMSG(commit.fullMessage))
    val authorName = commit.authorUserInfo.userName
    val authorEmail = commit.authorUserInfo.userEmail
    result.add(UserInfo(authorName, authorEmail))
    return result
  }
}
