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
      lineTo(15.09f, 8.26f)
      lineTo(22f, 9.27f)
      lineTo(17f, 14.14f)
      lineTo(18.18f, 21.02f)
      lineTo(12f, 17.77f)
      lineTo(5.82f, 21.02f)
      lineTo(7f, 14.14f)
      lineTo(2f, 9.27f)
      lineTo(8.91f, 8.26f)
      close()
    }
  .build().also { _star = it }

private var _star: ImageVector? = null
