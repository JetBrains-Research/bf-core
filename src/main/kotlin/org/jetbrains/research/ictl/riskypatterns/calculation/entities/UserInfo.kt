package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val userName: String, val userEmail: String)
