import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val star: ImageVector
  get() = _star ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
      .path(
        fill = SolidColor(Color(0, 0, 0, 255)),
      ) {
        moveTo(12f, 2.0f)
        lineToRelative(3.09f, 6.26f)
        lineTo(22f, 9.27f)
        lineToRelative(-5f, 4.87f)
        lineToRelative(1.18f, 6.88f)
        lineTo(12f, 17.77f)
        lineToRelative(-6.18f, 3.25f)
        lineTo(7f, 14.14f)
        lineTo(2f, 9.27f)
        lineToRelative(6.91f, -1.01f)
        close()
      }
    .build().also { _star = it }

  private var _star: ImageVector? = null
