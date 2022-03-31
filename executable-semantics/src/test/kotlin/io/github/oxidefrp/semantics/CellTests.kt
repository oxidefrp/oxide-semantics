package io.github.oxidefrp.semantics

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class CellOperatorsUnitTests {
    @Test
    fun testConstant() {
        val cell = Cell.constant(8)

        assertEquals(
            expected = 8,
            actual = cell.initialValue,
        )

        assertEquals(
            expected = 0,
            actual = cell.newValues.occurrences.count(),
        )
    }

    @Test
    fun testMap() {
        val cellInput = Cell(
            initialValue = 3,
            newValues = EventStream(
                occurrences = sequenceOf(
                    EventOccurrence(time = Time(t = 1.0), event = 4),
                    EventOccurrence(time = Time(t = 2.0), event = 5),
                    EventOccurrence(time = Time(t = 3.0), event = 6),
                ),
            ),
        )

        val result = cellInput.map { "#$it" }

        assertEquals(
            expected = "#3",
            actual = result.initialValue,
        )

        assertEquals(
            expected = listOf(
                EventOccurrence(time = Time(t = 1.0), event = "#4"),
                EventOccurrence(time = Time(t = 2.0), event = "#5"),
                EventOccurrence(time = Time(t = 3.0), event = "#6"),
            ),
            actual = result.newValues.occurrences.toList(),
        )
    }

    @Test
    fun testApply() {
        val functionCell: Cell<(Int) -> String> = Cell(
            initialValue = fun(n: Int) = "&$n",
            newValues = EventStream(
                occurrences = sequenceOf(
                    EventOccurrence(
                        time = Time(t = 2.0),
                        event = fun(n: Int) = "%$n",
                    ),
                    EventOccurrence(
                        time = Time(t = 4.0),
                        event = fun(n: Int) = "^$n",
                    ),
                ),
            ),
        )

        val argumentCell = Cell(
            initialValue = 10,
            newValues = EventStream(
                occurrences = sequenceOf(
                    EventOccurrence(
                        time = Time(t = 1.0),
                        event = 20,
                    ),
                    EventOccurrence(
                        time = Time(t = 2.0),
                        event = 30,
                    ),
                    EventOccurrence(
                        time = Time(t = 3.0),
                        event = 40,
                    ),
                ),
            ),
        )

        val result = Cell.apply(
            function = functionCell,
            argument = argumentCell,
        )

        assertEquals(
            expected = "&10",
            actual = result.initialValue,
        )

        assertEquals(
            expected = listOf(
                EventOccurrence(time = Time(t = 1.0), event = "&20"),
                EventOccurrence(time = Time(t = 2.0), event = "%30"),
                EventOccurrence(time = Time(t = 3.0), event = "%40"),
                EventOccurrence(time = Time(t = 4.0), event = "^40"),
            ),
            actual = result.newValues.occurrences.toList(),
        )
    }

    @Test
    fun testChanges() {
        val inputCell = Cell(
            initialValue = 10,
            newValues = EventStream(
                occurrences = sequenceOf(
                    EventOccurrence(
                        time = Time(t = 1.0),
                        event = 20,
                    ),
                    EventOccurrence(
                        time = Time(t = 2.0),
                        event = 30,
                    ),
                    EventOccurrence(
                        time = Time(t = 3.0),
                        event = 40,
                    ),
                ),
            ),
        )

        assertEquals(
            expected = listOf(
                EventOccurrence(
                    time = Time(t = 1.0),
                    event = ValueChange(
                        oldValue = 10,
                        newValue = 20,
                    ),
                ),
                EventOccurrence(
                    time = Time(t = 2.0),
                    event = ValueChange(
                        oldValue = 20,
                        newValue = 30,
                    ),
                ),
                EventOccurrence(
                    time = Time(t = 3.0),
                    event = ValueChange(
                        oldValue = 30,
                        newValue = 40,
                    ),
                ),
            ),
            actual = inputCell.changes.occurrences.toList(),
        )
    }

    @Test
    fun testValue() {
        val inputCell = Cell(
            initialValue = 5,
            newValues = EventStream(
                occurrences = sequenceOf(
                    EventOccurrence(
                        time = Time(t = 1.0),
                        event = 15,
                    ),
                    EventOccurrence(
                        time = Time(t = 2.0),
                        event = 25,
                    ),
                    EventOccurrence(
                        time = Time(t = 3.0),
                        event = 35,
                    ),
                ),
            ),
        )

        assertEquals(
            expected = 5,
            actual = inputCell.value.at(Time(t = 0.0)),
        )

        assertEquals(
            expected = 5,
            actual = inputCell.value.at(Time(t = 1.0)),
        )

        assertEquals(
            expected = 15,
            actual = inputCell.value.at(Time(t = 1.1)),
        )

        assertEquals(
            expected = 15,
            actual = inputCell.value.at(Time(t = 1.9)),
        )

        assertEquals(
            expected = 15,
            actual = inputCell.value.at(Time(t = 2.0)),
        )

        assertEquals(
            expected = 25,
            actual = inputCell.value.at(Time(t = 2.1)),
        )

        assertEquals(
            expected = 25,
            actual = inputCell.value.at(Time(t = 2.9)),
        )

        assertEquals(
            expected = 25,
            actual = inputCell.value.at(Time(t = 3.0)),
        )

        assertEquals(
            expected = 35,
            actual = inputCell.value.at(Time(t = 3.1)),
        )
    }

    @Test
    @Ignore
    fun testSwitch() {
        TODO()
    }

    @Test
    @Ignore
    fun testDivert() {
        TODO()
    }
}
