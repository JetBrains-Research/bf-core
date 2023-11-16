package org.jetbrains.research.ictl.riskypatterns.calculation

import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CommitInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.CompactCommitData
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.FileInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.Tree
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserInfo
import org.jetbrains.research.ictl.riskypatterns.calculation.entities.UserVis
import org.jetbrains.research.ictl.riskypatterns.calculation.mappers.UserMapper
import org.jetbrains.research.ictl.riskypatterns.calculation.processors.CommitProcessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BusFactor(context: BusFactorComputationContext) {

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

  private val _context: BusFactorComputationContext
  val context: BFContext
  private val commitProcessor: CommitProcessor

  init {
    _context = context
    this.context = _context
    commitProcessor = CommitProcessor(_context)
  }

  constructor(
    botFilter: BotFilter? = null,
    mergedUsers: Collection<Collection<UserInfo>> = emptyList(),
    configSnapshot: BusFactorConfigSnapshot = BusFactorConfigSnapshot.getDefault(),
  ) : this(BusFactorComputationContext(configSnapshot, UserMapper(botFilter, mergedUsers)))

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
  private fun buildTree(treeName: String, filePathsToBytes: Iterable<FileInfo>): Tree {
    val root = Tree(treeName, ".")
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

  fun setLastCommit(lastCommitInfo: CommitInfo) {
    _context.lastCommitCommitterTimestamp = lastCommitInfo.committerTimestamp
    _context.lastCommitHash = lastCommitInfo.hash
  }

  fun proceedCommits(commitsInfo: Iterable<CommitInfo>) {
    for (commitInfo in commitsInfo) {
      commitProcessor.processCommit(commitInfo)
    }
  }

  fun consumeCommit(commitInfo: CommitInfo) = commitProcessor.processCommit(commitInfo)

  fun consumeCompactCommitData(compactCommitData: CompactCommitData, timestamp: Long) = commitProcessor.processCompactCommitData(compactCommitData, timestamp)

  fun clearResults() = BusFactorCalculation(_context).clearResults()

  fun calculate(
    treeName: String,
    filePathsToBytes: Iterable<FileInfo>,
  ): Tree {
    val busFactorCalculation = BusFactorCalculation(_context)
    busFactorCalculation.computeAuthorship()
    val root = buildTree(treeName, filePathsToBytes)
    calculateBusFactorForTree(root, busFactorCalculation)
    return root
  }
}
