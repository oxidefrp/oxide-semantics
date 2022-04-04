import io.github.oxidefrp.semantics.Signal

data class Output(
    val signal: Signal<Double>,
)

fun transform(
    signal: Signal<Double>,
): Output =
    Output(
        signal = signal,
    )
