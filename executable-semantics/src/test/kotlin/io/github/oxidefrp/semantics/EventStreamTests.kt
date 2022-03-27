package io.github.oxidefrp.semantics

import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class EventStreamTests {
    @Test
    fun testNever() {
        val stream = EventStream.never<Int>()

        assertEquals(
            expected = emptyList(),
            actual = stream.occurrences.toList(),
        )
    }

    @Test
    fun testMapFinite() {
        val source = EventStream(
            occurrences = sequenceOf(
                EventOccurrence(
                    time = Time(t = 1.2),
                    event = 10,
                ),
                EventOccurrence(
                    time = Time(t = 3.1),
                    event = 20,
                ),
                EventOccurrence(
                    time = Time(t = 5.8),
                    event = 30,
                ),
            ),
        )

        val result = source.map { "0x${it.toString(radix = 16).uppercase()}" }

        assertEquals(
            expected = listOf(
                EventOccurrence(
                    time = Time(t = 1.2),
                    event = "0xA",
                ),
                EventOccurrence(
                    time = Time(t = 3.1),
                    event = "0x14",
                ),
                EventOccurrence(
                    time = Time(t = 5.8),
                    event = "0x1E",
                ),
            ),
            actual = result.occurrences.toList(),
        )
    }

    @Test
    fun testMapInfinite() {
        val source = generateIntegerEventStream()

        val result = source.map {
            when {
                it % 3 == 0 && it % 5 == 0 -> "Fizz Buzz"
                it % 3 == 0 -> "Fizz"
                it % 5 == 0 -> "Buzz"
                else -> it.toString()
            }
        }

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 1.0), event = "1"),
                EventOccurrence(Time(t = 2.0), event = "2"),
                EventOccurrence(Time(t = 3.0), event = "Fizz"),
                EventOccurrence(Time(t = 4.0), event = "4"),
                EventOccurrence(Time(t = 5.0), event = "Buzz"),
                EventOccurrence(Time(t = 6.0), event = "Fizz"),
                EventOccurrence(Time(t = 7.0), event = "7"),
                EventOccurrence(Time(t = 8.0), event = "8"),
                EventOccurrence(Time(t = 9.0), event = "Fizz"),
                EventOccurrence(Time(t = 10.0), event = "Buzz"),
                EventOccurrence(Time(t = 11.0), event = "11"),
                EventOccurrence(Time(t = 12.0), event = "Fizz"),
                EventOccurrence(Time(t = 13.0), event = "13"),
                EventOccurrence(Time(t = 14.0), event = "14"),
                EventOccurrence(Time(t = 15.0), event = "Fizz Buzz"),
                EventOccurrence(Time(t = 16.0), event = "16"),
                EventOccurrence(Time(t = 17.0), event = "17"),
                EventOccurrence(Time(t = 18.0), event = "Fizz"),
                EventOccurrence(Time(t = 19.0), event = "19"),
                EventOccurrence(Time(t = 20.0), event = "Buzz"),
            ),
            actual = result.occurrences.take(20).toList(),
        )
    }

    @Test
    fun testFilter() {
        val source = generateIntegerEventStream()

        val result = source.filter { it % 2 == 0 }

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 2.0), event = 2),
                EventOccurrence(Time(t = 4.0), event = 4),
                EventOccurrence(Time(t = 6.0), event = 6),
                EventOccurrence(Time(t = 8.0), event = 8),
                EventOccurrence(Time(t = 10.0), event = 10),
            ),
            actual = result.occurrences.take(5).toList(),
        )
    }

    @Test
    fun testMergeWithNonInstantaneous() {
        val source1 = EventStream(
            sequenceOf(
                EventOccurrence(Time(t = 1.0), event = "a"),
                EventOccurrence(Time(t = 5.0), event = "b"),
                EventOccurrence(Time(t = 10.0), event = "c"),
            ),
        )

        val source2 = EventStream(
            sequenceOf(
                EventOccurrence(Time(t = 2.0), event = "d"),
                EventOccurrence(Time(t = 7.0), event = "e"),
                EventOccurrence(Time(t = 11.0), event = "f"),
            ),
        )

        val result = source1.mergeWith(source2) { a, b -> a + b }

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 1.0), event = "a"),
                EventOccurrence(Time(t = 2.0), event = "d"),
                EventOccurrence(Time(t = 5.0), event = "b"),
                EventOccurrence(Time(t = 7.0), event = "e"),
                EventOccurrence(Time(t = 10.0), event = "c"),
                EventOccurrence(Time(t = 11.0), event = "f"),
            ),
            actual = result.occurrences.toList(),
        )
    }

    @Test
    fun testMergeWithInstantaneous() {
        val source1 = EventStream(
            sequenceOf(
                EventOccurrence(time = Time(t = 1.0), event = "a"),
                EventOccurrence(time = Time(t = 5.0), event = "b"),
                EventOccurrence(time = Time(t = 11.0), event = "c"),
                EventOccurrence(time = Time(t = 16.0), event = "d"),
            ) + generateOccurrencesSequence(
                t0 = 17.0,
                seed = "x",
                nextFunction = { "x" },
            ),
        )

        val source2 = EventStream(
            sequenceOf(
                EventOccurrence(Time(t = 2.0), event = "d"),
                EventOccurrence(Time(t = 5.0), event = "e"),
                EventOccurrence(Time(t = 10.0), event = "f"),
                EventOccurrence(Time(t = 16.0), event = "g"),
            ) + generateOccurrencesSequence(
                t0 = 17.5,
                seed = "y",
                nextFunction = { "y" },
            ),
        )

        val result = source1.mergeWith(source2) { a, b -> a + b }

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 1.0), event = "a"),
                EventOccurrence(Time(t = 2.0), event = "d"),
                EventOccurrence(Time(t = 5.0), event = "be"),
                EventOccurrence(Time(t = 10.0), event = "f"),
                EventOccurrence(Time(t = 11.0), event = "c"),
                EventOccurrence(Time(t = 16.0), event = "dg"),
            ),
            actual = result.occurrences.take(6).toList(),
        )
    }

    @Test
    fun testMergeWithFirstFinite() {
        val source1 = EventStream(
            sequenceOf(
                EventOccurrence(time = Time(t = 2.0), event = "a"),
            ),
        )

        val source2 = EventStream(
            generateOccurrencesSequence(
                t0 = 1.5,
                seed = "x",
                nextFunction = { "x" },
            ),
        )

        val result = source1.mergeWith(source2) { a, b -> a + b }

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 1.5), event = "x"),
                EventOccurrence(Time(t = 2.0), event = "a"),
                EventOccurrence(Time(t = 2.5), event = "x"),
                EventOccurrence(Time(t = 3.5), event = "x"),
            ),
            actual = result.occurrences.take(4).toList(),
        )
    }

    @Test
    fun testMergeWithSecondFinite() {
        val source1 = EventStream(
            generateOccurrencesSequence(
                t0 = 1.5,
                seed = "y",
                nextFunction = { "y" },
            ),
        )

        val source2 = EventStream(
            sequenceOf(
                EventOccurrence(time = Time(t = 2.0), event = "a"),
                EventOccurrence(time = Time(t = 3.0), event = "b"),
            ),
        )

        val result = source1.mergeWith(source2) { a, b -> a + b }

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 1.5), event = "y"),
                EventOccurrence(Time(t = 2.0), event = "a"),
                EventOccurrence(Time(t = 2.5), event = "y"),
                EventOccurrence(Time(t = 3.0), event = "b"),
                EventOccurrence(Time(t = 3.5), event = "y"),
                EventOccurrence(Time(t = 4.5), event = "y"),
            ),
            actual = result.occurrences.take(6).toList(),
        )
    }

    @Test
    fun testProbe() {
        val streamInput = EventStream(
            sequenceOf(
                EventOccurrence(Time(t = 1.0), event = "a"),
                EventOccurrence(Time(t = 2.0), event = "b"),
                EventOccurrence(Time(t = 3.0), event = "c"),
                EventOccurrence(Time(t = 7.0), event = "d"),
            ) + generateOccurrencesSequence(
                t0 = 8.0,
                seed = "x",
                nextFunction = { "x" },
            ),
        )

        val signalInput = object : Signal<Int>() {
            override fun at(t: Time): Int = (t.t * t.t).roundToInt()
        }

        val result = streamInput.probe(
            signalInput,
            combine = { s, n -> "$s$n" }
        )

        assertEquals(
            expected = listOf(
                EventOccurrence(Time(t = 1.0), event = "a1"),
                EventOccurrence(Time(t = 2.0), event = "b4"),
                EventOccurrence(Time(t = 3.0), event = "c9"),
                EventOccurrence(Time(t = 7.0), event = "d49"),
            ),
            actual = result.occurrences.take(4).toList(),
        )
    }

    @Test
    fun testSample() {
        val signalInput1 = object : Signal<Double>() {
            override fun at(t: Time): Double = t.t * t.t
        }

        val signalInput2 = object : Signal<Double>() {
            override fun at(t: Time): Double = t.t / 4.0
        }

        val signalInput3 = object : Signal<Double>() {
            override fun at(t: Time): Double = sin(t.t)
        }

        val streamInput = EventStream(
            sequenceOf(
                EventOccurrence(Time(t = 3.0), event = signalInput1),
                EventOccurrence(Time(t = 5.0), event = signalInput2),
                EventOccurrence(Time(t = 10.0), event = signalInput3),
            ) + generateOccurrencesSequence(
                t0 = 11.0,
                seed = signalInput1,
                nextFunction = { it },
            ),
        )

        val result = EventStream.sample(streamInput)

        val actualOccurrences = result.occurrences.take(3).toList()

        assertEquals(
            expected = 3.0,
            actual = actualOccurrences[0].time.t,
        )

        assertEquals(
            expected = 9.0,
            actual = actualOccurrences[0].event,
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = 5.0,
            actual = actualOccurrences[1].time.t,
        )

        assertEquals(
            expected = 1.25,
            actual = actualOccurrences[1].event,
            absoluteTolerance = epsilon,
        )

        assertEquals(
            expected = 10.0,
            actual = actualOccurrences[2].time.t,
        )

        assertEquals(
            expected = -0.5440211109,
            actual = actualOccurrences[2].event,
            absoluteTolerance = epsilon,
        )
    }

    @Test
    @Ignore
    fun testHold(): Unit = TODO()
}

// Build event stream in form [(1.0, 1), (2.0, 2), (3.0, 3), ...]
private fun generateIntegerEventStream(
    seed: Int = 1,
    step: Int = 1,
) = generateEventStream(
    seed = seed,
    nextFunction = { it + step },
)

private fun <T> generateEventStream(
    t0: Double = 1.0,
    seed: T,
    nextFunction: (T) -> T,
) = EventStream(
    occurrences = generateOccurrencesSequence(
        t0 = t0,
        seed = seed,
        nextFunction = nextFunction,
    ),
)

private fun <T> generateOccurrencesSequence(
    t0: Double = 1.0,
    seed: T,
    nextFunction: (T) -> T,
): Sequence<EventOccurrence<T>> = generateSequence(
    seed = EventOccurrence(
        time = Time(t = t0),
        event = seed,
    ),
    nextFunction = {
        EventOccurrence(
            time = Time(t = it.time.t + 1.0),
            event = nextFunction(it.event),
        )
    }
)
