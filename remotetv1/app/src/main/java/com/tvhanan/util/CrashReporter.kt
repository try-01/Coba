package com.tvhanan.util

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val TAG = "TvHananCrash"
    private const val PREFS_NAME = "crash_reporter_prefs"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val KEY_LAST_CRASH_TIME = "last_crash_time"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    fun initialize(context: Context) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(context, thread, throwable)
            // Pass to default handler after our processing
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                System.exit(2)
            }
        }
    }
    
    private fun handleUncaughtException(
        context: Context,
        thread: Thread,
        throwable: Throwable
    ) {
        try {
            val crashReport = generateCrashReport(context, thread, throwable)
            saveCrashReport(context, crashReport)
            Log.e(TAG, "FATAL EXCEPTION: ${thread.name}\n$crashReport")
            
            // Optionally send to remote server
            // sendCrashReportToServer(crashReport)
        } catch (e: Exception) {
            Log.e(TAG, "Error while reporting crash", e)
        }
    }
    
    private fun generateCrashReport(
        context: Context,
        thread: Thread,
        throwable: Throwable
    ): String {
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        val stackTrace = writer.toString()
        printWriter.close()
        
        val deviceInfo = StringBuilder()
        deviceInfo.append("Device: ${android.os.Build.MANUFACTURER} ")
        deviceInfo.append("${android.os.Build.MODEL} (${android.os.Build.PRODUCT})\n")
        deviceInfo.append("Version: ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})\n")
        deviceInfo.append("App Version: ${getAppVersion(context)} (${getAppVersionName(context)})\n")
        deviceInfo.append("Timestamp: ${dateFormat.format(Date())}\n")
        deviceInfo.append("Thread: ${thread.name} (${thread.id})\n")
        deviceInfo.append("Stack Trace:\n$stackTrace")
        
        return deviceInfo.toString()
    }
    
    private fun saveCrashReport(context: Context, report: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        
        val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
        edit.putInt(KEY_CRASH_COUNT, crashCount)
        edit.putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
        
        // Keep last 5 crash reports
        val existingReports = prefs.getStringSet("crash_reports", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        existingReports.add("${System.currentTimeMillis()}:$report")
        if (existingReports.size > 5) {
            // Remove oldest
            val oldest = existingReports.minBy { it.split(":")[0].toLong() }
            existingReports.remove(oldest)
        }
        edit.putStringSet("crash_reports", existingReports)
        edit.apply()
    }
    
    private fun getAppVersion(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getAppVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
    }
}