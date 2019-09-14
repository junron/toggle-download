package main.util

data class SubtitleData(
        val mediaId: String,
        val subtitleFiles: List<SubtitleFile>
)

data class SubtitleFile(val subtitleFileLanguage: String, val subtitleFileUrl: String)