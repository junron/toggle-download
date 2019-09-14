package http

import com.beust.klaxon.Klaxon
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import main.util.MediaData
import main.util.SubtitleData
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

suspend fun downloadMediaFiles(
        files: MutableList<Pair<String, Int>>, client: HttpClient, outputDirectory: File,
        progressCallback: (current: Int, max: Int) -> Unit
) {
  var totalDuration = 0
  val outputFilePath = outputDirectory.absolutePath
  var k = 0
  withContext(Dispatchers.IO) {
    files.forEach {
      val (url, duration) = it
      val fileName = if (url.endsWith(".SRT")) {
        "subtitles.srt"
      } else {
        "video-${totalDuration.toString().padStart(4, '0')}" +
                "-${(totalDuration + duration).toString().padStart(4, '0')}.mpeg"
      }
      totalDuration += duration
      launch {
        val data = downloadBinary(url, client)
        val file = File("$outputFilePath/$fileName")
        file.writeBytes(data)
        k++
        progressCallback(k, files.size)
      }
    }
  }
}

suspend fun getVideoData(url: String, client: HttpClient): MediaData {
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
  return Klaxon().parse<MediaData>(rawData)
          ?: throw IllegalArgumentException("Invalid JSON")
}

suspend fun getSubtitles(url: String, client: HttpClient): SubtitleData {
  val id = url.substringAfterLast("/")
  val rawData = download("https://sub.toggle.sg/toggle_api/v1.0/apiService/getSubtitleFilesForMedia?mediaId=$id", client);
  return Klaxon().parse<SubtitleData>(rawData)
          ?: throw IllegalArgumentException("Invalid JSON")
}

fun parseApiCred(resp: String) =
        Regex(
                """apiUser:\s*"([^"]+?)".+?apiPass:\s*"([^"]+?)"""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        ).find(resp)