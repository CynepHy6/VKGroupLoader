package ru.work.hy6.vkgrouploader

import android.app.Application
import android.content.res.Configuration
import com.vk.sdk.VKSdk

// Created by andrey on 15.09.15.
var isJustStarted = true
val DEBUG = true

public class VKGUapplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VKSdk.initialize(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        isJustStarted = false
        super.onConfigurationChanged(newConfig)
    }
}