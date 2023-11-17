package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import java.time.LocalDate


data class CommitEntry(val localDate: LocalDate, val compactCommitsData: List<CompactCommitData>)
