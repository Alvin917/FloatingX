package com.petterp.floatingx.impl.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.petterp.floatingx.assist.helper.AppHelper
import com.petterp.floatingx.impl.control.FxAppControlImpl
import com.petterp.floatingx.util.decorView
import java.lang.ref.WeakReference

/**
 * App-lifecycle
 */
class FxLifecycleCallbackImpl(
    private val helper: AppHelper
) :
    Application.ActivityLifecycleCallbacks {
    internal var appControl: FxAppControlImpl? = null
    internal var topActivity: WeakReference<Activity>? = null

    private val Activity.isParent: Boolean
        get() = appControl?.getManagerView()?.parent === decorView
    private val Activity.name: String
        get() = javaClass.name.split(".").last()
    private val Activity.isActivityInValid: Boolean
        get() = helper.blackList.contains(this::class.java)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        helper.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityCreated")
        helper.fxLifecycleExpand?.onActivityCreated?.let {
            if (activity.isActivityInValid) it.invoke(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        initActivity(activity)
        helper.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityStarted")
        helper.fxLifecycleExpand?.onActivityStarted?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    /**
     * 最开始想到在onActivityPostCreated后插入,
     * 但是最后发现在Android9及以下,此方法不会被调用,故选择了onResume
     * */
    override fun onActivityResumed(activity: Activity) {
        initActivity(activity)
        helper.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityResumed")
        if (!helper.enableFx) {
            helper.fxLog?.d("view->isAttach? -enableFx-${helper.enableFx}")
            return
        }
        val isActivityInValid = activity.isActivityInValid
        if (!isActivityInValid) {
            helper.fxLog?.d("view->isAttach? -isActivityInValid-$isActivityInValid")
            return
        }
        val isParent = activity.isParent
        if (isParent) {
            helper.fxLog?.d("view->isAttach? -isParent-$isParent")
            return
        }
        helper.fxLog?.d("view->isAttach? isContainActivity-$isActivityInValid--enableFx-${helper.enableFx}---isParent-$isParent")
        appControl?.attach(activity)
        helper.fxLifecycleExpand?.onActivityResumed?.let {
            if (isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        helper.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityPaused")
        helper.fxLifecycleExpand?.onActivityPaused?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        helper.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityStopped")
        helper.fxLifecycleExpand?.onActivityStopped?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        helper.fxLifecycleExpand?.onActivityDestroyed?.let {
            if (activity.isActivityInValid) it.invoke(activity)
        }
        val isParent = activity.isParent
        helper.fxLog?.d("AppLifecycle--[${activity.name}]-onActivityDestroyed")
        helper.fxLog?.d("view->isDetach? isContainActivity-${activity.isActivityInValid}--enableFx-${helper.enableFx}---isParent-$isParent")
        if (helper.enableFx && isParent)
            appControl?.detach(activity)
        if (topActivity?.get() === activity) {
            topActivity?.clear()
            topActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        helper.fxLifecycleExpand?.onActivitySaveInstanceState?.let {
            if (activity.isActivityInValid) it.invoke(activity, outState)
        }
    }

    private fun initActivity(activity: Activity) {
        if (topActivity?.get() != activity)
            topActivity = WeakReference(activity)
    }
}
