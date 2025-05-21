package rolling_window

import kotlin.math.pow

class RollingWindow(private val seconds: Int) {
    private val points = ArrayDeque<Pair<Long, Double>>()
    fun add(tMillis: Long, price: Double) {
        points.addLast(tMillis to price)
        val cutoff = tMillis - seconds * 1_000
        while (points.isNotEmpty() && points.first().first < cutoff) points.removeFirst()
    }
    fun slope(): Double? {
        val n = points.size
        if (n < 2) return null
        val t0 = points.first().first
        var sumT = 0.0
        var sumP = 0.0
        var sumTT = 0.0
        var sumTP = 0.0
        points.forEach { (t, p) ->
            val x = (t - t0) / 1000.0
            sumT += x
            sumP += p
            sumTT += x * x
            sumTP += x * p
        }
        val denom = n * sumTT - sumT.pow(2)
        return if (denom == 0.0) null else (n * sumTP - sumT * sumP) / denom
    }
}

