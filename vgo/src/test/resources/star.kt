import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

val star: ImageVector
    get() = _star ?: ImageVector.Builder(
        name = "star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFFFC107)),
            pathData = PathData {
                moveTo(12.0f, 2.0f)
                lineTo(15.09f, 8.26f)
                lineTo(22.0f, 9.27f)
                lineTo(17.0f, 14.14f)
                lineTo(18.18f, 21.02f)
                lineTo(12.0f, 17.77f)
                lineTo(5.82f, 21.02f)
                lineTo(7.0f, 14.14f)
                lineTo(2.0f, 9.27f)
                lineTo(8.91f, 8.26f)
                close()
            }
        )
    }.build().also { _star = it }

private var _star: ImageVector? = null