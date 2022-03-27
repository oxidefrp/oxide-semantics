package io.github.oxidefrp.semantics

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

const val epsilon = 0.000001

class SignalOperatorsUnitTests {
    @Test
    fun testConstant() {
        val signal = Signal.constant(8)

        assertEquals(
            expected = 8,
            actual = signal.at(Time(t = -10.0)),
        )

        assertEquals(
            expected = 8,
            actual = signal.at(Time(t = 0.0)),
        )

        assertEquals(
            expected = 8,
            actual = signal.at(Time(t = 6.2)),
        )
    }

    @Test
    fun testMap() {
        val signalInput = object : Signal<Double>() {
            override fun at(t: Time): Double = sin(t.t)
        }

        val result = signalInput.map { it * 2.0 }

        assertEquals(
            expected = 0.0,
            actual = result.at(Time(t = 0.0)),
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = 1.41421356237,
            actual = result.at(Time(t = PI / 4.0)),
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = 2.0,
            actual = result.at(Time(t = PI / 2.0)),
            absoluteTolerance = epsilon,
        )
    }

    @Test
    fun testApply() {
        val functionSignal = object : Signal<(Double) -> Double>() {
            override fun at(t: Time) = fun(x: Double) = cos(x) * t.t
        }

        val argumentSignal = object : Signal<Double>() {
            override fun at(t: Time) = t.t / 2.0
        }

        val result = Signal.apply(
            function = functionSignal,
            argument = argumentSignal,
        )

        assertEquals(
            expected = -2.8366218546,
            actual = result.at(Time(t = -10.0)),
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = 0.0,
            actual = result.at(Time(t = 0.0)),
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = -6.1946379317,
            actual = result.at(Time(t = 6.2)),
            absoluteTolerance = epsilon,
        )
    }

    @Test
    fun testSample() {
        val innerSignal1 = object : Signal<Double>() {
            override fun at(t: Time) = -sin(t.t)
        }

        val innerSignal2 = object : Signal<Double>() {
            override fun at(t: Time) = cos(t.t) + 3.0
        }

        val outerSignal = object : Signal<Signal<Double>>() {
            override fun at(t: Time) = if (t.t.toInt() % 2 == 0) innerSignal1
            else innerSignal2
        }

        val result = Signal.sample(
            signal = outerSignal,
        )

        assertEquals(
            expected = 3.2674988286,
            actual = result.at(Time(t = 1.3)),
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = -0.5155013718,
            actual = result.at(Time(t = 2.6)),
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = 3.6845466664,
            actual = result.at(Time(t = 7.1)),
            absoluteTolerance = epsilon,
        )
    }
}
