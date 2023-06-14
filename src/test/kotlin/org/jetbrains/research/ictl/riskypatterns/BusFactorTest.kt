package org.jetbrains.research.ictl.riskypatterns

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactor
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitProcessor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class BusFactorTest {

    companion object {
        private val localTestData = File("../testResources/")
        private val repositoryFile = File(localTestData, "repository")
        private val gitFile = File(repositoryFile, ".git")
    }

  @Test
  fun tryFirst() {
    val bf = BusFactor()
    val repository = FileRepository(gitFile)
    val commitsProvider = CommitsProvider(repository)
    val filePathToSizeProvider = FilePathToSizeProvider(repository)
    val tree = bf.calculate("testRepository", commitsProvider, filePathToSizeProvider)
  }
}
