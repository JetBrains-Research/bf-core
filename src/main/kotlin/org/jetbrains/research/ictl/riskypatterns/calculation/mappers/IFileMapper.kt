package org.jetbrains.research.ictl.riskypatterns.calculation.mappers

interface IFileMapper : Mapper {
  fun getRealFileId(fileId: Int): Int
}
