package org.jetbrains.research.ictl.riskypatterns.calculation.entities

data class DiffEntry(val oldPath: String, val newPath: String, val changeType: ChangeType) {
  enum class ChangeType {
    ADD, MODIFY, COPY, RENAME, DELETE,
  }
}