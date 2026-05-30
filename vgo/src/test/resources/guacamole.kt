import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val vector: ImageVector
  get() = _vector ?: ImageVector.Builder(defaultWidth = 1280.dp, defaultHeight = 800.dp, viewportWidth = 1280f, viewportHeight = 800f)
    .group(
      rotate = 0f,
      scaleX = 1f,
      scaleY = 1f,
      translationX = 0f,
      translationY = 0f,
    ) {
      group(
        clipPathData = listOf(
          PathNode.MoveTo(858.3f, 326.1f),
          PathNode.RelativeLineTo(-565.4f, -67.2f),
          PathNode.LineTo(283f, 383f),
          PathNode.RelativeLineTo(565.4f, 67.3f),
          PathNode.RelativeLineTo(9.9f, -124.2f),
        ),
      ) {
        group(
          rotate = 0f,
          scaleX = 1f,
          scaleY = 1f,
          translationX = 0f,
          translationY = 0f,
        ) {
          path(
            fill = SolidColor(Color(63, 178, 73, 255)),
          ) {
            moveTo(443.7f, 298.5f)
            curveToRelative(5.1f, -1.1f, 9.8f, -3.5f, 13.7f, -6.7f)
            curveToRelative(5.7f, 8.7f, 15.6f, 14.4f, 26.8f, 14.4f)
            curveToRelative(9.1f, 0f, 17.2f, -3.7f, 23.1f, -9.7f)
            curveToRelative(6.3f, 2.5f, 13.5f, 3.9f, 21.2f, 3.9f)
            curveToRelative(9.2f, 0f, 17.8f, -2.1f, 24.8f, -5.5f)
            curveToRelative(4f, 13f, 16.2f, 22.4f, 30.6f, 22.4f)
            curveToRelative(5.1f, 0f, 9.8f, -1.1f, 14f, -3.1f)
            curveToRelative(5.6f, 7.5f, 16f, 12.5f, 28f, 12.5f)
            curveToRelative(6.2f, 0f, 12f, -1.3f, 16.9f, -3.7f)
            curveToRelative(5.9f, 7.4f, 15f, 12.2f, 25.2f, 12.2f)
            curveToRelative(8.1f, 0f, 15.4f, -2.9f, 21f, -7.8f)
            curveToRelative(5.9f, 5.4f, 14.7f, 8.8f, 24.5f, 8.8f)
            curveToRelative(8.3f, 0f, 15.9f, -2.4f, 21.6f, -6.5f)
            curveToRelative(5.8f, 8.2f, 15.5f, 13.6f, 26.3f, 13.6f)
            curveToRelative(6.5f, 0f, 12.4f, -1.8f, 17.4f, -5f)
            curveToRelative(4.3f, 3.2f, 9.4f, 5.2f, 14.8f, 5.8f)
            curveToRelative(4.9f, 5.6f, 12.6f, 9.5f, 21.4f, 10.6f)
            curveToRelative(5.9f, 7.1f, 14.8f, 11.6f, 24.8f, 11.6f)
            lineToRelative(1.7f, 0f)
            curveToRelative(-3.1f, 3.8f, -4.8f, 8.3f, -4.8f, 13.2f)
            curveToRelative(0f, 8.6f, 5.5f, 16.2f, 14f, 20.7f)
            curveTo(828f, 489.6f, 702.3f, 546.4f, 556.8f, 529.1f)
            curveToRelative(-139.3f, -16.5f, -249.9f, -95.4f, -266.4f, -183.7f)
            curveToRelative(7.8f, -4.6f, 13f, -11.9f, 13f, -20.2f)
            curveToRelative(0f, -.7f, -.1f, -1.3f, -.2f, -2f)
            curveToRelative(2.1f, .3f, 4.2f, .5f, 6.4f, .5f)
            curveToRelative(12.1f, 0f, 22.6f, -5.2f, 28.1f, -12.9f)
            curveToRelative(2.8f, .7f, 5.7f, 1.1f, 8.7f, 1.1f)
            curveToRelative(3.6f, 0f, 7.1f, -.6f, 10.4f, -1.7f)
            curveToRelative(5.9f, 6.4f, 14.3f, 10.4f, 23.7f, 10.4f)
            curveToRelative(9.6f, 0f, 18.1f, -4.1f, 24f, -10.7f)
            curveToRelative(3.4f, 1f, 7.1f, 1.5f, 11f, 1.5f)
            curveToRelative(12.1f, 0f, 22.7f, -5.2f, 28.2f, -12.9f)
            close()
            moveToRelative(391.6f, 4.5f)
            lineToRelative(.2f, .3f)
            curveToRelative(-.2f, 0f, -.4f, 0f, -.6f, .1f)
            curveToRelative(.1f, -.2f, .1f, -.5f, .2f, -.7f)
            lineToRelative(.2f, .3f)
            close()
            moveToRelative(-511.7f, -56.5f)
            curveToRelative(-2f, -1.1f, -4.1f, -2f, -6.4f, -2.6f)
            curveToRelative(48.5f, -59.7f, 152.2f, -93.3f, 268.6f, -79.4f)
            curveToRelative(89.9f, 10.6f, 167.9f, 47.3f, 216.3f, 95.7f)
            curveToRelative(-13.2f, 1.2f, -24.1f, 10.4f, -27.7f, 22.6f)
            curveToRelative(-4f, -1.8f, -8.3f, -2.7f, -13f, -2.7f)
            curveToRelative(-4.6f, 0f, -9.1f, .9f, -13.1f, 2.7f)
            curveToRelative(0f, -17.4f, -14.3f, -31.6f, -32f, -31.6f)
            curveToRelative(-15.5f, 0f, -28.5f, 10.9f, -31.4f, 25.5f)
            curveToRelative(-4.9f, -3.1f, -10.7f, -4.8f, -16.9f, -4.8f)
            curveToRelative(-1.1f, 0f, -2.2f, 0f, -3.3f, .2f)
            lineToRelative(0f, -.4f)
            curveToRelative(0f, -17.4f, -19.7f, -31.6f, -44f, -31.6f)
            curveToRelative(-15.1f, 0f, -28.5f, 5.5f, -36.4f, 13.9f)
            lineToRelative(-.4f, 0f)
            curveToRelative(-5.1f, 0f, -10f, 1.2f, -14.4f, 3.4f)
            curveToRelative(-6.3f, -11.9f, -22.3f, -20.3f, -41f, -20.3f)
            curveToRelative(-11.9f, 0f, -22.6f, 3.4f, -30.5f, 8.9f)
            curveToRelative(-4.2f, -2f, -8.9f, -3.1f, -13.8f, -3.1f)
            curveToRelative(-7.8f, 0f, -15f, 2.8f, -20.5f, 7.4f)
            curveToRelative(-5.7f, -8.7f, -15.6f, -14.4f, -26.8f, -14.4f)
            curveToRelative(-6.4f, 0f, -12.4f, 1.9f, -17.4f, 5.1f)
            curveToRelative(-6.3f, -11.9f, -22.3f, -20.4f, -41.1f, -20.4f)
            curveToRelative(-17.5f, 0f, -32.6f, 7.4f, -39.7f, 18.1f)
            curveToRelative(-5.6f, 1.3f, -10.8f, 4f, -15.1f, 7.8f)
            close()
          }
        }
      }
    }
    .path(
      fill = SolidColor(Color(29, 29, 29, 255)),
    ) {
      moveTo(253.2f, 327.3f)
      curveToRelative(-1.1f, -3.8f, .1f, -7.8f, 3.2f, -10.3f)
      curveToRelative(3f, -2.6f, 7.2f, -3.1f, 10.7f, -1.3f)
      curveToRelative(95.4f, 46.3f, 216.3f, 74f, 347.8f, 73.9f)
      curveToRelative(95.1f, 0f, 184.7f, -14.6f, 263.1f, -40.3f)
      curveToRelative(3.7f, -1.2f, 7.8f, -.2f, 10.5f, 2.7f)
      curveToRelative(2.6f, 2.8f, 3.4f, 7f, 1.9f, 10.6f)
      curveToRelative(-51.1f, 122f, -171.7f, 207.9f, -312.2f, 208f)
      curveToRelative(-153.8f, .1f, -283.9f, -102.7f, -325f, -243.3f)
      close()
    }
  .build().also { _vector = it }

private var _vector: ImageVector? = null
