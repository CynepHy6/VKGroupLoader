package ru.work.hy6.vkgrouploader

import android.app.Application
import com.vk.sdk.VKSdk

// Created by andrey on 15.09.15.

public class VKGUapplication: Application() {
    override fun onCreate() {
        super.onCreate()
        VKSdk.initialize(this)
    }
}