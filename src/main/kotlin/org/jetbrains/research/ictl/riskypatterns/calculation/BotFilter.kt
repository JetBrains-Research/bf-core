package org.jetbrains.research.ictl.riskypatterns.calculation

import kotlinx.serialization.Serializable
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo

@Serializable
class BotFilter(private val botsParts: Collection<String> = emptySet()) {
  companion object {
    val defaultBotParts = setOf("dependabot", "[bot]", "testingbot")
    val defaultEmailBots = setOf("info@testingbot", "noreply@github.com")
    val defaultNameBots = setOf("GitHub", "TestingBot")
    val botRegex = Regex("\\bbot\\b")
  }

  private fun checkName(name: String): Boolean {
    return botsParts.any { name.contains(it) } ||
      defaultBotParts.any { name.contains(it) } ||
      defaultNameBots.any { name.contains(it) } ||
      name.lowercase().contains(botRegex)
  }

  private fun checkEmail(email: String): Boolean {
    return botsParts.any { email.contains(it) } ||
      defaultBotParts.any { email.contains(it) } ||
      defaultEmailBots.any { email.contains(it) }
  }

  fun isBot(email: String, name: String): Boolean {
    return checkEmail(email) || checkName(name)
  }

  fun isBot(userInfo: UserInfo): Boolean {
    return checkEmail(userInfo.userEmail) || checkName(userInfo.userName)
  }
}
