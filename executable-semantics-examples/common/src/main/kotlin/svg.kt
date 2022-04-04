import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class Point(
    val x: Double,
    val y: Double,
) {
    fun toSvgString() = "$x,$y"
}

abstract class SvgTransform {
    abstract fun buildTransformString(): String
}

abstract class SvgTransformFunction : SvgTransform() {
    final override fun buildTransformString(): String =
        buildTransformFunctionString()

    abstract fun buildTransformFunctionString(): String
}

data class SvgCombinedTransform(
    val transforms: List<SvgTransformFunction>,
) : SvgTransform() {
    override fun buildTransformString(): String =
        transforms.joinToString(separator = " ") {
            it.buildTransformFunctionString()
        }
}

data class SvgTranslate(
    val tx: Double,
    val ty: Double,
) : SvgTransformFunction() {
    override fun buildTransformFunctionString(): String = "translate($tx, $ty)"
}

data class SvgScale(
    val sx: Double = 1.0,
    val sy: Double = 1.0,
) : SvgTransformFunction() {
    override fun buildTransformFunctionString(): String = "scale($sx, $sy)"
}

abstract class SvgElement {
    abstract fun buildElement(document: Document): Element
}

data class SvgSvg(
    val width: Double,
    val height: Double,
    val children: List<SvgElement>,
) {
    private fun buildRootElement(document: Document): Element =
        document.createElement("svg").apply {
            setAttribute("xmlns", "http://www.w3.org/2000/svg")
            setAttribute("width", width.toString())
            setAttribute("height", height.toString())

            children.forEach {
                appendChild(it.buildElement(document = document))
            }
        }

    fun writeToFile(
        exampleId: Int,
        plotName: String,
    ) {
        val documentFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentFactory.newDocumentBuilder()
        val document = documentBuilder.newDocument()

        document.appendChild(buildRootElement(document))

        val transformerFactory = TransformerFactory.newInstance()

        val transformer = transformerFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }

        val source = DOMSource(document)
        val exampleIdStr = exampleId.toString().padStart(2, '0')
        val path = "example$exampleIdStr-$plotName.svg"
        val result = StreamResult(File(path))

        transformer.transform(source, result)
    }
}

data class SvgGroup(
    val transform: SvgTransform? = null,
    val children: List<SvgElement>,
) : SvgElement() {
    override fun buildElement(document: Document): Element =
        document.createElement("g").apply {
            transform?.let {
                setAttribute("transform", it.buildTransformString())
            }

            children.forEach {
                appendChild(it.buildElement(document = document))
            }
        }
}

data class SvgLine(
    val a: Point,
    val b: Point,
    val fill: String = "none",
    val stroke: String = "black",
) : SvgElement() {
    override fun buildElement(document: Document): Element =
        document.createElement("line").apply {
            setAttribute("x1", a.x.toString())
            setAttribute("y1", a.y.toString())
            setAttribute("x2", b.x.toString())
            setAttribute("y2", b.y.toString())

            setAttribute("fill", fill)
            setAttribute("stroke", stroke)
        }
}

data class SvgPolyline(
    val points: List<Point>,
    val fill: String = "none",
    val stroke: String = "black",
    val strokeWidth: Double? = null,
) : SvgElement() {
    override fun buildElement(document: Document): Element =
        document.createElement("polyline").apply {
            setAttribute(
                "points",
                points.joinToString(separator = " ") { it.toSvgString() },
            )

            setAttribute("fill", fill)
            setAttribute("stroke", stroke)

            strokeWidth?.let {
                setAttribute("stroke-width", it.toString())
            }
        }
}

data class SvgText(
    val p: Point,
    val text: String,
) : SvgElement() {
    override fun buildElement(document: Document): Element =
        document.createElement("text").apply {
            setAttribute("x", p.x.toString())
            setAttribute("y", p.y.toString())
            setAttribute("text-anchor", "middle")
            setAttribute("dominant-baseline", "middle")

            setAttribute("fill", "black")
            setAttribute("stroke", "none")

            appendChild(document.createTextNode(text))
        }
}
