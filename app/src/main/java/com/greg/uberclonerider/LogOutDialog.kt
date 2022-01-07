package com.greg.uberclonerider

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.droidman.ktoasty.KToasty
import com.google.firebase.auth.FirebaseAuth
import com.greg.uberclonerider.ui.activity.SplashScreenActivity

class LogOutDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
            .setTitle(getString(R.string.log_out_title))
            .setMessage(getString(R.string.log_out_message))
            .setNegativeButton(getString(R.string.log_out_negative_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setPositiveButton(getString(R.string.log_out_positive_button)) { dialogInterface, _ ->
                logOut()
                dialogInterface.dismiss()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        }
        return dialog
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Log out --------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun logOut(){
        FirebaseAuth.getInstance().signOut()
        KToasty.success(requireContext(), getString(R.string.log_out_k_toasty), Toast.LENGTH_SHORT).show()
        goToSplashScreenActivity()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Splash screen activity -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToSplashScreenActivity() {
        startActivity(Intent(context, SplashScreenActivity::class.java))
    }
}