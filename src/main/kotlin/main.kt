package main

import http.download
import http.downloadMediaFiles
import http.getVideoData
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import main.util.getM3U8FileByFormat
import main.util.parseResolutions
import main.util.parseStreamUrl
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import util.secondsToString
import java.io.File
import java.util.*

fun main() {
  print("Enter toggle URL: ")
  val reader = Scanner(System.`in`)
  val url = reader.nextLine()!!.trim()
  val client = HttpClient()
  runBlocking {
    val (mediaName, _, mediaDuration, files) = getVideoData(url, client)
    println("Processing video: $mediaName")
    println("Runtime: $mediaDuration seconds (${secondsToString(mediaDuration.toInt())})")
    val m3Url = files.getM3U8FileByFormat("HLS_Web")?.url
            ?: return@runBlocking println("M3U8 file with suitable format not found")
    val resolutionData = download(m3Url, client)
    val resolutions = parseResolutions(resolutionData)
    if (resolutions.isEmpty()) {
      println("No resolutions found.")
      return@runBlocking
    }
    println("Select resolution: ")
    resolutions.forEachIndexed { i, v ->
      println("[${i + 1}]: $v")
    }
    print("Enter choice [1 to ${resolutions.size}]: ")
    val resolutionChoice = reader.nextInt()
    println("You chose: ${resolutions[resolutionChoice - 1].first}")
    val streamUrl = resolutions[resolutionChoice - 1].second
    val outputDirectory = File("output-$mediaName")
    if (!outputDirectory.exists()) {
      outputDirectory.mkdir()
    }
    val tsFiles = parseStreamUrl(download(streamUrl, client))
    val pb = ProgressBarBuilder().setInitialMax(tsFiles.size.toLong()).setUpdateIntervalMillis(10).setStyle(ProgressBarStyle.ASCII).build()

    downloadMediaFiles(tsFiles, client, outputDirectory) { current, max ->
      pb.step()
      if (current == max) pb.close()
    }
    client.close()
  }
}
