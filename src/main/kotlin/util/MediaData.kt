package main.util

import com.beust.klaxon.Json
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import main.gui.EpisodeDisplay
import tornadofx.*

data class MediaData(
        @Json(name = "MediaName")
        val name: String,
        @Json(name = "Description")
        val description: String,
        @Json(name = "Duration")
        val duration: String,
        @Json(name = "Files")
        val files: List<M3U8File>,
        @Json(name = "Pictures")
        val pictures: List<MediaPicture>
) {
  fun renderGUIComponent() = EpisodeDisplay(this)
}

fun List<MediaPicture>.getPictureBySize(size: String) = this.find { it.size == size }
fun List<M3U8File>.getM3U8FileByFormat(format: String) = this.find { it.format == format }

data class M3U8File(
        @Json(name = "URL")
        val url: String,
        @Json(name = "Format")
        val format: String
)

data class MediaPicture(
        @Json(name = "PicSize")
        val size: String,
        @Json(name = "URL")
        val url: String
) {
  fun getWidth() = size.substringBefore("X").toInt()
  fun getHeight() = size.substringAfter("X").toInt()
  fun getJavafxImage(): ImageView {
    val imageView = ImageView()
    imageView.image = Image(url)
    imageView.fitHeight = getHeight().toDouble()
    imageView.fitWidth = getWidth().toDouble()
    return imageView
  }
}