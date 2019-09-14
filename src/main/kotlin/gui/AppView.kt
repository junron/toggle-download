package main.gui

import http.download
import http.downloadMediaFiles
import http.getSubtitles
import http.getVideoData
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.runBlocking
import main.util.*
import tornadofx.*
import java.io.File
import java.net.ConnectException

class AppView : View() {

  override val root: AnchorPane by fxml("/main.fxml")
  private val toggleUrl: TextField by fxid("toggleUrl")
  private val checkUrlBtn: Button by fxid("downloadButton")
  private val mediaData: HBox by fxid("mediaData")
  private val resolutionSelectHBox: HBox by fxid("resolutionSelectHbox")
  private val resolutionSelect: ComboBox<String> by fxid("resolutionSelect")
  private val downloadBtn: Button by fxid("confirmResolution")
  private val progressBar: ProgressBar by fxid("progressBar")
  private val progressLabel: Label by fxid("progressExact")
  private val outDirLabel: Label by fxid("outputDirectory")
  private val selectDir: Button by fxid("selectOutputDirectory")


  private val client = HttpClient()

  private val status: TaskStatus by inject()
  private lateinit var resolutions: List<Pair<String, String>>
  private lateinit var outputDirectory: File
  private lateinit var subtitleUrl: String

  init {
    checkUrlBtn.action {
      val url = toggleUrl.text
      //      Get media name
      mediaData.getChildList()?.clear()
      mediaData.getChildList()?.add(label("Checking..."))
      runAsync {
        try {
          runBlocking {
            subtitleUrl = getSubtitles(url, client).subtitleFiles.find { it.subtitleFileLanguage == "English" }?.subtitleFileUrl
                    ?: ""
            val mediaData = getVideoData(url, client)
            val m3Url = mediaData.files.getM3U8FileByFormat("HLS_Web")?.url
                    ?: return@runBlocking errorLabel("M3U8 file with suitable format not found")
            //            Get resolutions
            resolutions = parseResolutions(download(m3Url, client))
            mediaData.renderGUIComponent()
          }
        } catch (e: Exception) {
          when (e) {
            is ConnectException, is ClientRequestException -> {
              errorLabel("Connection failed: $e")
            }
            else -> {
              e.printStackTrace()
              errorLabel("Unknown exception: $e")
            }
          }
        }
      } ui {
        mediaData.setChild(it.root)

        if (this::resolutions.isInitialized) {
          //Set resolution data
          resolutionSelect.items.addAll(resolutions.map { (key, _) -> key })
          resolutionSelect.selectionModel.select(0)
          //Enable and disable stuffs
          checkUrlBtn.isDisable = true
          toggleUrl.isDisable = true
          resolutionSelectHBox.isVisible = true
          selectDir.isDisable = false
        }
      }
    }

    selectDir.action {
      val selectedDirectory = with(DirectoryChooser()) {
        title = "Choose directory"
        showDialog(null)
      }
      if (selectedDirectory == null) {
        outDirLabel.text = "None"
        downloadBtn.isDisable = true
      } else {
        outputDirectory = selectedDirectory
        outDirLabel.text = outputDirectory.path
        downloadBtn.isDisable = false
      }
    }

    downloadBtn.action {
      val selectedResolution = resolutionSelect.selectionModel.selectedItem
      val streamUrl = this.resolutions.find { (k, _) -> k == selectedResolution }?.second!!
      //    Progress bars
      progressBar.progressProperty().bind(status.progress)
      progressLabel.textProperty().bind(status.message)
      runAsync {
        runBlocking {
          val tsFiles = parseStreamUrl(download(streamUrl, client))
          if (subtitleUrl.isNotEmpty()) {
            tsFiles.add(Pair(subtitleUrl, 0))
          }
          updateMessage("0/${tsFiles.size}")
          downloadMediaFiles(tsFiles, client, outputDirectory) { current, max ->
            updateProgress(current.toLong(), max.toLong())
            updateMessage("$current/$max")
          }
        }
      } ui {
        alert(Alert.AlertType.INFORMATION, header = "Completed") {
          contentText = "Download completed successfully"
          showAndWait()
        }
      }
    }
  }

}