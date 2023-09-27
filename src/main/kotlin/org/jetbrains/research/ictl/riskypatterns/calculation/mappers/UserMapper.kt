package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

import kotlinx.serialization.Serializable
import org.jetbrains.research.ictl.riskypatterns.calculation.BotFilter
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo

/**
 * Stores Space user ids (emails if no user id is available)
 */

@Serializable
class UserMapper() : Mapper() {
  private var botFilter: BotFilter? = null
  private val reviewerNameToUserId = HashMap<String, Int>()
  private val idToName = HashMap<Int, String>()

  constructor(
    botFilter: BotFilter? = null,
    mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
  ) : this() {
    this.botFilter = botFilter
    for (userEmails in mergedUsers) {
      addUserEmails(userEmails)
    }
  }

  // TODO: change logic, reuse merger
  fun addReviewerName(name: String): Int {
    val lowercaseName = name.lowercase()
    return reviewerNameToUserId[lowercaseName] ?: run {
      val id = add(lowercaseName)
      reviewerNameToUserId[lowercaseName] = id
      id
    }
  }

  private fun addUserEmails(emails: Collection<UserInfo>) {
    val id = add(emails.first().userEmail.lowercase())
    val name = emails.first().userName
    for (email in emails) {
      val lowercaseEmail = email.userEmail.lowercase()
      entityToId[lowercaseEmail] = id
    }
    idToName[id] = name
  }

  fun addUser(userInfo: UserInfo) = addUser(userInfo.userName, userInfo.userEmail)

  fun addUser(name: String, email: String): Int {
    val lowercaseEmail = email.lowercase()
    val nameForReviewer = lowercaseEmail.split("@").first()
    if (contains(nameForReviewer)) {
      val id = entityToId.remove(nameForReviewer)!!
      entityToId[lowercaseEmail] = id
      idToEntity[id] = lowercaseEmail
      reviewerNameToUserId[nameForReviewer] = id
      return id
    }
    val id = add(lowercaseEmail)
    reviewerNameToUserId[nameForReviewer] = id
    idToName[id] = name
    return id
  }

  fun getNameToEmailMap(): Map<String, String> {
    val result = HashMap<String, String>()
    for ((id, email) in idToEntity) {
      val name = idToName[id]!!
      result[email] = name
    }
    return result
  }

  fun isBot(email: String, name: String = "") = botFilter?.isBot(email, name) ?: false

  fun isBot(userInfo: UserInfo) = isBot(userInfo.userEmail, userInfo.userName)
}
