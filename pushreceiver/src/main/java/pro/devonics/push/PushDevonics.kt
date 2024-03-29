package pro.devonics.push

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pro.devonics.push.DataHelper.Companion.createTransition
import pro.devonics.push.DataHelper.Companion.startTime
import pro.devonics.push.model.PushData
import pro.devonics.push.model.TimeData
import pro.devonics.push.network.ApiHelper
import pro.devonics.push.network.RetrofitBuilder
import java.util.*


private const val TAG = "PushDevonics"
private const val PERMISSIONS_REQUEST_CODE = 2

class PushDevonics(activity: Activity, appId: String) {

    private val service = ApiHelper(RetrofitBuilder.apiService)
    private val myContext = activity
    private val helperCache = HelperCache(activity)

    init {
        AppContextKeeper.setContext(activity)
        PushInitialization.run(appId)
        createInternalId()
        startTime()
        startSession()
        sendTransition()
        askNotificationPermission()
    }

    private fun askNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(myContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "askNotificationPermission: PERMISSION_GRANTED")

                // FCM SDK (and your app) can post notifications.
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    myContext,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                Log.v(TAG, "askNotificationPermission: ")
            } else {
                myContext.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSIONS_REQUEST_CODE
                )
            }

        }
    }

    private fun sendTransition() {

        val sentPushId = helperCache.getSentPushId()
        Log.d(TAG, "sendTransition: sentPushId = $sentPushId")
        if (sentPushId == "" || sentPushId == null) {
            return
        }
        val pushData = PushData(sentPushId)
        createTransition(pushData)
        Log.d(TAG, "sendTransition: pushData = $pushData")

        helperCache.saveSentPushId("")

    }

    /*fun sendIntent(intent: Intent) {

        if ("transition" == intent.getStringExtra("command")) {

            val bundle = intent.extras
            val sentPushId = bundle?.get("sent_push_id").toString()
            val pushData = PushData(sentPushId)
            createTransition(pushData)
            //Log.d(TAG, "sendIntent: pushData = $pushData")
            //Log.d(TAG, "pushType: $pushType")
            //Log.d(TAG, "pushId: $pushId")
        }
    }*/

    fun openUrl() {
        val openUrl = helperCache.getOpenUrl()
        if (openUrl == "") {
            return
        }
        Log.d(TAG, "openUrl: openUrl = $openUrl")

        if (openUrl != null) {
            val urlIntent = Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(openUrl))

            urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                myContext.startActivity(urlIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "ActivityNotFoundException $e")
            }
        }
        helperCache.saveOpenUrl("")
    }

    fun getDeeplink(): String {
        val deep1 = helperCache.getDeeplink()
        Log.d(TAG, "getDeeplink: deep1 = $deep1")
        helperCache.saveDeeplink("")
        return deep1.toString()
    }

    private fun createInternalId() {
        val pushCache = PushCache()

        var internalId = pushCache.getInternalIdFromPref()
        if (internalId == null) {
            val uuid = UUID.randomUUID()
            internalId = uuid.toString()
            pushCache.saveInternalId(internalId)

        }
        //Log.d(TAG, "createInternalId(): internalId = $internalId")
    }

    fun getInternalId(): String? {
        val pushCache = PushCache()
        return pushCache.getInternalIdFromPref()
    }

    fun startSession() {
        //Log.d(TAG, "startSession: ")
        val pushCache = PushCache()
        val registrationId = pushCache.getRegistrationIdFromPref()

        if (pushCache.getSubscribeStatusFromPref() == true) {
            registrationId?.let { service.createSession(it) }
            //Log.d(TAG, "subscribeStatus = ${pushCache.getSubscribeStatusFromPref()}")

        }
    }

    fun stopSession() {
        val duration = DataHelper.getDuration()
        val pushCache = PushCache()
        val regId = pushCache.getRegistrationIdFromPref()
        if (regId != null) {
            val timeData = TimeData(duration)
            service.sendTimeStatistic(regId, timeData)
            //Log.d(TAG, "stopSession: timeData $timeData")
        }

        //Log.d(TAG, "stopSession: duration $duration")
        //Log.d(TAG, "stopSession: regId $regId")

        //Log.d(TAG, "stopSession")
    }

    fun setTags(key: String, value: String) {
        val pushCache = PushCache()
        if (key == null && value == null) {
            pushCache.saveTagKey("")
            pushCache.saveTagValue("")
        } else {
            pushCache.saveTagKey(key)
            pushCache.saveTagValue(value)
        }

        //Log.d(TAG, "setTags: $key : $value")
    }


}