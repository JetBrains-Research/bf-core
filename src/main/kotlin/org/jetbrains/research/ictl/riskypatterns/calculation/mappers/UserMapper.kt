package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

import kotlinx.serialization.Serializable
import org.eclipse.jgit.revwalk.RevCommit

/**
 * Stores Space user ids (emails if no user id is available)
 */

@Serializable
class UserMapper(private val botsLogins: Set<String> = emptySet()) : Mapper() {
  companion object {
    val defaultBots = setOf("dependabot", "[bot]")
  }

  private val nameToUserId = HashMap<String, Int>()

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

  fun isBot(email: String) = botsLogins.any { email.contains(it) } || defaultBots.any { email.contains(it) }
}
