package org.jetbrains.research.ictl.riskypatterns.jgit

import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.jetbrains.research.ictl.riskypatterns.calculation.BusFactor

class FilePathToSizeProvider(private val repository: Repository) : Iterable<Pair<String, Long>> {

  override fun iterator(): Iterator<Pair<String, Long>> = FilePathToSizeRepoIterator(repository)

  class FilePathToSizeRepoIterator(repository: Repository) : Iterator<Pair<String, Long>> {
    private val treeWalk = TreeWalk(repository)
    private val reader = repository.newObjectReader()
    private var value: Pair<String, Long>? = null

    init {
      val revWalk = RevWalk(repository)
      val lastCommit = revWalk.use {
        it.parseCommit(repository.resolve(Constants.HEAD))
      }
      treeWalk.addTree(lastCommit.tree)
      treeWalk.isRecursive = false

      value = lookForValue()
    }

    override fun hasNext(): Boolean = value != null

    override fun next(): Pair<String, Long> {
      val v = value
      if (v != null) {
        value = lookForValue()
        return v
      }
      throw Exception("No value found")
    }

    private fun lookForValue(): Pair<String, Long>? {
      while (treeWalk.next()) {
        val filePath = treeWalk.pathString
        var bytes = 0L
        try {
          bytes = reader.getObjectSize(treeWalk.getObjectId(0), Constants.OBJ_BLOB)
          if (treeWalk.isSubtree) {
            treeWalk.enterSubtree()
            continue
          }
        } catch (e: MissingObjectException) {
          BusFactor.log.warn("Missing blob : $filePath : ${e.message} ")
        }
        return filePath to bytes
      }

      return null
    }

  }
}