package org.jetbrains.research.ictl.riskypatterns.calculation.entities


data class CommitEntry(val timestamp: Long, val compactCommitsData: List<CompactCommitData>)
