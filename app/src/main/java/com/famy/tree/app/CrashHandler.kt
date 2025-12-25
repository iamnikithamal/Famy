package com.famy.tree.app

import android.app.Application
import android.content.Intent
import android.os.Process
import com.famy.tree.ui.activity.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val application: Application
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val stackTrace = getStackTraceString(throwable)
            val crashInfo = buildCrashInfo(thread, throwable, stackTrace)

            launchCrashActivity(crashInfo)

            Thread.sleep(200)
        } catch (e: Exception) {
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    private fun buildCrashInfo(thread: Thread, throwable: Throwable, stackTrace: String): String {
        return buildString {
            appendLine("═══════════════════════════════════════════")
            appendLine("CRASH REPORT")
            appendLine("═══════════════════════════════════════════")
            appendLine()
            appendLine("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine()
            appendLine("───────────────────────────────────────────")
            appendLine("EXCEPTION")
            appendLine("───────────────────────────────────────────")
            appendLine("Type: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message ?: "No message"}")
            appendLine()
            appendLine("───────────────────────────────────────────")
            appendLine("STACK TRACE")
            appendLine("───────────────────────────────────────────")
            appendLine(stackTrace)
            appendLine()
            appendLine("───────────────────────────────────────────")
            appendLine("DEVICE INFO")
            appendLine("───────────────────────────────────────────")
            appendLine("Brand: ${android.os.Build.BRAND}")
            appendLine("Model: ${android.os.Build.MODEL}")
            appendLine("Device: ${android.os.Build.DEVICE}")
            appendLine("SDK: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
            appendLine()
            appendLine("───────────────────────────────────────────")
            appendLine("APP INFO")
            appendLine("───────────────────────────────────────────")
            try {
                val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                appendLine("Package: ${application.packageName}")
                appendLine("Version: ${packageInfo.versionName}")
                @Suppress("DEPRECATION")
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }
                appendLine("Version Code: $versionCode")
            } catch (e: Exception) {
                appendLine("Package: ${application.packageName}")
                appendLine("Version: Unknown")
            }
            appendLine()
            appendLine("───────────────────────────────────────────")
            appendLine("MEMORY INFO")
            appendLine("───────────────────────────────────────────")
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024)
            val totalMemory = runtime.totalMemory() / (1024 * 1024)
            val freeMemory = runtime.freeMemory() / (1024 * 1024)
            val usedMemory = totalMemory - freeMemory
            appendLine("Max Memory: ${maxMemory}MB")
            appendLine("Total Memory: ${totalMemory}MB")
            appendLine("Used Memory: ${usedMemory}MB")
            appendLine("Free Memory: ${freeMemory}MB")
            appendLine()
            appendLine("═══════════════════════════════════════════")
        }
    }

    private fun launchCrashActivity(crashInfo: String) {
        val intent = Intent(application, CrashActivity::class.java).apply {
            putExtra(CrashActivity.EXTRA_CRASH_INFO, crashInfo)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        application.startActivity(intent)
    }

    companion object {
        fun install(application: Application) {
            val handler = CrashHandler(application)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }
}
