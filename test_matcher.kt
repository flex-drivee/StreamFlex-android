import kotlin.math.max

fun levenshtein(first: String, second: String): Int {
    val costs = IntArray(second.length + 1)
    for (j in costs.indices) { costs[j] = j }
    for (i in first.indices) {
        var previous = i
        costs[0] = i + 1
        for (j in second.indices) {
            val current = costs[j + 1]
            costs[j + 1] = minOf(
                costs[j + 1] + 1,
                costs[j] + 1,
                previous + if (first[i] == second[j]) 0 else 1
            )
            previous = current
        }
    }
    return costs.last()
}

fun similarity(first: String, second: String): Double {
    var a = first.lowercase().replace(Regex("^(the|a|an)\\s+"), "").replace(Regex("[^a-z0-9 ]"), "")
    var b = second.lowercase().replace(Regex("^(the|a|an)\\s+"), "").replace(Regex("[^a-z0-9 ]"), "")

    if (a == b) return 1.0

    val patternA = Regex("\\b${Regex.escape(a)}\\b")
    val patternB = Regex("\\b${Regex.escape(b)}\\b")
    val matchA = patternA.find(b)
    val matchB = patternB.find(a)
    
    print("a=$a, b=$b | ")
    
    if ((matchA != null && b.length > 3) || (matchB != null && a.length > 3)) {
        val indexA = matchA?.range?.first ?: Int.MAX_VALUE
        val indexB = matchB?.range?.first ?: Int.MAX_VALUE
        print("MATCHED CONTAINMENT indexA=$indexA indexB=$indexB | ")
        if (indexA == 0 || indexB == 0) {
            return 0.90
        } else {
            return 0.80
        }
    }

    val distance = levenshtein(a, b)
    val maxLength = max(a.length, b.length)
    var sim = 1.0 - distance.toDouble() / maxLength.toDouble()
    return sim
}

println("Sim: " + similarity("Coyote vs. Acme", "Coyote"))
println("Sim: " + similarity("Coyote vs. Acme", "Coyote vs. Acme [CAM] 1080p"))
