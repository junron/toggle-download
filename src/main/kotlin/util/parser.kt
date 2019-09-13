package main.util

fun parseResolutions(data : String): MutableList<Pair<String, String>> {
  val resolutions = mutableListOf<Pair<String,String>>()
  var currentKey = ""
  data.lines().forEach {
    if (it.contains("RESOLUTION")) {
      currentKey = it.substringAfterLast("=")
    } else if (it.contains("http")) {
      resolutions.add(currentKey to it.substringAfterLast(" "))
    }
  }
  return resolutions
}

fun parseStreamUrl(data : String): MutableList<Pair<String, Int>> {
  val tsFiles = mutableListOf<Pair<String, Int>>()
  var currentFileTime = -1
  data.lines().forEach {
    if (it.startsWith("#EXTINF")) {
      // Time indicator
      currentFileTime = it.substringAfter(":").substringBefore(".").toInt()
    } else if (it.contains("http")) {
      // Url
      tsFiles.add(it to currentFileTime)
    }
  }
  return tsFiles
}