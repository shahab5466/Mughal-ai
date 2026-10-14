package com.example

import com.example.calculator.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    private val engine = CalculatorEngine()

    @Test
    fun addition_isCorrect() {
        assertEquals("4", engine.evaluate("2+2"))
    }

    @Test
    fun multiplication_isCorrect() {
        assertEquals("50", engine.evaluate("10×5"))
    }

    @Test
    fun division_isCorrect() {
        assertEquals("5", engine.evaluate("25÷5"))
    }

    @Test
    fun percentage_isCorrect() {
        assertEquals("0.5", engine.evaluate("50%"))
    }

    @Test
    fun complexExpression_isCorrect() {
        assertEquals("26", engine.evaluate("2+4×6"))
    }
}
