package main

import http.download
import http.downloadTSFiles
import http.getVideoData
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import util.secondsToString
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    print("Enter toggle URL: ")
    val reader = Scanner(System.`in`)
    val url = reader.nextLine()!!.trim()
    val client = HttpClient()
    runBlocking {
        val (m3Url, mediaName, mediaDuration) = getVideoData(url, client)
        println("Processing video: $mediaName")
        println("Runtime: $mediaDuration seconds (${secondsToString(mediaDuration)})")

        val lines = download(m3Url, client).lines()
        val resolutions = mutableMapOf<String, String>()
        var currentKey = ""
        lines.forEach {
            if (it.contains("RESOLUTION")) {
                currentKey = it.substringAfterLast("=")
            } else if (it.contains("http")) {
                resolutions[currentKey] = it.substringAfterLast(" ")
            }
        }
        if (resolutions.isEmpty()) {
            println("No resolutions found.")
            return@runBlocking
        }
        println("Select resolution: ")
        resolutions.keys.forEachIndexed { i, v ->
            println("[${i + 1}]: $v")
        }
        print("Enter choice [1 to ${resolutions.keys.size}]: ")
        val resolutionChoice = reader.nextInt()
        println("You chose: ${resolutions.keys.elementAt(resolutionChoice - 1)}")
        //        This cannot be null
        val streamUrl = resolutions[resolutions.keys.elementAt(resolutionChoice - 1)]!!
        val outputDirectory = File("output-$mediaName")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdir()
        }
        val tsFiles = hashMapOf<String, Int>()
        var currentVal = -1
        download(streamUrl, client).lines().forEach {
            if (it.startsWith("#EXTINF")) {
//                Time indicator
                currentVal = it.substringAfter(":").substringBefore(".").toInt()
            } else if (it.contains("http")) {
//                Url
                tsFiles[it] = currentVal
            }
        }
        downloadTSFiles(tsFiles, client, outputDirectory)
        client.close()
    }
}
