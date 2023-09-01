package org.jetbrains.research.ictl.riskypatterns

import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.util.FS
import org.jetbrains.research.ictl.riskypatterns.calculation.BotFilter
import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactor
import org.jetbrains.research.ictl.riskypatterns.calculation.UserMerger
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
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
    private val mergedUsersResult = File(localTestData, "user_merge_result")
    private val cleanBFResult = File(localTestData, "bf_result")
    private val botFilterMergedUsersBFResult = File(localTestData, "bf_botFilter_mergedUsers_result")
    private val botFilterBFResult = File(localTestData, "bf_botFilter_result")
    private val mergedUsersBFResult = File(localTestData, "bf_mergedUsers_result")
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
  fun compareWithPreviousVersion() = runBFTest(cleanBFResult)

  @Test
  fun testUserMerger() {
    val repository = FileRepository(gitFile)
    val botFilter = BotFilter()
    val merger = UserMerger(botFilter)
    val users = merger.mergeUsers(repository)
    val testUsers = Json.decodeFromString<Collection<Collection<UserInfo>>>(mergedUsersResult.readText())
    assertEquals(users, testUsers)
  }

  @Test
  fun testBFUserMergerBotFilter() = runBFTest(botFilterMergedUsersBFResult, useBotFilter = true, useUserMerger = true)

  @Test
  fun testBFUserMerger() = runBFTest(mergedUsersBFResult, useBotFilter = false, useUserMerger = true)

  @Test
  fun testBFBotFilter() = runBFTest(botFilterBFResult, useBotFilter = true, useUserMerger = false)

  private fun runBFTest(previousResultFile: File, useBotFilter: Boolean = false, useUserMerger: Boolean = false) {
    var botFilter: BotFilter? = null
    var mergedUsers: Collection<Collection<UserInfo>> = emptyList()
    val repository = FileRepository(gitFile)

    if (useBotFilter) {
      botFilter = BotFilter()
    }
    if (useUserMerger) {
      botFilter = BotFilter()
      val merger = UserMerger(botFilter)
      mergedUsers = merger.mergeUsers(repository)
    }

    val tree = runBF(botFilter, mergedUsers)
    val treeTest = Json.decodeFromString<Tree>(previousResultFile.readText())
    compareTrees(tree, treeTest)
  }

  private fun runBF(botFilter: BotFilter? = null, mergedUsers: Collection<Collection<UserInfo>> = emptyList()): Tree {
    val bf = BusFactor()
    val repository = FileRepository(gitFile)
    val commitsProvider = CommitsProvider(repository)
    val fileInfoProvider = FileInfoProvider(repository)
    return bf.calculate("test", commitsProvider, fileInfoProvider, botFilter, mergedUsers)
  }

  private fun compareTrees(tree1: Tree, tree2: Tree) {
    val fileNames1 = tree1.getFileNames().toSet()
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
