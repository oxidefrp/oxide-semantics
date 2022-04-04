import io.github.oxidefrp.semantics.Signal
import io.github.oxidefrp.semantics.Time
import kotlin.math.sin

fun main() {
    val signalInput = object : Signal<Double>() {
        override fun at(t: Time): Double = sin(t.t)
    }

    println(signalInput.at(Time(t = 0.0)))
}
