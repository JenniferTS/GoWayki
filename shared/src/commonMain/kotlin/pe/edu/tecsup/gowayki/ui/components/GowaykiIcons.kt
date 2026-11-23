package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos lúdicos estilo Phosphor/Bootstrap rounded (stroke suave, esquinas redondas).
 * Ligero: sin dependencia extra; coherente con la marca GoWayki Go.
 */
object GowaykiIcons {
    val Routes: ImageVector by lazy {
        materialIcon("Routes") {
            // mapa + pin
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 6.5f)
                lineTo(9.5f, 4.5f)
                lineTo(14.5f, 6.5f)
                lineTo(19.5f, 4.5f)
                lineTo(19.5f, 17.5f)
                lineTo(14.5f, 19.5f)
                lineTo(9.5f, 17.5f)
                lineTo(4.5f, 19.5f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(15.5f, 10f)
                curveToRelative(0f, -1.4f, -1.1f, -2.5f, -2.5f, -2.5f)
                reflectiveCurveToRelative(-2.5f, 1.1f, -2.5f, 2.5f)
                curveToRelative(0f, 1.8f, 2.5f, 4.5f, 2.5f, 4.5f)
                reflectiveCurveToRelative(2.5f, -2.7f, 2.5f, -4.5f)
                close()
            }
        }
    }

    val Search: ImageVector by lazy {
        materialIcon("Search") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(11f, 11f)
                moveToRelative(-5.5f, 0f)
                arcToRelative(5.5f, 5.5f, 0f, true, true, 11f, 0f)
                arcToRelative(5.5f, 5.5f, 0f, true, true, -11f, 0f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(15.5f, 15.5f)
                lineTo(20f, 20f)
            }
        }
    }

    val Report: ImageVector by lazy {
        materialIcon("Report") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.5f)
                lineTo(20.5f, 18.5f)
                lineTo(3.5f, 18.5f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 9.5f)
                lineTo(12f, 13f)
            }
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(12f, 15.8f)
                moveToRelative(-0.9f, 0f)
                arcToRelative(0.9f, 0.9f, 0f, true, true, 1.8f, 0f)
                arcToRelative(0.9f, 0.9f, 0f, true, true, -1.8f, 0f)
            }
        }
    }

    val Profile: ImageVector by lazy {
        materialIcon("Profile") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 8f)
                moveToRelative(-3.2f, 0f)
                arcToRelative(3.2f, 3.2f, 0f, true, true, 6.4f, 0f)
                arcToRelative(3.2f, 3.2f, 0f, true, true, -6.4f, 0f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.5f, 19.5f)
                curveToRelative(0.8f, -3.2f, 3.2f, -5f, 6.5f, -5f)
                reflectiveCurveToRelative(5.7f, 1.8f, 6.5f, 5f)
            }
        }
    }

    val Bus: ImageVector by lazy {
        materialIcon("Bus") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 5.5f)
                horizontalLineToRelative(12f)
                curveToRelative(1f, 0f, 1.5f, 0.8f, 1.5f, 1.8f)
                verticalLineToRelative(9.2f)
                horizontalLineToRelative(-15f)
                verticalLineToRelative(-9.2f)
                curveToRelative(0f, -1f, 0.5f, -1.8f, 1.5f, -1.8f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6.5f, 11f)
                horizontalLineToRelative(11f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.2f, 17.8f)
                moveToRelative(-1.2f, 0f)
                arcToRelative(1.2f, 1.2f, 0f, true, true, 2.4f, 0f)
                arcToRelative(1.2f, 1.2f, 0f, true, true, -2.4f, 0f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.8f, 17.8f)
                moveToRelative(-1.2f, 0f)
                arcToRelative(1.2f, 1.2f, 0f, true, true, 2.4f, 0f)
                arcToRelative(1.2f, 1.2f, 0f, true, true, -2.4f, 0f)
            }
        }
    }

    val Location: ImageVector by lazy {
        materialIcon("Location") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 21f)
                curveToRelative(0f, 0f, 7f, -5.2f, 7f, -11f)
                arcToRelative(7f, 7f, 0f, true, false, -14f, 0f)
                curveToRelative(0f, 5.8f, 7f, 11f, 7f, 11f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
            ) {
                moveTo(12f, 10f)
                moveToRelative(-2.2f, 0f)
                arcToRelative(2.2f, 2.2f, 0f, true, true, 4.4f, 0f)
                arcToRelative(2.2f, 2.2f, 0f, true, true, -4.4f, 0f)
            }
        }
    }

    val Game: ImageVector by lazy {
        materialIcon("Game") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 9f)
                horizontalLineToRelative(10f)
                curveToRelative(2.2f, 0f, 3.5f, 1.8f, 3f, 4f)
                lineToRelative(-0.8f, 3.2f)
                curveToRelative(-0.3f, 1.2f, -1.4f, 2f, -2.6f, 2f)
                horizontalLineToRelative(-9.2f)
                curveToRelative(-1.2f, 0f, -2.3f, -0.8f, -2.6f, -2f)
                lineTo(4f, 13f)
                curveToRelative(-0.5f, -2.2f, 0.8f, -4f, 3f, -4f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(9f, 13f)
                horizontalLineToRelative(2.5f)
                moveTo(10.25f, 11.75f)
                verticalLineToRelative(2.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.2f, 12.2f)
                moveToRelative(-0.9f, 0f)
                arcToRelative(0.9f, 0.9f, 0f, true, true, 1.8f, 0f)
                arcToRelative(0.9f, 0.9f, 0f, true, true, -1.8f, 0f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.2f, 14.2f)
                moveToRelative(-0.9f, 0f)
                arcToRelative(0.9f, 0.9f, 0f, true, true, 1.8f, 0f)
                arcToRelative(0.9f, 0.9f, 0f, true, true, -1.8f, 0f)
            }
        }
    }

    val Lines: ImageVector by lazy {
        materialIcon("Lines") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4.5f, 7f)
                horizontalLineToRelative(15f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4.5f, 12f)
                horizontalLineToRelative(15f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4.5f, 17f)
                horizontalLineToRelative(10f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.5f, 17f)
                moveToRelative(-1.4f, 0f)
                arcToRelative(1.4f, 1.4f, 0f, true, true, 2.8f, 0f)
                arcToRelative(1.4f, 1.4f, 0f, true, true, -2.8f, 0f)
            }
        }
    }

    val Back: ImageVector by lazy {
        materialIcon("Back") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14.5f, 6f)
                lineTo(8.5f, 12f)
                lineTo(14.5f, 18f)
            }
        }
    }
}

private inline fun materialIcon(
    name: String,
    block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    name = name,
).apply(block).build()
