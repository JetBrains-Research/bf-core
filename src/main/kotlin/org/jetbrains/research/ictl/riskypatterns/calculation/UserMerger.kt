package org.jetbrains.research.ictl.riskypatterns.calculation

import org.eclipse.jgit.lib.Repository
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.NMaxHeap
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitProcessor
import org.jetbrains.research.ictl.riskypatterns.jgit.CommitsProvider
import kotlin.math.max
import kotlin.math.min

class UserMerger(val botFilter: BotFilter? = null) {

  private class UserMergeData(val userInfo: UserInfo) {
    companion object {
      private val partsToRemove = setOf("jr", "admin", "support", "anonymous", "anon", "user", "cvs")
      private val timezones = setOf("cst", "pst", "pdt", "cdt", "gmt", "bst", "cet", "cest")
      private const val SIZE = 3
      private val DELIMITERS = setOf(' ', '+', '-', ',', '.', '_', ';')

      private fun cleanName(name: String): String {
        val lowerCaseNoUnicode = name.lowercase().filter { c -> c.isLowerCase() || c == ' ' }
        val split = splitName(lowerCaseNoUnicode).toMutableList()
        split.removeAll(partsToRemove)
        split.removeAll(timezones)
        return split.joinToString(" ")
      }

      private fun fixCapitalisation(string: String): String {
        var result = ""
        for ((idx, c) in string.withIndex()) {
          if (idx == 0 || !(string[idx - 1]).isUpperCase()) {
            result += c
          } else {
            result += c.lowercase()
          }
        }
        return result
      }

      private fun splitName(name: String): List<String> {
        var result = fixCapitalisation(name)
        for (delimiter in DELIMITERS) {
          result = result.replace(delimiter, ' ')
        }
        return result.split(" ")
      }

      fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        if (lhs == rhs) {
          return 0
        }
        if (lhs.isEmpty()) {
          return rhs.length
        }
        if (rhs.isEmpty()) {
          return lhs.length
        }

        val lhsLength = lhs.length + 1
        val rhsLength = rhs.length + 1

        var cost = Array(lhsLength) { it }
        var newCost = Array(lhsLength) { 0 }

        for (i in 1 until rhsLength) {
          newCost[0] = i

          for (j in 1 until lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
          }

          val swap = cost
          cost = newCost
          newCost = swap
        }

        return cost[lhsLength - 1]
      }

      fun score(lhs: CharSequence, rhs: CharSequence) = 1.0 - levenshtein(lhs, rhs) / max(lhs.length, rhs.length).toDouble()

      fun checkSize(vararg strings: String): Boolean =
        !strings.any { it.length < SIZE }

      fun compare1(userData1: UserMergeData, userData2: UserMergeData, heap: NMaxHeap<Double>) {
        heap.add(
          min(
            score(userData1.firstName, userData2.firstName),
            max(
              score(userData1.penultimateName, userData2.lastName),
              score(userData1.lastName, userData2.lastName),
            ),
          ),
        )
        heap.add(
          min(
            score(userData1.firstName, userData2.lastName),
            max(
              score(userData1.penultimateName, userData2.firstName),
              score(userData1.lastName, userData2.firstName),
            ),
          ),
        )
        heap.add(
          min(
            score(userData1.lastName, userData2.firstName),
            max(
              score(userData1.penultimateName, userData2.lastName),
              score(userData1.firstName, userData2.lastName),
            ),
          ),
        )
      }

      fun Boolean.toDouble() = if (this) 1.0 else 0.0

      fun compareEmailBase(userData1: UserMergeData, userData2: UserMergeData, heap: NMaxHeap<Double>) {
        val base1 = userData2.firstName.first() + userData2.lastName
        heap.add(userData1.emailBase.contains(base1).toDouble())

        val base2 = userData2.firstName + userData2.lastName.first()
        heap.add(userData1.emailBase.contains(base2).toDouble())

        heap.add(2 * (userData1.emailBase.contains(userData2.firstName) && userData1.emailBase.contains(userData2.lastName)).toDouble())
      }
    }

    val name: String = cleanName(userInfo.userName)
    val email: String = userInfo.userEmail.lowercase()
    val firstName: String
    val lastName: String
    val penultimateName: String
    val emailBase: String = email.split('@').first()
    var authorId = -1

    init {
      val nameParts = name.split(" ")
      firstName = nameParts.firstOrNull() ?: ""
      lastName = nameParts.lastOrNull() ?: ""
      penultimateName = if (nameParts.size > 2) nameParts[nameParts.lastIndex - 1] else ""
    }

