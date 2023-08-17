package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

import kotlinx.serialization.Serializable
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.research.ictl.riskypatterns.calculation.BotFilter

/**
 * Stores Space user ids (emails if no user id is available)
 */

@Serializable
class UserMapper() : Mapper() {
  private var botFilter: BotFilter? = null
  private val nameToUserId = HashMap<String, Int>()

  constructor(botsLogins: Collection<String> = emptySet(), sameUserEmails: Collection<List<String>> = emptySet()) : this() {
    botFilter = BotFilter(botsLogins)
    for (userEmails in sameUserEmails) {
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

  private fun addUserEmails(emails: List<String>) {
    val id = add(emails.first().lowercase())
    for (email in emails) {
      val lowercaseEmail = email.lowercase()
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
