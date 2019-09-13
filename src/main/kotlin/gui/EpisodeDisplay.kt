package main.gui

import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import main.util.MediaData
import main.util.getPictureBySize
import tornadofx.*

class EpisodeDisplay(episode: MediaData) : Fragment() {
  override val root = vbox {
    hbox {
      add(episode.pictures.getPictureBySize("50X50")!!.getJavafxImage())
      vbox {
        paddingLeft = 10
        label(episode.name) {
          font = Font.font("System", FontWeight.EXTRA_BOLD, 16.0)
          isWrapText = true
        }
        label(episode.description) {
          isWrapText = true
        }
      }
    }
  }

}