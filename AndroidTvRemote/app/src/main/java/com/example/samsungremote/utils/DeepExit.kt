package com.example.samsungremote.utils

fun performDeepExit(activity: android.app.Activity) {
    activity.finishAndRemoveTask()
}