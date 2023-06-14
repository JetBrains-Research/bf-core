package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

import kotlinx.serialization.Serializable

/**
 * Stores Space user ids (emails if no user id is available)
 */

@Serializable
class UserMapper : Mapper() {

  private val nameToUserId = HashMap<String, Int>()

  fun addEmail(authorEmail: String): Int {
    // TODO: why lowercase?
    val email = authorEmail.lowercase()
    val name = email.split("@").first()
    if (contains(name)) {
      val id = entityToId.remove(name)!!
      entityToId[email] = id
      idToEntity[id] = email
      nameToUserId[name] = id
      return id
    }
    val id = add(email)
    nameToUserId[name] = id
    return id
  }

  fun addName(name: String): Int =
    nameToUserId[name] ?: run {
      val id = add(name)
      nameToUserId[name] = id
      id
    }
}
