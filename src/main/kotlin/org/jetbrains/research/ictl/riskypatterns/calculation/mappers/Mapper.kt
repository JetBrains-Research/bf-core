package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

interface Mapper {
  val entityToId: Map<String, Int>
  val idToEntity: Map<Int, String>
}
