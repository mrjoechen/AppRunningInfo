package com.alpha.perfermanceinfo

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.Choreographer
import com.alpha.performanceinfo.Utils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by chenqiao on 1/13/21.
 * e-mail : mrjctech@gmail.com
 */
class PerformanceManager private constructor() {

    val TAG = "PerformanceManager"

    companion object {
        fun get() = Holder.holder
    }

    private object Holder {
        val holder = PerformanceManager()
    }


    private lateinit var mAms: ActivityManager
    private val mHandlerThread: HandlerThread by lazy {
        HandlerThread("Perfermance Thread")
    }
    private var mHandler: Handler? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var context: Context

    private var aboveAndroidO = false

    fun init(context: Context) {

        this.context = context
        mAms =
            context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        mHandlerThread.start()
        if (mHandler == null) {
            mHandler = Handler(mHandlerThread.looper)
        }

        aboveAndroidO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        startMonitorCpuInfo()
        startMonitorFpsInfo()
        startMonitorMemInfo()

    }


    private fun startMonitorCpuInfo() {

        val cpuRunnable = object : Runnable{
            override fun run() {
                val cpuDataForO = if (aboveAndroidO) Utils.getCpuDataForO() else Utils.getCPUData()
                Log.d(TAG, "cpu: " + cpuDataForO + "%")
                mHandler?.postDelayed(this, 1000)
            }
        }

        mHandler?.postDelayed(cpuRunnable, 1000)

    }

    private val fpsCallback = FpsCallback()
    private fun startMonitorFpsInfo() {
        val fpsRunnable = object : Runnable{
            override fun run() {
                Log.d(TAG, "fps : " + fpsCallback.fps)
                fpsCallback.reset()
                mainHandler?.postDelayed(this, 1000)
            }
        }
        Choreographer.getInstance().postFrameCallback(fpsCallback)
        mainHandler?.postDelayed(fpsRunnable, 1000)
    }

    private fun startMonitorMemInfo() {
        val memRunnable = object : Runnable{
            override fun run() {
                val mem = getMemoryData()
                Log.d(TAG, "mem :" + mem + "mb")

                mHandler?.postDelayed(this, 1000)
            }
        }

        mHandler?.postDelayed(memRunnable, 2000)
    }



    fun getMemoryData(): Float {
        var mem = 0.0f
        try {
            var memInfo: Debug.MemoryInfo? = null
            //28 为Android P
            if (Build.VERSION.SDK_INT > 28) {
                // 统计进程的内存信息 totalPss
                memInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memInfo)
            } else {
                //As of Android Q, for regular apps this method will only return information about the memory info for the processes running as the caller's uid;
                // no other process memory info is available and will be zero. Also of Android Q the sample rate allowed by this API is significantly limited, if called faster the limit you will receive the same data as the previous call.
                val memInfos: Array<Debug.MemoryInfo> = mAms.getProcessMemoryInfo(
                    intArrayOf(
                        Process.myPid()
                    )
                )
                if (memInfos != null && memInfos.size > 0) {
                    memInfo = memInfos[0]
                }
            }
            val totalPss = memInfo!!.totalPss
            if (totalPss >= 0) {
                // Mem in MB
                mem = totalPss / 1024.0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mem
    }


    class FpsCallback : Choreographer.FrameCallback {
        var fps = 0

        override fun doFrame(frameTimeNanos: Long) {

            fps++
            Choreographer.getInstance().postFrameCallback(this)
        }

        fun reset() {
            fps = 0
        }
    }

}