package ai.voicedoc.app.metrics

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process

data class DeviceMemorySnapshot(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val processPssMb: Int,
    val nativeHeapMb: Long,
    val lowMemory: Boolean
)

/** Local, opt-in diagnostics; no health text/audio or telemetry is transmitted. */
object LocalDeviceMetrics {
    fun snapshot(context: Context): DeviceMemorySnapshot {
        val info = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(info)
        val processes = activityManager.getProcessMemoryInfo(intArrayOf(Process.myPid()))
        val pssKb = processes.firstOrNull()?.totalPss ?: 0
        return DeviceMemorySnapshot(
            totalRamMb = info.totalMem / (1024 * 1024),
            availableRamMb = info.availMem / (1024 * 1024),
            processPssMb = pssKb / 1024,
            nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024),
            lowMemory = info.lowMemory
        )
    }
}
