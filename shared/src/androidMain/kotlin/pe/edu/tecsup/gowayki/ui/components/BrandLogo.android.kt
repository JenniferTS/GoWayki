package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.myapplication.common.R
import pe.edu.tecsup.gowayki.ui.theme.GowaykiElevation
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme

@Composable
actual fun BrandLogo(
    modifier: Modifier,
    size: Dp,
) {
    // PNG del adjunto ya viene con fondo blanco; se recorta en círculo.
    Image(
        painter = painterResource(R.drawable.logo_gowayki_go),
        contentDescription = "gowayki go",
        modifier = modifier
            .size(size)
            .shadow(if (size >= GowaykiSize.logoLg) GowaykiElevation.panel else GowaykiElevation.low, CircleShape, spotColor = GowaykiTheme.colors.shadow)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}
