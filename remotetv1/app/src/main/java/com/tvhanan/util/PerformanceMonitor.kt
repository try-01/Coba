package com.tvhanan.util

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import kotlin.system.measureNanoTime

/**
 * Utility for measuring and logging performance metrics
 */
object PerformanceMonitor {
    private const val TAG = "PerfMonitor"

    /**
     * Measures execution time of a block and logs it
     * @param tag Custom tag for the log entry
     * @param block Code block to measure
     * @param block Code block to measure
     * @return Result of the block
     */
    @MainThread
    inline fun <T> measureMain(
        tag: String = TAG,
        crossinline block: () -> T
    ): T {
        val start = System.nanoTime()
        val result = block()
        val durationMs = (System.nanoTime() - start) / 1_000_000
        Log.d(tag, "Main thread operation took ${durationMs}ms")
        return result
    }

    @WorkerThread
    inline fun <T> measureWorker(
        tag: String = TAG,
        crossinline block: () -> T
    ): T {
        val start = System.nanoTime()
        val result = block()
        val durationMs = (System.nanoTime() - start) / 1_000_000
        Log.d(tag, "Worker thread operation took ${durationMs}ms")
        return result
    }

    /**
     * Measures frame timing for UI operations
     * @param frameId Identifier for the frame being measured
     * @param block UI operation to measure
     */
    @MainThread
    fun measureFrame(frameId: String, crossinline block: () -> Unit) {
        val start = System.nanoTime()
        block()
        val durationMs = (System.nanoTime() - start) / 1_000_000
        if (durationMs > 16) { // > 16ms = potential frame drop (60fps)
            Log.w(TAG, "Frame $frameId took ${durationMs}ms (exceeds 16ms budget)")
        } else {
            Log.d(TAG, "Frame $frameId took ${durationMs}ms")
        }
    }
}