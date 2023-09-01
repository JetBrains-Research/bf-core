package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

import kotlinx.serialization.Serializable
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.research.ictl.riskypatterns.calculation.BotFilter
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo

/**
 * Stores Space user ids (emails if no user id is available)
 */

@Serializable
class UserMapper() : Mapper() {
  private var botFilter: BotFilter? = null
  private val nameToUserId = HashMap<String, Int>()

  constructor(
    botFilter: BotFilter? = null,
    mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
  ) : this() {
    this.botFilter = botFilter
    for (userEmails in mergedUsers) {
      addUserEmails(userEmails)
    }
  }

  fun add(commit: RevCommit): Int {
    val email = commit.authorIdent.emailAddress
    return addEmail(email)
  }

  fun addName(name: String): Int {
    val lowercaseName = name.lowercase()
    return nameToUserId[lowercaseName] ?: run {
      val id = add(lowercaseName)
      nameToUserId[lowercaseName] = id
      id
    }
  }

  private fun addUserEmails(emails: Collection<UserInfo>) {
    val id = add(emails.first().userEmail.lowercase())
    for (email in emails) {
      val lowercaseEmail = email.userEmail.lowercase()
      entityToId[lowercaseEmail] = id
    }
  }

  fun addEmail(email: String): Int {
    val lowercaseEmail = email.lowercase()
    val name = lowercaseEmail.split("@").first()
    if (contains(name)) {
      val id = entityToId.remove(name)!!
      entityToId[lowercaseEmail] = id
      idToEntity[id] = lowercaseEmail
      nameToUserId[name] = id
      return id
    }
    val id = add(lowercaseEmail)
    nameToUserId[name] = id
    return id
  }

  fun isBot(email: String, name: String = "") = botFilter?.isBot(email, name) ?: false
}
