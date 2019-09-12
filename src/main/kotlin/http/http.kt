package http

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import java.io.File

suspend fun download(url: String, client: HttpClient, reqBody: String = "") = client.request<String> {
  headers {
    append(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36"
    )
  }
  url(url)
  method = HttpMethod.Get
  body = reqBody
}

suspend fun downloadBinary(url: String, client: HttpClient) = client.request<ByteArray> {
  headers {
    append(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36"
    )
  }
  url(url)
  method = HttpMethod.Get
}

suspend fun downloadTSFiles(files: MutableList<Pair<String, Int>>, client: HttpClient, outputDirectory: File) {
  var totalDuration = 0
  val outputFilePath = outputDirectory.absolutePath
  var k = 0
  val pb = ProgressBarBuilder().setInitialMax(files.size.toLong()).setUpdateIntervalMillis(10).setStyle(ProgressBarStyle.ASCII).build()
  withContext(Dispatchers.IO) {
    files.forEach {
      val (url, duration) = it
      val fileName = "video-$totalDuration-${totalDuration + duration}.mpeg"
      totalDuration += duration
      launch {
        val data = downloadBinary(url, client)
        val file = File("$outputFilePath/$fileName")
        file.writeBytes(data)
        k++
        pb.step()
        //Done
        if (k == files.size - 1) {
          //            Update progressbar
          delay(500)
          pb.close()
        }
      }
    }
  }
}

suspend fun getVideoData(url: String, client: HttpClient): Triple<String, String, Int> {
  val id = url.substringAfterLast("/")
  val initialResponse = download(url, client)
  val (user, pass) = parseApiCred(initialResponse)?.destructured
          ?: throw Exception("Credentials not found")
  val downloadUrlParams = """{
        "initObj": {
			"Locale": {
				"LocaleLanguage": "", "LocaleCountry": "",
				"LocaleDevice": "", "LocaleUserState": 0
			},
			"Platform": 0, "SiteGuid": 0, "DomainID": "0", "UDID": "",
			"ApiUser": "$user", "ApiPass": "$pass"
		},
		"MediaID": $id,
		"mediaType": 0
    }
    """.trimIndent()
  //    Download media data
  val rawData = download(
          "http://tvpapi.as.tvinci.com/v3_9/gateways/jsonpostgw.aspx?m=GetMediaInfo",
          client,
          downloadUrlParams
  )
  //    Filter raw data to get correct file
  val filteredMediaData = rawData.substringAfter("\"Files\":[{")
          .substringBefore("}],\"Pictures")
          .split("},{")
          .find { it.contains("\"Format\":\"HLS_Web\"") }
          ?: throw Exception("M3U8 file not found")
  val m3Url = filteredMediaData
          .substringAfter("\"URL\":\"")
          .substringBefore("\"")
  val mediaName = rawData.substringAfter("\"MediaName\":\"").substringBefore("\"")
  val duration = filteredMediaData.substringAfter("\"Duration\":\"").substringBefore("\"").toInt()
  return Triple(m3Url, mediaName, duration)
}

fun parseApiCred(resp: String) =
        Regex(
                """apiUser:\s*"([^"]+?)".+?apiPass:\s*"([^"]+?)"""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        ).find(resp)