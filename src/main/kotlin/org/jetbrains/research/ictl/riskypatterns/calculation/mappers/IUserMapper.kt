package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

interface IUserMapper : Mapper {
  val reviewerNameToUserId: Map<String, Int>
  val idToName: Map<Int, String>
  fun getNameToEmailMap(): Map<String, String>
}
