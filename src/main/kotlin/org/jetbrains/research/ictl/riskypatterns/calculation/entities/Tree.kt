package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import kotlinx.serialization.Serializable

@Serializable
data class Tree(
  val name: String,
  val path: String,
  // TODO: add
  var bytes: Long = -1,
  var busFactorStatus: BusFactorStatus? = null,
  var users: List<UserVis> = emptyList(),
  val children: MutableList<Tree> = mutableListOf(),
) {
  fun getFileNames(): List<String> {
    val result = mutableListOf<String>()
    val queue = ArrayDeque<Tree>()
    queue.add(this)
    while (queue.isNotEmpty()) {
      val node = queue.removeLast()
      val children = node.children
      if (children.isEmpty()) {
        result.add(node.path)
      } else {
        queue.addAll(children)
      }
    }
    return result
  }

  fun getNode(filePath: String): Tree? {
    val parts = filePath.split("/")
    var node = this
    for (part in parts) {
      node = node.children.find { it.name == part } ?: return null
    }
    return node
  }
}
