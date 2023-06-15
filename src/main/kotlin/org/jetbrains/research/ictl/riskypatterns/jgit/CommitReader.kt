package org.jetbrains.research.ictl.riskypatterns.jgit

import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter
import org.eclipse.jgit.util.io.NullOutputStream
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitInfo
import java.time.Duration
import java.util.*

class CommitReader(
  private val repository: Repository,
) {

  fun commitsFromHead(daysGap: Long): Sequence<CommitInfo> {
    val headId = repository.resolve(Constants.HEAD) ?: return emptySequence()

    RevWalk(repository).use { walk ->
      val head = walk.parseCommit(headId)
      walk.markStart(head)
      walk.revFilter = CommitTimeRevFilter.between(
        Date.from(head.commitDate().toInstant().minus(Duration.ofDays(daysGap))),
        head.commitDate(),
      )

      repository.newObjectReader().use { reader ->
        DiffFormatter(NullOutputStream.INSTANCE).use { formatter ->
          formatter.setRepository(repository)
          formatter.isDetectRenames = true

          val converter = RevCommitToCommitInfoConverter(reader, formatter)
          return walk.asSequence()
            .map { commit ->
              val commitInfo = converter.convert(commit)
              commit.disposeBody()
              commitInfo
            }
        }
      }
    }
  }

  private fun RevCommit.commitDate() = Date(commitTime * 1000L)
}
