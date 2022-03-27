package io.github.oxidefrp.semantics

import java.lang.IllegalArgumentException

data class EventOccurrence<out A>(
    val time: Time,
    val event: A,
) {
    fun <B> map(transform: (A) -> B): EventOccurrence<B> =
        EventOccurrence(
            time = time,
            event = transform(event),
        )
}

class EventStream<out A>(
    occurrences: Sequence<EventOccurrence<A>>,
) {
    companion object {
        fun <A> never(): EventStream<A> =
            EventStream(occurrences = emptySequence())

        fun <A> sample(stream: EventStream<Signal<A>>): EventStream<A> =
            EventStream(
                occurrences = stream.occurrences.map {
                    val time = it.time
                    val signal = it.event

                    EventOccurrence(
                        time = time,
                        event = signal.at(time),
                    )
                },
            )
    }

    val occurrences = regenerateOccurrencesWithMonotonicityChecks(occurrences)

    fun <B> map(transform: (A) -> B): EventStream<B> =
        EventStream(
            occurrences = occurrences.map {
                it.map(transform)
            },
        )

    fun filter(predicate: (A) -> Boolean): EventStream<A> =
        EventStream(
            occurrences = occurrences.filter {
                predicate(it.event)
            },
        )

    fun <B, C> probe(
        signal: Signal<B>,
        combine: (A, B) -> C,
    ): EventStream<C> =
        EventStream(
            occurrences = occurrences.map {
                EventOccurrence(
                    time = it.time,
                    event = combine(
                        it.event,
                        signal.at(it.time),
                    ),
                )
            },
        )

    fun <B> prick(signal: Signal<B>): EventStream<B> =
        probe(signal) { _, b -> b }

    fun <B> sampleOf(selector: (A) -> Signal<B>): EventStream<B> =
        sample(map(selector))

    fun subscribe(listener: (A) -> Unit): Unit =
        TODO()
}

fun <A> EventStream<A>.mergeWith(
    other: EventStream<A>,
    combine: (A, A) -> A,
): EventStream<A> {
    fun generateResultOccurrences(
        occurrences1: Sequence<EventOccurrence<A>>,
        occurrences2: Sequence<EventOccurrence<A>>,
    ): Sequence<EventOccurrence<A>> {
        val (head1, tail1) = occurrences1.cutOff() ?: return occurrences2
        val (head2, tail2) = occurrences2.cutOff() ?: return occurrences1

        return if (head1.time == head2.time) {
            val combinedOccurrence = EventOccurrence(
                head1.time,
                event = combine(
                    head1.event,
                    head2.event,
                ),
            )

            sequenceCons(combinedOccurrence) {
                generateResultOccurrences(
                    tail1,
                    tail2,
                )
            }
        } else if (head1.time < head2.time) {
            sequenceCons(head1) {
                generateResultOccurrences(
                    tail1,
                    occurrences2,
                )
            }
        } else {
            assert(head1.time > head2.time)

            sequenceCons(head2) {
                generateResultOccurrences(
                    occurrences1,
                    tail2,
                )
            }
        }
    }

    return EventStream(
        occurrences = generateResultOccurrences(
            occurrences,
            other.occurrences,
        ),
    )
}

//fun <A> EventStream<A>.hold(initialValue: A): Signal<Cell<A>> =
//    TODO()

// Lazily construct a sequence from head and tail
// Although we use yield-based imperative sequence builder here, this is
// semantically equivalent to the classic FP list cons-constructor.
private fun <T> sequenceCons(
    head: T,
    buildTail: () -> Sequence<T>,
): Sequence<T> =
    sequence {
        yield(head)
        yieldAll(buildTail())
    }

data class CutSequence<out A>(
    val head: A,
    val tail: Sequence<A>,
)

// Cut the head off the sequence and return a pair in the form (head, tail).
// If the corner case when the list is empty, return null instead (which
// corresponds to FP list nil).
private fun <T> Sequence<T>.cutOff(): CutSequence<T>? =
    firstOrNull()?.let {
        CutSequence(
            head = it,
            tail = drop(1),
        )
    }

private fun <A> regenerateOccurrencesWithMonotonicityChecks(
    occurrences: Sequence<EventOccurrence<A>>,
) = sequence {
    var previousTime: Time? = null

    occurrences.forEach {
        val prevTime = previousTime

        if (prevTime != null) {
            if (prevTime >= it.time) {
                throw IllegalArgumentException("Occurrences in the event stream aren't monotonic in the time axis (${prevTime.t} >= ${it.time.t})")
            }
        }

        yield(it)

        // This locally stateful computation doesn't affect the results and
        // its functional pureness.
        previousTime = it.time
    }
}