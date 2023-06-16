package org.jetbrains.research.ictl.riskypatterns

import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.util.FS
import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactor
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.jgit.CommitsProvider
import org.jetbrains.research.ictl.riskypatterns.jgit.FileInfoProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusFactorTest {

  companion object {
    private val localTestData = File("src/test/testData/")
    private val repositoryDir = File(localTestData, "repository")
    private val gitFile = File(repositoryDir, ".git")
    private val previousResult = File(localTestData, "bf_result")
  }

  @BeforeEach
  fun prepareRepository() {
    if (RepositoryCache.FileKey.isGitRepository(gitFile, FS.DETECTED)) return

    val repoURI = "https://github.com/facebook/react.git"
    repositoryDir.mkdir()

    Git.cloneRepository()
      .setURI(repoURI)
      .setDirectory(repositoryDir)
      .setBranchesToClone(listOf("refs/heads/main"))
      .setNoCheckout(true)
      .call().use { result ->
        println("Finish loading repo $repoURI")
        println("Repository inside: " + result.repository.directory)
      }

    val repository = FileRepository(gitFile)
    val git = Git(repository)
    git.reset().setRef("e1e68b9f7ffecf98e1b625cfe3ab95741be1417b").call()
  }

  @Test
  fun compareWithPreviousVersion() {
    val bf = BusFactor()
    val repository = FileRepository(gitFile)
    val commitsProvider = CommitsProvider(repository)
    val fileInfoProvider = FileInfoProvider(repository)
    val tree = bf.calculate("test", commitsProvider, fileInfoProvider)
    val json = Json { encodeDefaults = false }
    val treeTest = json.decodeFromString<Tree>(previousResult.readText())
    compareTrees(tree, treeTest)
  }

  private fun compareTrees(tree1: Tree, tree2: Tree) {
    val fileNames1 = tree1.getFileNames().toSet()
    val fileNames2 = tree1.getFileNames().toSet()
    assertEquals(fileNames1, fileNames2, "Not same file trees")

    for (fileName in fileNames1) {
      val node1 = tree1.getNode(fileName)!!
      val node2 = tree2.getNode(fileName)!!

      val usersSize1 = node1.users.size
      val usersSize2 = node2.users.size
      assertEquals(usersSize1, usersSize2, "Not equal number of users.")

      val onlyInNode1 = node1.users.mapTo(mutableSetOf()) { it.email } - node2.users.mapTo(mutableSetOf()) { it.email }
      val onlyInNode2 = node2.users.mapTo(mutableSetOf()) { it.email } - node1.users.mapTo(mutableSetOf()) { it.email }
      assertTrue(onlyInNode1.isEmpty(), "Users only in one node $onlyInNode1 for file $fileName")
      assertTrue(onlyInNode2.isEmpty(), "Users only in one node $onlyInNode2 for file $fileName")

      for (user1 in node1.users) {
        val user2 = node2.users.find { it.email == user1.email }
          ?: throw Exception("Can't find user: ${user1.email} in $fileName.")
        assertEquals(user1.authorship, user2.authorship, 0.001)
      }

      val nodeBF1 = node1.busFactorStatus!!.busFactor
      val nodeBF2 = node2.busFactorStatus!!.busFactor
      assertEquals(nodeBF1, nodeBF2)
    }
  }
}
