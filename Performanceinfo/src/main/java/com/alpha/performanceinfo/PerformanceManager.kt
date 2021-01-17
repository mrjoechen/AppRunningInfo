package com.alpha.perfermanceinfo

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.Choreographer
import com.alpha.performanceinfo.PerformanceInfoWindowUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by chenqiao on 1/13/21.
 * e-mail : mrjctech@gmail.com
 */

private const val TAG = "PerferenceManager"


const val WARNING_CPU = 15
const val WARNING_MEM = 100
const val WARNING_FPS = 50

const val RED_WARNING_CPU = 30
const val RED_WARNING_MEM = 200
const val RED_WARNING_FPS = 40

class PerformanceManager private constructor() : ActivityLifecycleCallbacks {

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


    private var cpu: Float = 0f
    private var mem: Float = 0f
    private var fps: Float = 0f


    fun init(context: Application) {

        this.context = context
        mAms =
            context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        mHandlerThread.start()
        if (mHandler == null) {
            mHandler = Handler(mHandlerThread.looper)
        }

        startMonitorCpuInfo()
        startMonitorFpsInfo()
        startMonitorMemInfo()
        startRefresh()

        context.registerActivityLifecycleCallbacks(this)

    }

    private fun startRefresh() {
        val refreshRunnable = object : Runnable{
            override fun run() {
                Log.d(TAG, "info [ " + "cpu: $cpu %, mem: $mem Mb, fps: $fps"+"]")
                performanceInfoWindowUtil?.refreshInfo(cpu, mem, fps)

                mainHandler.postDelayed(this, 1000)
            }
        }
        mainHandler.postDelayed(refreshRunnable, 1000)
    }

    private var performanceInfoWindowUtil: PerformanceInfoWindowUtil? = null

    private fun startMonitorCpuInfo() {

        val cpuRunnable = object : Runnable{
            override fun run() {
                val cpuDataForO = getCpuDataForO()
                cpu = cpuDataForO
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
                val f = fpsCallback.fps
                Log.d(TAG, "fps : " + f)
                fps = f.toFloat()
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
                val m = getMemoryData()
                mem = m
                Log.d(TAG, "mem :" + m + "mb")

                mHandler?.postDelayed(this, 1000)
            }
        }

        mHandler?.postDelayed(memRunnable, 2000)
    }



    private fun getMemoryData(): Float {
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


    /**
     * 8.0以上获取cpu的方式
     *
     * @return
     */
    private fun getCpuDataForO(): Float {
        var process: java.lang.Process? = null
        try {
            process = Runtime.getRuntime().exec("top -n 1")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String
            var cpuIndex = -1
            while (reader.readLine().also { line = it } != null) {
                line = line.trim { it <= ' ' }
                if (TextUtils.isEmpty(line)) {
                    continue
                }
                val tempIndex = getCPUIndex(line)
                if (tempIndex != -1) {
                    cpuIndex = tempIndex
                    continue
                }
                if (line.startsWith(Process.myPid().toString())) {
                    if (cpuIndex == -1) {
                        continue
                    }
                    val param = line.split("\\s+".toRegex()).toTypedArray()
                    if (param.size <= cpuIndex) {
                        continue
                    }
                    var cpu = param[cpuIndex]
                    if (cpu.endsWith("%")) {
                        cpu = cpu.substring(0, cpu.lastIndexOf("%"))
                    }
                    return cpu.toFloat() / Runtime.getRuntime().availableProcessors()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            process?.destroy()
        }
        return 0f
    }


    /**
     * 8.0一下获取cpu的方式
     *
     * @return
     */
    //    private float getCPUData() {
    //        long cpuTime;
    //        long appTime;
    //        float value = 0.0f;
    //        try {
    //            if (mProcStatFile == null || mAppStatFile == null) {
    //                mProcStatFile = new RandomAccessFile("/proc/stat", "r");
    //                mAppStatFile = new RandomAccessFile("/proc/" + android.os.Process.myPid() + "/stat", "r");
    //            } else {
    //                mProcStatFile.seek(0L);
    //                mAppStatFile.seek(0L);
    //            }
    //            String procStatString = mProcStatFile.readLine();
    //            String appStatString = mAppStatFile.readLine();
    //            String procStats[] = procStatString.split(" ");
    //            String appStats[] = appStatString.split(" ");
    //            cpuTime = Long.parseLong(procStats[2]) + Long.parseLong(procStats[3])
    //                    + Long.parseLong(procStats[4]) + Long.parseLong(procStats[5])
    //                    + Long.parseLong(procStats[6]) + Long.parseLong(procStats[7])
    //                    + Long.parseLong(procStats[8]);
    //            appTime = Long.parseLong(appStats[13]) + Long.parseLong(appStats[14]);
    //            if (mLastCpuTime == null && mLastAppCpuTime == null) {
    //                mLastCpuTime = cpuTime;
    //                mLastAppCpuTime = appTime;
    //                return value;
    //            }
    //            value = ((float) (appTime - mLastAppCpuTime) / (float) (cpuTime - mLastCpuTime)) * 100f;
    //            mLastCpuTime = cpuTime;
    //            mLastAppCpuTime = appTime;
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //        return value;
    //    }
    private fun getCPUIndex(line: String): Int {
        if (line.contains("CPU")) {
            val titles = line.split("\\s+".toRegex()).toTypedArray()
            for (i in titles.indices) {
                if (titles[i].contains("CPU")) {
                    return i
                }
            }
        }
        return -1
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

    override fun onActivityPaused(activity: Activity) {
        if (performanceInfoWindowUtil != null){
            performanceInfoWindowUtil?.hideAllView()
            performanceInfoWindowUtil = null
        }
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity) {
        if (performanceInfoWindowUtil != null){
            performanceInfoWindowUtil?.hideAllView()
            performanceInfoWindowUtil = null
        }
        performanceInfoWindowUtil =
            PerformanceInfoWindowUtil(activity)
        performanceInfoWindowUtil?.showContactView()
    }

}