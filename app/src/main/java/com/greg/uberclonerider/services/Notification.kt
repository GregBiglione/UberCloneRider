package com.greg.uberclonerider.services

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.greg.uberclonerider.event.AcceptedRequestEventFromDriver
import com.greg.uberclonerider.event.DeclineRequestEventFromDriver
import com.greg.uberclonerider.ui.activity.HomeActivity
import com.greg.uberclonerider.utils.Common
import com.greg.uberclonerider.utils.Constant.Companion.NOTIFICATION_BODY
import com.greg.uberclonerider.utils.Constant.Companion.NOTIFICATION_TITLE
import com.greg.uberclonerider.utils.Constant.Companion.REQUEST_DRIVER_ACCEPT
import com.greg.uberclonerider.utils.Constant.Companion.REQUEST_DRIVER_DECLINE
import com.greg.uberclonerider.utils.Constant.Companion.TRIP_KEY
import com.greg.uberclonerider.utils.UserUtils
import org.greenrobot.eventbus.EventBus
import java.util.*

class Notification: FirebaseMessagingService() {

    private var currentUser = FirebaseAuth.getInstance().currentUser

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (currentUser != null){
            UserUtils.updateToken(this, token)
            Log.d("Token in new token", token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if (data != null){
            if (data[NOTIFICATION_TITLE] != null) {
                val intent = Intent(this, HomeActivity::class.java)
                if (data[NOTIFICATION_TITLE].equals(REQUEST_DRIVER_DECLINE)) {
                    EventBus.getDefault().postSticky(DeclineRequestEventFromDriver())
                    Log.e("Test msg received", data[NOTIFICATION_TITLE].toString())
                }
                else if (data[NOTIFICATION_TITLE].equals(REQUEST_DRIVER_ACCEPT)) {
                    EventBus.getDefault().postSticky(AcceptedRequestEventFromDriver(data[TRIP_KEY]!!))
                }
                else {
                    Common.showNotification(this, Random().nextInt(), NOTIFICATION_TITLE, NOTIFICATION_BODY, intent)
                }
            }
        }
    }
}