package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.myapplication.common.R

@Composable
actual fun BrandLogo(
    modifier: Modifier,
    size: Dp,
) {
    Image(
        painter = painterResource(R.drawable.logo_gowayki_go),
        contentDescription = "gowayki go",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}
