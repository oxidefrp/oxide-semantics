import io.github.oxidefrp.semantics.Signal
import io.github.oxidefrp.semantics.Time
import kotlin.math.floor

private const val horizontalCanvasPadding = 40.0
private const val verticalCanvasPadding = 40.0
private const val polylinePointCount = 1000

data class CanvasProps(
    val width: Double,
    val height: Double,
)

data class FunctionWindow(
    val timeMax: Double,
    val codomainMax: Double,
) {
    fun getTRelative(s: Double): Double = s * timeMax
}

data class PlotProps(
    val canvas: CanvasProps,
    val window: FunctionWindow,
) {
    // Map the "t" value from the Time domain to a relative SVG X-coord
    fun mapT(t: Double): Double = (t / window.timeMax) * canvas.width

    // Map the "a" value from the A codomain to a relative SVG Y-coord
    // (with up-pointing Y axis)
    fun mapA(a: Double): Double = (a / window.codomainMax) * canvas.height
}

fun buildSignalPlot(
    signal: Signal<Double>,
    props: PlotProps,
): SvgSvg = SvgSvg(
    width = horizontalCanvasPadding + props.canvas.width + horizontalCanvasPadding,
    height = verticalCanvasPadding + props.canvas.height + verticalCanvasPadding,
    children = listOf(
        SvgGroup(
            transform = SvgCombinedTransform(
                transforms = listOf(
                    SvgTranslate(
                        tx = horizontalCanvasPadding,
                        ty = verticalCanvasPadding + props.canvas.height,
                    ),
                ),
            ),
            children = buildCanvasContent(
                props = props,
                signal = signal,
            ),
        ),
    ),
)

private fun buildCanvasContent(
    props: PlotProps,
    signal: Signal<Double>,
): List<SvgElement> {
    val horizontalLabelOffset = 10.0
    val verticalLabelOffset = 14.0

    val tAxis = buildAxis(
        pMax = props.window.timeMax,
        mapP = props::mapT,
        buildPoint = { lx, ly -> Point(lx, ly) },
        labelLy = +verticalLabelOffset,
    )

    val aAxis = buildAxis(
        pMax = props.window.codomainMax,
        mapP = props::mapA,
        buildPoint = { lx, ly -> Point(ly, -lx) },
        labelLy = -horizontalLabelOffset,
    )

    return listOf(
        SvgPolyline(
            points = (0..polylinePointCount).map { i ->
                val t = props.window.getTRelative(i.toDouble() / polylinePointCount)
                val a = signal.at(Time(t = t))

                Point(props.mapT(t), -props.mapA(a))
            },
            stroke = "red",
            strokeWidth = 2.0,
        ),
        SvgGroup(
            children = listOf(tAxis, aAxis) + buildLabel(
                a = Point(-horizontalLabelOffset, verticalLabelOffset),
                text = "0",
            ),
        ),
    )
}

private fun buildAxis(
    pMax: Double,
    // Map "p" value to logical X-coord
    mapP: (p: Double) -> Double,
    // Build a point from logical coord
    buildPoint: (lx: Double, ly: Double) -> Point,
    labelLy: Double,
): SvgElement {
    val triangleLength = 16.0
    val triangleWidth = 10.0
    val barWidth = 6.0
    val axisPadding = 8.0

    val pBarRange = (1..floor(pMax).toInt())

    val lxShaftStart = -axisPadding
    val lxShaftEnd = mapP(pMax) + axisPadding

    val shaftLine = SvgLine(
        a = buildPoint(lxShaftStart, 0.0),
        b = buildPoint(lxShaftEnd, 0.0),
    )

    val trianglePolyline = SvgPolyline(
        points = listOf(
            buildPoint(lxShaftEnd, -triangleWidth / 2.0),
            buildPoint(lxShaftEnd, +triangleWidth / 2.0),
            buildPoint(lxShaftEnd + triangleLength, 0.0),
        ),
        fill = "black",
        stroke = "none",
    )

    val bars = pBarRange.map { p ->
        val lx = mapP(p.toDouble())

        SvgLine(
            buildPoint(lx, -barWidth / 2),
            buildPoint(lx, +barWidth / 2),
        )
    }

    val labels = pBarRange.map { p ->
        val lx = mapP(p.toDouble())

        buildLabel(
            a = buildPoint(lx, labelLy),
            text = p.toString(),
        )
    }

    return SvgGroup(
        children = listOf(
            shaftLine,
            trianglePolyline,
        ) + bars + labels,
    )
}

private fun buildLabel(
    a: Point,
    text: String,
): SvgText = SvgText(
    p = a,
    text = text,
)
