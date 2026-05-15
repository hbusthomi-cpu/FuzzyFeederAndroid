package com.example.fuzzyfeeder

import kotlin.math.max
import kotlin.math.min

/**
 * Converted from fuzzy_feeder.fis
 * System: fuzzy_feeder_temp_do
 * Type: Mamdani
 * Inputs: Temperature [10..45], DissolvedOxygen [0..100]
 * Output: FeedingPercent [0..6]
 * AND=min, OR=max, implication=min, aggregation=max, defuzzification=centroid
 */
object FuzzyFeeder {

    private fun trimf(x: Double, a: Double, b: Double, c: Double): Double {
        return when {
            x <= a || x >= c -> 0.0
            x == b -> 1.0
            x < b -> (x - a) / (b - a)
            else -> (c - x) / (c - b)
        }.coerceIn(0.0, 1.0)
    }

    private fun trapmf(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
        return when {
            x <= a || x >= d -> 0.0
            x >= b && x <= c -> 1.0
            x > a && x < b -> if (b == a) 1.0 else (x - a) / (b - a)
            x > c && x < d -> if (d == c) 1.0 else (d - x) / (d - c)
            else -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    private fun temperatureMF(temp: Double): DoubleArray = doubleArrayOf(
        trapmf(temp, 0.0, 0.0, 20.0, 24.0),   // TVL
        trimf(temp, 20.0, 24.0, 26.0),          // TL
        trimf(temp, 24.0, 26.0, 28.0),          // TME
        trimf(temp, 26.0, 28.0, 30.0),          // TH
        trapmf(temp, 28.0, 30.0, 45.0, 45.0)   // TVH
    )

    private fun oxygenMF(oxygen: Double): DoubleArray = doubleArrayOf(
        trapmf(oxygen, 0.0, 0.0, 20.0, 40.0),      // OVL
        trimf(oxygen, 20.0, 40.0, 60.0),           // OL
        trimf(oxygen, 40.0, 60.0, 80.0),           // OME
        trimf(oxygen, 60.0, 80.0, 100.0),          // OH
        trapmf(oxygen, 80.0, 100.0, 100.0, 100.0) // OVH
    )

    private fun feedingMF(index: Int, y: Double): Double {
        return when (index) {
            1 -> trimf(y, 2.0, 2.5, 3.0)   // FVL
            2 -> trimf(y, 2.5, 3.0, 3.5)   // FL
            3 -> trimf(y, 3.0, 3.5, 4.0)   // FME
            4 -> trimf(y, 3.5, 4.0, 4.5)   // FH
            5 -> trimf(y, 4.0, 4.5, 5.0)   // FVH
            else -> 0.0
        }
    }

    // Rules from .fis: each row maps Temperature MF + Oxygen MF -> Feeding MF
    private val ruleOutput = arrayOf(
        intArrayOf(1, 1, 1, 1, 1),
        intArrayOf(1, 2, 2, 2, 2),
        intArrayOf(1, 2, 3, 3, 3),
        intArrayOf(1, 2, 3, 3, 3),
        intArrayOf(1, 2, 3, 4, 5)
    )

    /**
     * Returns recommended feeding percentage.
     * Example: val result = FuzzyFeeder.calculate(27.0, 70.0)
     */
    fun calculate(temperature: Double, dissolvedOxygen: Double): Double {
        val temp = temperature.coerceIn(10.0, 45.0)
        val oxygen = dissolvedOxygen.coerceIn(0.0, 100.0)

        val tMF = temperatureMF(temp)
        val oMF = oxygenMF(oxygen)

        // Aggregate output fuzzy set using Mamdani max-min
        val step = 0.01
        var numerator = 0.0
        var denominator = 0.0
        var y = 0.0

        while (y <= 6.0) {
            var aggregated = 0.0

            for (i in 0..4) {
                for (j in 0..4) {
                    val firingStrength = min(tMF[i], oMF[j])
                    val outputIndex = ruleOutput[i][j]
                    val implied = min(firingStrength, feedingMF(outputIndex, y))
                    aggregated = max(aggregated, implied)
                }
            }

            numerator += y * aggregated
            denominator += aggregated
            y += step
        }

        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
}
