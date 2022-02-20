package com.greg.uberclonerider.utils

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import com.droidman.ktoasty.KToasty
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.greg.uberclonerider.R
import com.greg.uberclonerider.event.SelectedPlaceEvent
import com.greg.uberclonerider.model.DriverGeolocation
import com.greg.uberclonerider.model.FCMSendData
import com.greg.uberclonerider.model.Token
import com.greg.uberclonerider.remote.FCMService
import com.greg.uberclonerider.utils.Constant.Companion.DESTINATION_LOCATION
import com.greg.uberclonerider.utils.Constant.Companion.DESTINATION_LOCATION_STRING
import com.greg.uberclonerider.utils.Constant.Companion.NOTIFICATION_BODY
import com.greg.uberclonerider.utils.Constant.Companion.NOTIFICATION_TITLE
import com.greg.uberclonerider.utils.Constant.Companion.PICKUP_LOCATION
import com.greg.uberclonerider.utils.Constant.Companion.PICKUP_LOCATION_STRING
import com.greg.uberclonerider.utils.Constant.Companion.REQUEST_DRIVER_BODY
import com.greg.uberclonerider.utils.Constant.Companion.REQUEST_DRIVER_TITLE
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_INFORMATION
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_KEY
import com.greg.uberclonerider.utils.Constant.Companion.TOKEN
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

object UserUtils {

    private var currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Update driver -----------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun updateDriver(view: View?, updateAvatar: Map<String, Any>){
        FirebaseDatabase.getInstance()
            .getReference(RIDER_INFORMATION)
            .child(currentUserUid)
            .updateChildren(updateAvatar)
            .addOnSuccessListener {
                Snackbar.make(view!!, R.string.information_update_success, Snackbar.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(view!!, e.message!!, Snackbar.LENGTH_LONG).show()
            }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Update token ------------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun updateToken(context: Context, token: String) {
        val currentToken = Token()
        currentToken.token = token

        FirebaseDatabase.getInstance().getReference(TOKEN)
            .child(currentUserUid)
            .setValue(token)
            .addOnSuccessListener {}
            .addOnFailureListener { e ->
                KToasty.error(context, e.message!!, Toast.LENGTH_LONG).show()
            }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Send request to driver --------------------------------------
    //----------------------------------------------------------------------------------------------

    fun sendRequestToDriver(context: Context, mainLayout: RelativeLayout?, foundDriver: DriverGeolocation?, selectedPlaceEvent: SelectedPlaceEvent) {
        val compositeDisposable = CompositeDisposable()
        val iFcmService = FCMService.getInstance()

        FirebaseDatabase.getInstance().getReference(TOKEN)
                .child(foundDriver!!.key!!)
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val token = snapshot.getValue(true)

                            val notificationData: MutableMap<String, String> = HashMap()

                            notificationData[NOTIFICATION_TITLE] = REQUEST_DRIVER_TITLE
                            notificationData[NOTIFICATION_BODY] = REQUEST_DRIVER_BODY
                            notificationData[PICKUP_LOCATION_STRING] = selectedPlaceEvent.originString
                            notificationData[PICKUP_LOCATION] = Common.buildPickUpLocation(selectedPlaceEvent)
                            notificationData[RIDER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid

                            notificationData[DESTINATION_LOCATION_STRING] = selectedPlaceEvent.destinationString
                            notificationData[DESTINATION_LOCATION] = Common.buildPickUpDestination(selectedPlaceEvent)
                            notificationData[RIDER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid

                            val fcmSendData = FCMSendData(token.toString(), notificationData)

                            compositeDisposable.add(iFcmService.sendNotification(fcmSendData)
                            !!.subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ fcmResponse ->
                                        if (fcmResponse!!.success == 0){
                                            compositeDisposable.clear()
                                            Snackbar.make(mainLayout!!, context.getString(R.string.send_request_driver_failed),
                                                    Snackbar.LENGTH_LONG).show()
                                        }
                                    },
                                    { t: Throwable ->
                                        compositeDisposable.clear()
                                        Snackbar.make(mainLayout!!, t.message!!, Snackbar.LENGTH_LONG).show()
                                        Log.e(TAG, t.message!!)
                                        KToasty.error(context, t.message!!, Toast.LENGTH_LONG).show()
                                    })
                            )
                        }
                        else{
                            Snackbar.make(mainLayout!!, context.getString(R.string.token_not_found), Snackbar.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(mainLayout!!, error.message, Snackbar.LENGTH_LONG).show()
                        Log.e(TAG, error.message)
                        KToasty.error(context, error.message, Toast.LENGTH_LONG).show()
                    }
                })
    }
}