package com.greg.uberclonerider.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.droidman.ktoasty.KToasty
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.greg.uberclonerider.R
import com.greg.uberclonerider.model.Token
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_INFORMATION
import com.greg.uberclonerider.utils.Constant.Companion.TOKEN

object UserUtils {

    private var currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid

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
}