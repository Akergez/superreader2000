import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun BitmapCanvas(
    modifier: Modifier,
    bitmap: ImageBitmap,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier.fillMaxSize()
    )
}