import io.github.oxidefrp.semantics.Signal
import io.github.oxidefrp.semantics.Time
import kotlin.math.PI
import kotlin.math.sin

private const val exampleId = 1

fun main() {
    val inputSignal = object : Signal<Double>() {
        override fun at(t: Time): Double = sin(t.t) + 1.5
    }

    val output = transform(
        signal = inputSignal,
    )

    val outputSignal = output.signal

    buildSignalPlot(
        signal = outputSignal,
        props = PlotProps(
            canvas = CanvasProps(
                width = 400.0,
                height = 300.0,
            ),
            window = FunctionWindow(
                timeMax = 2 * PI,
                codomainMax = 3.0,
            ),
        ),
    ).writeToFile(
        exampleId = exampleId,
        plotName = "outputSignal",
    )
}
