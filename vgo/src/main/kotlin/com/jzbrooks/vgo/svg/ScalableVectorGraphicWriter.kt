package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.svg.graphic.ClipPath
import org.w3c.dom.Document
import java.io.OutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Collections.emptySet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ScalableVectorGraphicWriter(
    override val options: Set<Writer.Option> = emptySet(),
) : Writer<ScalableVectorGraphic> {

    private val commandPrinter = ScalableVectorGraphicCommandPrinter(3)

    private val formatter = DecimalFormat().apply {
        maximumFractionDigits = 2 // todo: parameterize?
        isDecimalSeparatorAlwaysShown = false
        roundingMode = RoundingMode.HALF_UP
    }

    override fun write(graphic: ScalableVectorGraphic, stream: OutputStream) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("svg")
        val elementName = graphic.id
        if (elementName != null) {
            root.setAttribute("id", elementName)
        }
        for (item in graphic.foreign) {
            root.setAttribute(item.key, item.value)
        }
        document.appendChild(root)

        for (element in graphic.elements) {
            write(root, element, document)
        }

        write(document, stream)
    }

    private fun write(parent: org.w3c.dom.Element, element: Element, document: Document) {
        val node = when (element) {
            is Path -> {
                document.createElement("path").apply {
                    val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                    setAttribute("d", data)

                    if (element.fill != Colors.BLACK) {
                        val color = NAMED_COLORS[element.fill] ?: element.fill.toHexString(Color.HexFormat.RGBA)
                        setAttribute("fill", color)
                    }
                }
            }
            is Group -> {
                document.createElement("g").also { node ->
                    if (element.transform !== Matrix3.IDENTITY) {
                        val matrix = element.transform
                        val matrixElements = listOf(
                            formatter.format(matrix[0, 0]),
                            formatter.format(matrix[1, 0]),
                            formatter.format(matrix[0, 1]),
                            formatter.format(matrix[1, 1]),
                            formatter.format(matrix[0, 2]),
                            formatter.format(matrix[1, 2]),
                        ).joinToString(separator = ",")

                        node.setAttribute("transform", "matrix($matrixElements)")
                    }

                    for (child in element.elements) {
                        write(node, child, document)
                    }
                }
            }
            is ClipPath -> {
                document.createElement("clipPath").also {
                    for (child in element.elements) {
                        write(it, child, document)
                    }
                }
            }
            is Extra -> {
                document.createElement(element.name).also {
                    for (child in element.elements) {
                        write(it, child, document)
                    }
                }
            }
            else -> null
        }

        if (node != null) {
            val elementName = element.id
            if (elementName != null) {
                node.setAttribute("id", elementName)
            }
            for (item in element.foreign) {
                node.setAttribute(item.key, item.value)
            }
            parent.appendChild(node)
        }
    }

    private fun write(document: Document, outputStream: OutputStream) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        val indent = options.filterIsInstance<Writer.Option.Indent>().singleOrNull()?.columns
        if (indent != null) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.toString())
        }

        val source = DOMSource(document)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }

    private val NAMED_COLORS = mapOf(
        Color(0xFF000000u) to "black",
        Color(0xFFC0C0C0u) to "silver",
        Color(0xFF808080u) to "gray",
        Color(0xFFFFFFFFu) to "white",
        Color(0xFF800000u) to "maroon",
        Color(0xFFFF0000u) to "red",
        Color(0xFF800080u) to "purple",
        Color(0xFFFF00FFu) to "fuchsia",
        Color(0xFF008000u) to "green",
        Color(0xFF00FF00u) to "lime",
        Color(0xFF808000u) to "olive",
        Color(0xFFFFFF00u) to "yellow",
        Color(0xFF000080u) to "navy",
        Color(0xFF0000FFu) to "blue",
        Color(0xFF008080u) to "teal",
        Color(0xFF00FFFFu) to "cyan",
        Color(0xFFFFA500u) to "orange",
        Color(0xFFF0F8FFu) to "aliceblue",
        Color(0xFFFAEBD7u) to "antiquewhite",
        Color(0xFF7FFFD4u) to "aquamarine",
        Color(0xFFF0FFFFu) to "azure",
        Color(0xFFF5F5DCu) to "beige",
        Color(0xFFFFE4C4u) to "bisque",
        Color(0xFFFF3BCDu) to "blanchedalmond",
        Color(0xFF8A2BE2u) to "blueviolet",
        Color(0xFFA52A2Au) to "brown",
        Color(0xFFDEB887u) to "burlywood",
        Color(0xFF5F9EA0u) to "cadetblue",
        Color(0xFF7FFF00u) to "chartreuse",
        Color(0xFFD2691Eu) to "chocolate",
        Color(0xFFFF7F50u) to "coral",
        Color(0xFF6495EDu) to "cornflowerblue",
        Color(0xFFFFF8DCu) to "cornsilk",
        Color(0xFFDC143Cu) to "crimson",
        Color(0xFF00008Bu) to "darkblue",
        Color(0xFF008B8Bu) to "darkcyan",
        Color(0xFFB8860Bu) to "darkgoldenrod",
        Color(0xFFA9A9A9u) to "darkgray",
        Color(0xFF006400u) to "darkgreen",
        Color(0xFFBDB76Bu) to "darkkhaki",
        Color(0xFF8B008Bu) to "darkmagenta",
        Color(0xFF556B2Fu) to "darkolivegreen",
        Color(0xFFFF8C00u) to "darkorange",
        Color(0xFF9932CCu) to "darkorchid",
        Color(0xFF8B0000u) to "darkred",
        Color(0xFFE9967Au) to "darksalmon",
        Color(0xFF8FBC8Fu) to "darkseagreen",
        Color(0xFF483B8Bu) to "darkslateblue",
        Color(0xFF2F4F4Fu) to "darkslategray",
        Color(0xFF00CED1u) to "darkturquoise",
        Color(0xFF9400D3u) to "darkviolet",
        Color(0xFFFF1493u) to "deeppink",
        Color(0xFF00BFFFu) to "deepskyblue",
        Color(0xFF696969u) to "dimgray",
        Color(0xFF1E90FFu) to "dodgerblue",
        Color(0xFFB22222u) to "firebrick",
        Color(0xFFFFFAF0u) to "floralwhite",
        Color(0xFF228B22u) to "forestgreen",
        Color(0xFFDCDCDCu) to "gainsboro",
        Color(0xFFF8F8FFu) to "ghostwhite",
        Color(0xFFFFD700u) to "gold",
        Color(0xFFDAA520u) to "goldenrod",
        Color(0xFFADFF2Fu) to "greenyellow",
        Color(0xFFF0FFF0u) to "honeydew",
        Color(0xFFFF69B4u) to "hotpink",
        Color(0xFFCD5C5Cu) to "indianred",
        Color(0xFF4B0082u) to "indigo",
        Color(0xFFFFFFF0u) to "ivory",
        Color(0xFFF0E68Cu) to "khaki",
        Color(0xFFE6E6FAu) to "lavender",
        Color(0xFFFFF0F5u) to "lavenderblush",
        Color(0xFF7CFC00u) to "lawngreen",
        Color(0xFFFFFACDu) to "lemonchiffon",
        Color(0xFFADD8E6u) to "lightblue",
        Color(0xFFF08080u) to "lightcoral",
        Color(0xFFE0FFFFu) to "lightcyan",
        Color(0xFFFAFAD2u) to "lightgoldenrodyellow",
        Color(0xFFD3D3D3u) to "lightgray",
        Color(0xFF90EE90u) to "lightgreen",
        Color(0xFFFFB6C1u) to "lightpink",
        Color(0xFFFFA07Au) to "lightsalmon",
        Color(0xFF20B2AAu) to "lightseagreen",
        Color(0xFF87CEFAu) to "lightskyblue",
        Color(0xFF778899u) to "lightslategray",
        Color(0xFFB0C4DEu) to "lightsteelblue",
        Color(0xFFFFFFE0u) to "lightyellow",
        Color(0xFF32CD32u) to "limegreen",
        Color(0xFFFAF0E6u) to "linen",
        Color(0xFF66CDAAu) to "mediumaquamarine",
        Color(0xFF0000CDu) to "mediumblue",
        Color(0xFFBA55D3u) to "mediumorchid",
        Color(0xFF9370DBu) to "mediumpurple",
        Color(0xFF3CB371u) to "mediumseagreen",
        Color(0xFF7B68EEu) to "mediumslateblue",
        Color(0xFF00FA9Au) to "mediumspringgreen",
        Color(0xFF48D1CCu) to "mediumturquoise",
        Color(0xFFC71585u) to "mediumvioletred",
        Color(0xFF191970u) to "midnightblue",
        Color(0xFFF5FFFAu) to "mintcream",
        Color(0xFFFFE4E1u) to "mistyrose",
        Color(0xFFFFE4B5u) to "moccasin",
        Color(0xFFFFDEADu) to "navajowhite",
        Color(0xFFFDF5E6u) to "oldlace",
        Color(0xFF6B8E23u) to "olivedrab",
        Color(0xFFFF4500u) to "orangered",
        Color(0xFFDA70D6u) to "orchid",
        Color(0xFFEEE8AAu) to "palegoldenrod",
        Color(0xFF98FB98u) to "palegreen",
        Color(0xFFAFEEEEu) to "paleturquoise",
        Color(0xFFDB7093u) to "palevioletred",
        Color(0xFFFFEFD5u) to "papayawhip",
        Color(0xFFFFDAB9u) to "peachpuff",
        Color(0xFFCD853Fu) to "peru",
        Color(0xFFFFC8CBu) to "pink",
        Color(0xFFDDA0DDu) to "plum",
        Color(0xFFB0E0E6u) to "powderblue",
        Color(0xFFBC8F8Fu) to "rosybrown",
        Color(0xFF4169E1u) to "royalblue",
        Color(0xFF8B4513u) to "saddlebrown",
        Color(0xFFFA8072u) to "salmon",
        Color(0xFFF4A460u) to "sandybrown",
        Color(0xFF2E8B57u) to "seagreen",
        Color(0xFFFFF5EEu) to "seashell",
        Color(0xFFA0522Du) to "sienna",
        Color(0xFF87CEEBu) to "skyblue",
        Color(0xFF6A5ACDu) to "slateblue",
        Color(0xFF708090u) to "slategray",
        Color(0xFFFFFAFAu) to "snow",
        Color(0xFF00FF7Fu) to "springgreen",
        Color(0xFF4682B4u) to "steelblue",
        Color(0xFFD2B48Cu) to "tan",
        Color(0xFFD8DBD8u) to "thistle",
        Color(0xFFFF6347u) to "tomato",
        Color(0xFF40E0D0u) to "turquoise",
        Color(0xFFEE82EEu) to "violet",
        Color(0xFFF5DEB3u) to "wheat",
        Color(0xFFF5F5F5u) to "whitesmoke",
        Color(0xFF9ACD32u) to "yellowgreen",
        Color(0xFF663399u) to "rebeccapurple",
    )
}
