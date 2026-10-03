// language: Kotlin, file: RemoteAssets.kt
// *Coil يتولى التخزين المؤقت — الصور تُحفظ في cache/coil_cache*

package com.sys.update2

object RemoteAssets {

    private const val BASE_URL =
        "https://raw.githubusercontent.com/METAEGGAS/special-octo-goggleajajjajws/main/"

    /**
     * يعيد الرابط الكامل للصورة.
     * مثال: url("tab_home") → ".../main/tab_home.png"
     */
    fun url(name: String): String = BASE_URL + name + ".png"
}
