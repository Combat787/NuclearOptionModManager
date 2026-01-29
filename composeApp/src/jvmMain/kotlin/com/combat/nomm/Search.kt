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

    val scored = ArrayList<Pair<T, Double>>(this.size)
    for (item in this) {
        val result = block(item, query)
        if (result.second >= minSimilarity) {
            scored.add(result)
        }
    }

    scored.sortByDescending { it.second }

    val output = ArrayList<T>(scored.size)
    for (pair in scored) {
        output.add(pair.first)
    }
    return output
}

fun fuzzyPowerScore(query: String, target: String): Double {
    if (query.isEmpty()) return 1.0
    if (target == query) return 1.0

    val q = query.lowercase()
    val t = target.lowercase()
    val qLen = q.length
    val tLen = t.length

    if (t == q) return 1.0
    if (t.startsWith(q)) return 0.9

    val words = t.split(' ', '-', '_', '.')
    if (words.any { it.startsWith(q) }) return 0.85

    val acronym = words.mapNotNull { it.firstOrNull() }.joinToString("")
    if (acronym.contains(q)) return 0.8

    var sequenceIndex = 0
    var matches = 0
    for (char in t) {
        if (sequenceIndex < qLen && char == q[sequenceIndex]) {
            sequenceIndex++
            matches++
        }
    }
    if (matches == qLen) return 0.7

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

    val row0 = IntArray(tLen + 1)
    val row1 = IntArray(tLen + 1)
    val row2 = IntArray(tLen + 1)

    for (j in 0..tLen) row0[j] = j

    for (i in 0 until sLen) {
        val sChar = source[i]
        row1[0] = i + 1

        var minRowDist = row1[0]

        for (j in 0 until tLen) {
            val tChar = target[j]
            val cost = if (sChar == tChar) 0 else 1

            var dist = min(row1[j] + 1, row0[j + 1] + 1)
            dist = min(dist, row0[j] + cost)

            if (i > 0 && j > 0 && sChar == target[j - 1] && source[i - 1] == tChar) {
                dist = min(dist, row2[j - 1] + cost)
            }

            row1[j + 1] = dist
            minRowDist = min(minRowDist, dist)
        }

        if (minRowDist > threshold) return Int.MAX_VALUE

        System.arraycopy(row0, 0, row2, 0, row0.size)
        System.arraycopy(row1, 0, row0, 0, row1.size)
    }

    return row0[tLen]
}