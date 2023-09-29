package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.FileInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserVis
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.UserMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitProcessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BusFactor(
  val repositoryName: String,
  botFilter: BotFilter? = null,
  mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
  configSnapshot: BusFactorConfigSnapshot = BusFactorConfigSnapshot.getDefault(),
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(BusFactor::class.java)

    fun getExtension(fileName: String): CharSequence? {
      val index: Int = fileName.indexOfLast { it == '.' }
      return if (index < 0) {
        null
      } else {
        fileName.subSequence(index + 1, fileName.length)
      }
    }

    fun isValidFilePath(filePath: String, ignoreExtensions: Set<String>): Boolean {
      val extension = getExtension(filePath) ?: return true
      return extension !in ignoreExtensions
    }

    fun isMinorContributor(userAuthorship: Double, sumAuthorship: Double) = (userAuthorship / sumAuthorship) <= 0.05

    fun isMainContributor(userAuthorship: Double, normalizedUserAuthorship: Double) =
      (normalizedUserAuthorship > 0.75) && (userAuthorship > 1)
  }

  private val userMapper = UserMapper(botFilter, mergedUsers)
  private val _context = BusFactorComputationContext(configSnapshot, userMapper)
  val context: BFContext = _context
  private val commitProcessor = CommitProcessor(_context)

  private fun processCommit(commitInfo: CommitInfo): Boolean {
    return commitProcessor.processCommit(commitInfo)
  }

  fun getBusFactorForTree(filePathsToBytes: Iterable<FileInfo>): Tree {
    val busFactorCalculation = BusFactorCalculation(_context)
    val root = buildTree(filePathsToBytes)
    calculateBusFactorForTree(root, busFactorCalculation)
    return root
  }

  private fun calculateBusFactorForTree(root: Tree, busFactorCalculation: BusFactorCalculation) {
    val queue = ArrayDeque<Tree>()
    queue.add(root)

    while (queue.isNotEmpty()) {
      val node = queue.removeLast()

      val children = node.children
      if (children.isNotEmpty()) {
        queue.addAll(children)
      }

      val fileNames = node.getFileNames()
      val userStats = busFactorCalculation.userStats(fileNames)
      val busFactorCalculationResult = busFactorCalculation.computeBusFactorForFiles(fileNames)

      node.busFactorStatus = busFactorCalculationResult.busFactorStatus
      node.users = UserVis.convert(userStats, busFactorCalculationResult.developersSorted)
    }
  }

  // fixme: add filter for files
  private fun buildTree(filePathsToBytes: Iterable<FileInfo>): Tree {
    val root = Tree(repositoryName, ".")
    var allSize = 0L
    filePathsToBytes.forEach { (filePath, bytes) ->
      val parts = filePath.split("/")
      var node = root
      var path = ""
      for (part in parts) {
        if (path.isEmpty()) path = part else path += "/$part"
        node = node.children.find { it.name == part } ?: run {
          val newNode = Tree(part, path, bytes)
          node.children.add(newNode)
          newNode
        }
      }
      allSize += bytes
    }
    root.bytes = allSize
    return root
  }

  fun getNameToEmailMap() = userMapper.getNameToEmailMap()

  fun proceedCommits(commitsInfo: Iterable<CommitInfo>) {
    for (commit in commitsInfo) {
      processCommit(commit)
    }
  }

  fun consumeCommit(commitInfo: CommitInfo) = processCommit(commitInfo)

  fun calculate(
    filePathsToBytes: Iterable<FileInfo>,
  ): Tree {
    return getBusFactorForTree(filePathsToBytes)
  }
}
