import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val visibilitystrike: ImageVector
  get() = _visibilitystrike ?: ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    .path(
      fill = SolidColor(Color(0, 0, 0, 255)),
    ) {
      moveTo(2f, 4.27f)
      lineTo(3.27f, 3f)
      lineTo(3.27f, 3f)
      lineTo(2f, 4.27f)
      close()
    }
    .group(
      clipPathData = listOf(
        PathNode.MoveTo(0f, 0.0f),
        PathNode.LineTo(24f, 0f),
        PathNode.LineTo(24f, 24f),
        PathNode.LineTo(0f, 24f),
        PathNode.LineTo(0f, 0f),
        PathNode.Close,
        PathNode.MoveTo(4.54f, 1.73f),
        PathNode.LineTo(3.27f, 3f),
        PathNode.LineTo(3.27f, 3f),
        PathNode.LineTo(4.54f, 1.73f),
        PathNode.Close,
      ),
    ) {
      path(
        fill = SolidColor(Color(0, 0, 0, 255)),
      ) {
        moveTo(12f, 4.5f)
        curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
        curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
        curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
        curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
        lineTo(12f, 4.5f)
        close()
        moveTo(12f, 17.0f)
        curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
        curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
        curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
        curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
        lineTo(12f, 17f)
        close()
        moveTo(12f, 9.0f)
        curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
        curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
        curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
        curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
        lineTo(12f, 9f)
        close()
      }
    }
  .build().also { _visibilitystrike = it }

private var _visibilitystrike: ImageVector? = null
