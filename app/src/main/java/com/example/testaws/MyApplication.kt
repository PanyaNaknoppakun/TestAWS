package com.example.testaws

import android.app.Application

import com.androidnetworking.AndroidNetworking

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidNetworking.initialize(getApplicationContext());

    }
}