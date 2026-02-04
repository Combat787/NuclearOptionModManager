package com.combat.nomm

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun <T> List<T>.sortFilterByQuery(
    query: String,
    minSimilarity: Double = 0.25,
    block: (T, String) -> Pair<T, Double>
): List<T> {
    if (query.isBlank()) return this

    return this.mapNotNull {
        val result = block(it, query)
        if (result.second >= minSimilarity) result else null
    }
        .sortedByDescending { it.second }
        .map { it.first }
}

fun fuzzyPowerScore(query: String, target: String): Double {
    if (query.isEmpty()) return 1.0
    val q = query.lowercase()
    val t = target.lowercase()
    if (t == q) return 1.0
    if (t.startsWith(q)) return 0.9

    val qLen = q.length
    val tLen = t.length

    var anyWordStarts = false
    val acronymBuilder = StringBuilder()
    var nextIsStart = true

    for (char in t) {
        if (char == ' ' || char == '-' || char == '_' || char == '.') {
            nextIsStart = true
        } else if (nextIsStart) {
            acronymBuilder.append(char)
            if (t.startsWith(q, t.indexOf(char))) anyWordStarts = true
            nextIsStart = false
        }
    }

    if (anyWordStarts) return 0.85
    if (acronymBuilder.toString().contains(q)) return 0.8

    var sequenceIndex = 0
    for (i in 0 until tLen) {
        if (sequenceIndex < qLen && t[i] == q[sequenceIndex]) {
            sequenceIndex++
        }
    }
    if (sequenceIndex == qLen) return 0.7
    if (t.contains(q)) return 0.65

    val maxDist = (qLen * 0.4).toInt().coerceAtLeast(1)
    val dist = measureDamerauLevenshtein(q, t, maxDist)

    if (dist > maxDist) return 0.0
    return 0.5 * (1.0 - dist.toDouble() / max(qLen, tLen))
}

fun measureDamerauLevenshtein(source: CharSequence, target: CharSequence, threshold: Int = Int.MAX_VALUE): Int {
    val sLen = source.length
    val tLen = target.length

    if (sLen == 0) return tLen
    if (tLen == 0) return sLen
    if (abs(sLen - tLen) > threshold) return Int.MAX_VALUE

    var prevRow = IntArray(tLen + 1) { it }
    var currRow = IntArray(tLen + 1)
    var transRow = IntArray(tLen + 1)

    for (i in 1..sLen) {
        currRow[0] = i
        val sChar = source[i - 1]
        var minRowDist = i

        for (j in 1..tLen) {
            val tChar = target[j - 1]
            val cost = if (sChar == tChar) 0 else 1

            var dist = min(currRow[j - 1] + 1, prevRow[j] + 1)
            dist = min(dist, prevRow[j - 1] + cost)

            if (i > 1 && j > 1 && sChar == target[j - 2] && source[i - 2] == tChar) {
                dist = min(dist, transRow[j - 2] + cost)
            }

            currRow[j] = dist
            minRowDist = min(minRowDist, dist)
        }

        if (minRowDist > threshold) return Int.MAX_VALUE

        val temp = transRow
        transRow = prevRow
        prevRow = currRow
        currRow = temp
    }

    return prevRow[tLen]
}