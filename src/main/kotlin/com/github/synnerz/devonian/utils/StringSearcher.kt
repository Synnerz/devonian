package com.github.synnerz.devonian.utils

import java.util.ArrayDeque

// Aho-Corasick
class StringSearcher(private val patterns: List<String>) {
    private val root = Node()

    init {
        patterns.forEachIndexed { i, str ->
            var curr = root
            for (c in str) {
                curr = curr.children.getOrPut(c) { Node() }
            }
            curr.output.add(i)
        }
    }

    fun build() {
        val q = ArrayDeque<Node>()

        for ((_, child) in root.children) {
            child.fail = root
            q.add(child)
        }

        while (!q.isEmpty()) {
            val curr = q.poll()!!

            for ((c, child) in curr.children) {
                var failNode = curr.fail
                while (failNode != null && !failNode.children.containsKey(c)) {
                    failNode = failNode.fail
                }

                failNode = failNode?.children?.get(c) ?: root
                child.fail = failNode

                child.output.addAll(failNode.output)

                q.add(child)
            }
        }
    }

    fun search(text: String): List<Match> {
        val results = mutableListOf<Match>()
        var curr = root

        for (i in text.indices) {
            val c = text[i]

            var tempCurr: Node? = curr
            while (tempCurr != null && !tempCurr.children.containsKey(c)) {
                tempCurr = tempCurr.fail
            }

            if (tempCurr == null) {
                curr = root
                continue
            }

            curr = tempCurr.children[c]!!

            for (patternIndex in curr.output) {
                val pattern = patterns[patternIndex]
                val startIndex = i - pattern.length + 1
                results.add(Match(patternIndex, pattern, startIndex, i))
            }
        }

        return results
    }

    data class Match(
        val patternIndex: Int,
        val pattern: String,
        val startIndex: Int,
        val endIndex: Int
    )

    private class Node {
        val children = mutableMapOf<Char, Node>()
        var fail: Node? = null
        val output = mutableListOf<Int>()
    }
}