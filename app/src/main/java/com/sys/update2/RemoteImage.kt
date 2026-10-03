// language: Kotlin, file: RemoteImage.kt
// *يستخدم Coil لتحميل وعرض الصور من GitHub مع placeholder أثناء التحميل*

package com.sys.update2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * يعرض صورة من GitHub.
 *
 * @param name اسم الملف بدون .png — مثل "tab_home" أو "banner_rooms"
 * @param contentDescription وصف للصورة (للإتاحة)
 * @param contentScale طريقة القص
 * @param modifier أبعاد الصورة
 */
@Composable
fun RemoteImage(
    name: String,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = RemoteAssets.url(name),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Placeholder بسيط يظهر أثناء التحميل (دائرة رمادية).
 * يمكن استخدامه في الـ AsyncImage عبر placeholder parameter لاحقاً.
 */
@Composable
private fun PlaceholderDot() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0xFF2A2F45),
            radius = size.minDimension * 0.3f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}
