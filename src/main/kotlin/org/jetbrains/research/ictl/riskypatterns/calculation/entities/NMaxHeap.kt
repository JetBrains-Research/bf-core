package org.jetbrains.research.ictl.riskypatterns.calculation.entities

import java.util.*

class NMaxHeap<T : Comparable<T>>(private val numberOfElements: Int) : PriorityQueue<T>() {

  override fun add(element: T): Boolean {
    val result = offer(element)
    if (size > numberOfElements) remove()
    return result
  }
}
