package util

import java.io.IOException
import java.time.LocalTime

fun checkFFMPEG(): Boolean {
  return try {
    ProcessBuilder().command("ffmpeg", "-version").start().waitFor() == 0
  } catch (e: IOException) {
    println(e.message)
    false
  }
}

fun secondsToString(seconds: Int) = LocalTime.ofSecondOfDay(seconds.toLong()).toString()