    fun compare(userMergeData: UserMergeData, threshold: Double = 0.95): Boolean {
      val heap = NMaxHeap<Double>(2)

      if (checkSize(this.name, userMergeData.name)) {
        heap.add(score(this.name, userMergeData.name))
        heap.add((this.name.replace(" ", "") == userMergeData.name).toDouble())
        heap.add((userMergeData.name.replace(" ", "") == this.name).toDouble())
        heap.add((this.name.replace(" ", "") == userMergeData.name.replace(" ", "")).toDouble())
      }

      if (checkSize(this.firstName, this.lastName, userMergeData.firstName, userMergeData.lastName)) {
        val ffScore = score(this.firstName, userMergeData.firstName)
        val fpScore = score(this.firstName, userMergeData.penultimateName)
        val pfScore = score(this.penultimateName, userMergeData.firstName)
        if (checkSize(this.penultimateName, userMergeData.penultimateName)) {
          heap.add(
            min(
              ffScore,
              max(
                ffScore,
                max(
                  fpScore,
                  pfScore,
                ),
              ),
            ),
          )

          heap.add(
            min(
              ffScore,
              max(
                pfScore,
                max(
                  fpScore,
                  ffScore,
                ),
              ),
            ),
          )

          heap.add(
            min(
              ffScore,
              max(
                pfScore,
                max(
                  fpScore,
                  ffScore,
                ),
              ),
            ),
          )
        } else if (checkSize(this.penultimateName)) {
          compare1(this, userMergeData, heap)
        } else if (checkSize(userMergeData.penultimateName)) {
          compare1(userMergeData, this, heap)
        } else {
          heap.add(
            min(
              ffScore,
              score(this.lastName, userMergeData.lastName),
            ),
          )
          heap.add(
            min(
              score(this.firstName, userMergeData.lastName),
              score(this.lastName, userMergeData.firstName),
            ),
          )
        }
      }

      if (checkSize(this.emailBase, userMergeData.emailBase)) {
        heap.add(2 * (this.email == userMergeData.email).toDouble())
        heap.add(score(this.emailBase, userMergeData.emailBase))
      }

      if (checkSize(
          this.firstName,
          this.lastName,
          this.emailBase,
          userMergeData.firstName,
          userMergeData.lastName,
          userMergeData.emailBase,
        )
      ) {
        if (this.firstName != this.lastName) {
          compareEmailBase(userMergeData, this, heap)
        }

        if (userMergeData.firstName != userMergeData.lastName) {
          compareEmailBase(this, userMergeData, heap)
        }
      }

      if (heap.isNotEmpty()) {
        return (heap.sum() / heap.size) >= threshold
      }
      return false
    }
  }

  private fun MutableSet<UserInfo>.addNoBot(userInfo: UserInfo) {
    if (botFilter?.isBot(userInfo) == false) {
      this.add(userInfo)
    }
  }

  fun mergeUsers(repository: Repository): Collection<Collection<UserInfo>> {
    val commitsProvider = CommitsProvider(repository)
    val set = mutableSetOf<UserInfo>()
    for (commit in commitsProvider) {
      set.addNoBot(commit.authorUserInfo)
      set.addNoBot(commit.committerUserInfo)
      CommitProcessor.getCoAuthorsFromMSG(commit.fullMessage).forEach {
        set.addNoBot(it)
      }
    }

    val users = mutableListOf<UserInfo>()
    for (userInfo in set) {
      if (botFilter?.isBot(userInfo) == false) {
        users.add(userInfo)
      }
    }
    return mergeUsers(users)
  }

  fun mergeUsers(usersInfo: Collection<UserInfo>): Collection<Collection<UserInfo>> {
    val userMergeData = usersInfo.map { UserMergeData(it) }
    var id = 0
    for ((idx1, user1) in userMergeData.withIndex()) {
      if (user1.authorId == -1) {
        user1.authorId = id
        id++
      }

      for (idx2 in idx1 + 1..userMergeData.lastIndex) {
        val user2 = userMergeData[idx2]
        if (user1.compare(user2)) {
          if (user1.authorId != -1 && user2.authorId != -1) {
            val newId = min(user1.authorId, user2.authorId)
            user1.authorId = newId
            user2.authorId = newId
          } else {
            user2.authorId = user1.authorId
          }
        }
      }
    }

    val result = HashMap<Int, MutableList<UserInfo>>()
    userMergeData.forEach {
      result.computeIfAbsent(it.authorId) { mutableListOf() }.add(it.userInfo)
    }
    return result.values.filter { users -> users.mapTo(mutableSetOf()) { it.userEmail }.size > 1 }
  }
}
