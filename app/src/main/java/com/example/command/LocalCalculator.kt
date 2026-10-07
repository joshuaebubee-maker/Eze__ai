package com.example.command

import java.text.DecimalFormat
import java.util.Locale
import java.util.regex.Pattern

object LocalCalculator {

    private val df = DecimalFormat("#.##")

    /**
     * Attempts to parse and evaluate mathematical expressions or unit conversions locally.
     * Returns the formatted string result, or null if the query is not a supported mathematical formula.
     */
    fun evaluate(query: String): String? {
        val clean = query.trim().lowercase(Locale.ROOT)
            .removePrefix("what is ")
            .removePrefix("what's ")
            .removePrefix("calculate ")
            .removePrefix("compute ")
            .removePrefix("can you calculate ")
            .removePrefix("please calculate ")
            .trim(' ', '?', '.')

        // 1. Percentage check: "15 percent of 200" or "15% of 200"
        val pctRegex = Pattern.compile("([0-9.]+)\\s*(?:%|percent)\\s+of\\s+([0-9.]+)")
        val pctMatcher = pctRegex.matcher(clean)
        if (pctMatcher.find()) {
            val p = pctMatcher.group(1)?.toDoubleOrNull()
            val total = pctMatcher.group(2)?.toDoubleOrNull()
            if (p != null && total != null) {
                val res = (p / 100.0) * total
                return "$p% of $total is ${df.format(res)}"
            }
        }

        // 2. Unit Conversions
        // "convert 5 km to miles" or "5 kilometers to miles"
        val convRegex = Pattern.compile("(?:convert\\s+)?([0-9.]+)\\s*([a-zA-Z]+)\\s+(?:to|in)\\s+([a-zA-Z]+)")
        val convMatcher = convRegex.matcher(clean)
        if (convMatcher.find()) {
            val value = convMatcher.group(1)?.toDoubleOrNull()
            val fromUnit = convMatcher.group(2)?.lowercase(Locale.ROOT)
            val toUnit = convMatcher.group(3)?.lowercase(Locale.ROOT)
            if (value != null && fromUnit != null && toUnit != null) {
                val converted = convertUnits(value, fromUnit, toUnit)
                if (converted != null) return converted
            }
        }

        // 3. Natural Arithmetic: "25 times 8", "500 divided by 4", "12 plus 45", "100 minus 35"
        val wordArithRegex = Pattern.compile("([0-9.]+)\\s*(times|multiplied by|x|\\*|divided by|/|over|plus|\\+|minus|-)\\s*([0-9.]+)")
        val wordMatcher = wordArithRegex.matcher(clean)
        if (wordMatcher.find()) {
            val a = wordMatcher.group(1)?.toDoubleOrNull()
            val op = wordMatcher.group(2)
            val b = wordMatcher.group(3)?.toDoubleOrNull()
            if (a != null && b != null && op != null) {
                return when (op) {
                    "times", "multiplied by", "x", "*" -> {
                        val res = a * b
                        "${df.format(a)} × ${df.format(b)} = ${df.format(res)}"
                    }
                    "divided by", "/", "over" -> {
                        if (b == 0.0) "Cannot divide by zero."
                        else {
                            val res = a / b
                            "${df.format(a)} ÷ ${df.format(b)} = ${df.format(res)}"
                        }
                    }
                    "plus", "+" -> {
                        val res = a + b
                        "${df.format(a)} + ${df.format(b)} = ${df.format(res)}"
                    }
                    "minus", "-" -> {
                        val res = a - b
                        "${df.format(a)} - ${df.format(b)} = ${df.format(res)}"
                    }
                    else -> null
                }
            }
        }

        // 4. Square root: "sqrt of 144" or "square root of 144"
        val sqrtRegex = Pattern.compile("(?:square root|sqrt)\\s+of\\s+([0-9.]+)")
        val sqrtMatcher = sqrtRegex.matcher(clean)
        if (sqrtMatcher.find()) {
            val n = sqrtMatcher.group(1)?.toDoubleOrNull()
            if (n != null) {
                if (n < 0) return "Square root of a negative number is undefined in real numbers."
                return "The square root of $n is ${df.format(Math.sqrt(n))}"
            }
        }

        return null
    }

    private fun convertUnits(value: Double, from: String, to: String): String? {
        return when {
            // Distance
            (from in listOf("km", "kilometer", "kilometers")) && (to in listOf("miles", "mile", "mi")) ->
                "$value km = ${df.format(value * 0.621371)} miles"
            (from in listOf("miles", "mile", "mi")) && (to in listOf("km", "kilometer", "kilometers")) ->
                "$value miles = ${df.format(value * 1.60934)} km"
            (from in listOf("m", "meter", "meters")) && (to in listOf("feet", "foot", "ft")) ->
                "$value meters = ${df.format(value * 3.28084)} feet"
            (from in listOf("feet", "foot", "ft")) && (to in listOf("m", "meter", "meters")) ->
                "$value feet = ${df.format(value * 0.3048)} meters"
            (from in listOf("cm", "centimeter", "centimeters")) && (to in listOf("inches", "inch", "in")) ->
                "$value cm = ${df.format(value * 0.393701)} inches"
            (from in listOf("inches", "inch", "in")) && (to in listOf("cm", "centimeter", "centimeters")) ->
                "$value inches = ${df.format(value * 2.54)} cm"

            // Temperature
            (from in listOf("celsius", "c", "centigrade")) && (to in listOf("fahrenheit", "f")) ->
                "$value°C = ${df.format((value * 9.0 / 5.0) + 32.0)}°F"
            (from in listOf("fahrenheit", "f")) && (to in listOf("celsius", "c", "centigrade")) ->
                "$value°F = ${df.format((value - 32.0) * 5.0 / 9.0)}°C"

            // Weight
            (from in listOf("kg", "kilogram", "kilograms")) && (to in listOf("lbs", "pounds", "pound")) ->
                "$value kg = ${df.format(value * 2.20462)} lbs"
            (from in listOf("lbs", "pounds", "pound")) && (to in listOf("kg", "kilogram", "kilograms")) ->
                "$value lbs = ${df.format(value * 0.453592)} kg"
            (from in listOf("g", "gram", "grams")) && (to in listOf("ounces", "oz", "ounce")) ->
                "$value g = ${df.format(value * 0.035274)} oz"
            (from in listOf("ounces", "oz", "ounce")) && (to in listOf("g", "gram", "grams")) ->
                "$value oz = ${df.format(value * 28.3495)} g"

            else -> null
        }
    }
}
