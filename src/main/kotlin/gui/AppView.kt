package main.gui

import http.download
import http.downloadTSFiles
import http.getVideoData
import io.ktor.client.HttpClient
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import kotlinx.coroutines.runBlocking
import main.util.parseResolutions
import main.util.parseStreamUrl
import tornadofx.*
import util.secondsToString
import java.io.File
import java.net.ConnectException

class AppView : View() {

  override val root: AnchorPane by fxml("/main.fxml")
  private val toggleUrl: TextField by fxid("toggleUrl")
  private val checkUrlBtn: Button by fxid("downloadButton")
  private val mediaData: Label by fxid("mediaData")
  private val resolutionSelectHBox: HBox by fxid("resolutionSelectHbox")
  private val resolutionSelect: ComboBox<String> by fxid("resolutionSelect")
  private val resolutionSelectButton: Button by fxid("confirmResolution")
  private val progressBar: ProgressBar by fxid("progressBar")
  private val progressLabel: Label by fxid("progressExact")

  private val client = HttpClient()

  private val status: TaskStatus by inject()
  private lateinit var resolutions: List<Pair<String, String>>
  private lateinit var outputDirectory: File

  init {

    checkUrlBtn.setOnAction {
      val url = toggleUrl.text
      //      Get media name
      mediaData.text = "Checking..."
      runAsync {
        try {
          runBlocking {
            val (m3Url, mediaName, mediaDuration) = getVideoData(url, client)
            //            Create media output directory
            outputDirectory = File("output-$mediaName")
            if (!outputDirectory.exists()) outputDirectory.mkdir()
            //            Get resolutions
            resolutions = parseResolutions(download(m3Url, client))
            """
            Media name: $mediaName
            Runtime: $mediaDuration seconds (${secondsToString(mediaDuration)})
            """.trimIndent()
          }
        } catch (e: ConnectException) {
          "Connection failed: $e"
        }
      } ui {
        mediaData.text = it

        if (this::resolutions.isInitialized) {
          //Set resolution data
          resolutionSelect.items.addAll(resolutions.map { (key, _) -> key })
          resolutionSelect.selectionModel.select(0)
          //Enable and disable stuffs
          checkUrlBtn.isDisable = true
          toggleUrl.isDisable = true
          resolutionSelectHBox.isVisible = true
        }
      }
    }


    resolutionSelectButton.setOnAction {
      val selectedResolution = resolutionSelect.selectionModel.selectedItem
      val streamUrl = this.resolutions.find { (k, _) -> k == selectedResolution }?.second!!
      //    Progress bars
      progressBar.progressProperty().bind(status.progress)
      progressLabel.textProperty().bind(status.message)
      runAsync {
        runBlocking {
          val tsFiles = parseStreamUrl(download(streamUrl, client))
          updateMessage("0/${tsFiles.size}")
          downloadTSFiles(tsFiles, client, outputDirectory) { current, max ->
            updateProgress(current.toLong(), max.toLong())
            updateMessage("$current/$max")
          }
        }
      }ui {
        alert(Alert.AlertType.INFORMATION,header = "Completed"){
          contentText ="Download completed successfully"
          showAndWait()
        }
      }
    }
  }

}