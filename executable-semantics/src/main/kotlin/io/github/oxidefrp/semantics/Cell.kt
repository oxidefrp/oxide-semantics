package io.github.oxidefrp.semantics

data class ValueChange<out A>(
    val oldValue: A,
    val newValue: A,
)

data class Cell<out A>(
    val initialValue: A,
    val newValues: EventStream<A>,
) {
    companion object {
        fun <A> constant(value: A): Cell<A> =
            Cell(
                initialValue = value,
                newValues = EventStream.never(),
            )

        fun <A> switch(cell: Cell<Cell<A>>): Cell<A> =
            TODO()

        fun <A> divert(cell: Cell<EventStream<A>>): EventStream<A> =
            TODO()

        fun <A, B> apply(
            function: Cell<(A) -> B>,
            argument: Cell<A>,
        ): Cell<B> = Cell(
            initialValue = function.initialValue(argument.initialValue),
            newValues = EventStream(
                occurrences = mergeOccurrences(
                    occurrencesA = function.newValues.occurrences,
                    occurrencesB = argument.newValues.occurrences,
                    transformA = { fnOcc ->
                        val fn = fnOcc.event
                        val arg = argument.value.at(fnOcc.time)
                        fn(arg)
                    },
                    transformB = { argOcc ->
                        val fn = function.value.at(argOcc.time)
                        val arg = argOcc.event
                        fn(arg)
                    },
                    combine = { fnOcc, argOcc ->
                        val fn = fnOcc.event
                        val arg = argOcc.event
                        fn(arg)
                    },
                ),
            ),
        )

        fun <A, B> map1(
            ca: Cell<A>,
            f: (a: A) -> B,
        ): Cell<B> {
            fun g(a: A) = f(a)

            return ca.map(::g)
        }

        fun <A, B, C> map2(
            ca: Cell<A>,
            cb: Cell<B>,
            f: (a: A, b: B) -> C,
        ): Cell<C> {
            fun g(a: A) = fun(b: B) = f(a, b)

            return apply(
                ca.map(::g),
                cb,
            )
        }

        fun <A, B, C, D> map3(
            ca: Cell<A>,
            cb: Cell<B>,
            cc: Cell<C>,
            f: (a: A, b: B, c: C) -> D,
        ): Cell<D> {
            fun g(a: A) = fun(b: B) = fun(c: C) = f(a, b, c)

            return apply(
                apply(
                    ca.map(::g),
                    cb,
                ),
                cc,
            )
        }
    }

    val changes: EventStream<ValueChange<A>>
        get() {
            fun generateChangeOccurrences(
                oldValue: A,
                newValuesOccurrences: Sequence<EventOccurrence<A>>,
            ): Sequence<EventOccurrence<ValueChange<A>>> {
                val (newValueOccurrence, newValuesTail) =
                    newValuesOccurrences.cutOff() ?: return emptySequence()

                val newValue = newValueOccurrence.event

                return sequenceCons(
                    head = newValueOccurrence.map {
                        ValueChange(
                            oldValue = oldValue,
                            newValue = newValue,
                        )
                    },
                    tail = {
                        generateChangeOccurrences(
                            oldValue = newValue,
                            newValuesOccurrences = newValuesTail,
                        )
                    },
                )
            }

            return EventStream(
                occurrences = generateChangeOccurrences(
                    oldValue = initialValue,
                    newValuesOccurrences = newValues.occurrences,
                ),
            )
        }

    val value: Signal<A> = object : Signal<A>() {
        override fun at(t: Time): A {
            fun findValue(
                oldValue: A,
                newValuesOccurrences: Sequence<EventOccurrence<A>>,
            ): A {
                val (head, tail) = newValuesOccurrences.cutOff() ?: return oldValue

                if (head.time >= t) return oldValue

                return findValue(
                    oldValue = head.event,
                    newValuesOccurrences = tail,
                )
            }

            return findValue(
                oldValue = initialValue,
                newValuesOccurrences = newValues.occurrences,
            )
        }
    }

    fun <B> map(transform: (A) -> B): Cell<B> =
        Cell(
            initialValue = transform(initialValue),
            newValues = newValues.map(transform),
        )

    fun <B> switchOf(transform: (A) -> Cell<B>): Cell<B> =
        switch(map(transform))
}
