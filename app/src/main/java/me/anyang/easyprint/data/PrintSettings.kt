package me.anyang.easyprint.data

data class PrintSettings(
    val scale: Float = 100f,
    val isLandscape: Boolean = false,
    val pageRange: PageRange = PageRange.All,
    val paperSize: PaperSize = PaperSize.A4,
    val copies: Int = 1,
    val colorMode: ColorMode = ColorMode.Color,
    val duplexMode: DuplexMode = DuplexMode.Simplex
)

sealed class PageRange {
    data object All : PageRange()
    data object Odd : PageRange()
    data object Even : PageRange()
    data class Custom(val pages: List<Int>, val selectedPages: Set<Int> = emptySet()) : PageRange()
}

enum class PaperSize(val displayName: String, val widthMm: Float, val heightMm: Float) {
    A4("A4", 210f, 297f),
    A5("A5", 148f, 210f),
    Letter("Letter", 215.9f, 279.4f),
    Legal("Legal", 215.9f, 355.6f)
}

enum class ColorMode(val displayName: String) {
    Color("彩色"),
    Grayscale("灰度"),
    BlackWhite("黑白")
}

enum class DuplexMode(val displayName: String) {
    Simplex("单面打印"),
    LongEdge("双面打印 - 长边翻转"),
    ShortEdge("双面打印 - 短边翻转")
}

enum class FileType {
    PDF, IMAGE, UNKNOWN
}

data class PrintFile(
    val uri: String,
    val name: String,
    val type: FileType,
    val size: Long,
    val pageCount: Int = 1
)
