package com.alpha.apprunninginfo

import android.app.Application
import com.didichuxing.doraemonkit.DoraemonKit
import com.didichuxing.doraemonkit.kit.AbstractKit
import com.didichuxing.doraemonkit.kit.parameter.cpu.CpuKit


class App: Application() {

    override fun onCreate() {
        super.onCreate()
//        val kits: MutableList<AbstractKit> = ArrayList()
//        kits.add(CpuKit())
        DoraemonKit.install(this, "f55c2239680ed641a5ab0caf38fb190b")
    }

}