import io.github.oxidefrp.semantics.Signal
import io.github.oxidefrp.semantics.Time
import kotlin.math.cos

fun main() {
    val inputSignal = object : Signal<Double>() {
        override fun at(t: Time): Double = cos(t.t)
    }

    val output = transform(
        signal = inputSignal,
    )

    val outputSignal = output.signal

    println(outputSignal.at(Time(t = 0.0)))
}
