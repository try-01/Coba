package com.samsungremote

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppLogger private constructor(private val logFile: File) {

    private val writer: FileWriter = FileWriter(logFile, true)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun d(tag: String, message: String) = log("DEBUG", tag, message)
    fun i(tag: String, message: String) = log("INFO", tag, message)
    fun w(tag: String, message: String) = log("WARN", tag, message)
    fun e(tag: String, message: String) = log("ERROR", tag, message)

    @Synchronized
    private fun log(level: String, tag: String, message: String) {
        try {
            val ts = dateFormat.format(Date())
            writer.write("[$ts] [$level] [$tag] $message\n")
            writer.flush()
        } catch (_: IOException) { }
    }

    @Synchronized
    fun close() {
        try {
            writer.flush()
            writer.close()
        } catch (_: IOException) { }
    }

    companion object {
        private const val LOG_DIR = "logs"
        private const val MAX_SESSION_LOGS = 10

        fun create(context: Context): AppLogger {
            val dir = File(context.filesDir, LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            trimOldLogs(dir)
            val fileName = "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.log"
            val file = File(dir, fileName)
            file.createNewFile()
            return AppLogger(file)
        }

        private fun trimOldLogs(dir: File) {
            val files = dir.listFiles { f -> f.name.startsWith("session_") && f.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            if (files.size > MAX_SESSION_LOGS) {
                files.drop(MAX_SESSION_LOGS).forEach { it.delete() }
            }
        }
    }
}
