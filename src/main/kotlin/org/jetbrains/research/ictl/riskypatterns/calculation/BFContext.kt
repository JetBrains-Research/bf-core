package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.IFileMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.IUserMapper

interface BFContext {
  val filesOwnership: Map<Int, Map<Int, ContributionsByUser>>
  val weightedOwnership: Map<Int, Pair<Int, Double>>
  val userMapper: IUserMapper
  val fileMapper: IFileMapper
  val configSnapshot: BusFactorConfigSnapshot
}
