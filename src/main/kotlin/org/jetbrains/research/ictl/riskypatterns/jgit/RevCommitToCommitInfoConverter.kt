package org.jetbrains.research.ictl.riskypatterns.jgit

import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.DiffEntry

internal class RevCommitToCommitInfoConverter(
  private val reader: ObjectReader,
  private val diffFormatter: DiffFormatter,
) {
  fun convert(commit: RevCommit) =
    CommitInfo(
      commit.authorIdent.emailAddress,
      commit.committerIdent.emailAddress,
      commit.authorIdent.`when`.time,
      commit.committerIdent.`when`.time,
      collectDiffEntries(commit),
      commit.parents.size,
      commit.fullMessage,
    )

  private fun collectDiffEntries(commit: RevCommit): List<DiffEntry> {
    val parentCommitTreeParser = if (commit.parents.isNotEmpty()) {
      val firstParent = commit.parents[0]
      val treeParser = CanonicalTreeParser()
      treeParser.reset(reader, firstParent.tree)
      treeParser
    } else {
      EmptyTreeIterator()
    }
    val currentCommitTreeParser = CanonicalTreeParser()
    currentCommitTreeParser.reset(reader, commit.tree)

    return diffFormatter.scan(parentCommitTreeParser, currentCommitTreeParser)
      .map {
        DiffEntry(
          it.oldPath,
          it.newPath,
          when (it.changeType) {
            org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD -> DiffEntry.ChangeType.ADD
            org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME -> DiffEntry.ChangeType.RENAME
            org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY -> DiffEntry.ChangeType.MODIFY
            org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY -> DiffEntry.ChangeType.COPY
            org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE -> DiffEntry.ChangeType.DELETE
          },
        )
      }
  }
}
