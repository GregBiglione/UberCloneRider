package com.greg.uberclonerider.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.greg.uberclonerider.R
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_INFORMATION

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
}