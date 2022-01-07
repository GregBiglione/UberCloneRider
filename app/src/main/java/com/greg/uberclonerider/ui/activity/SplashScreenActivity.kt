package com.greg.uberclonerider.ui.activity

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.droidman.ktoasty.KToasty
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.greg.uberclonerider.utils.Common
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_INFORMATION
import com.greg.uberclonerider.R
import com.greg.uberclonerider.model.Rider
import com.greg.uberclonerider.databinding.SpashProgressBarBinding
import com.greg.uberclonerider.utils.UserUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: SpashProgressBarBinding
    //----------------------- Firebase -------------------------------------------------------------
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var auth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private var currentUser: FirebaseUser? = null
    private lateinit var authMethodPickerLayout: AuthMethodPickerLayout
    //----------------------- Firebase database ----------------------------------------------------
    private lateinit var database: FirebaseDatabase
    private lateinit var riderInformationReference: DatabaseReference
    private lateinit var rider: Rider
    //----------------------- Registration dialog --------------------------------------------------
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SpashProgressBarBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        initializeLogin()
    }

    override fun onStart() {
        super.onStart()
        delayedSplashScreen()
    }

    override fun onStop() {
        removeListenerOnFirebaseAuth()
        super.onStop()
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Firebase ---------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize firebase --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun firebaseAuth(){
        auth = FirebaseAuth.getInstance()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Get current user -----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getCurrentUser(): FirebaseUser? {
        currentUser = auth.currentUser
        return currentUser
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Timer ----------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun delayedSplashScreen(){
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                addListenerOnFirebaseAuth()
            }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Add listener on firebase auth ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addListenerOnFirebaseAuth(){
        auth.addAuthStateListener(listener)
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Remove listener on firebase auth -------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeListenerOnFirebaseAuth(){
        if (auth != null && listener != null){
            auth.removeAuthStateListener(listener)
        }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize login -----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeLogin(){
        firebaseDatabase()
        providers = listOf(
                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
        )
        firebaseAuth()
        initializeListener()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize listener --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeListener() {
        listener = FirebaseAuth.AuthStateListener {
            if (getCurrentUser() != null){
                updateToken()
                checkUserFromFirebase()
            }
            else{
                showLoginLayout()
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Show login layout ----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showLoginLayout() {
        authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.activity_splash_screen)
                .setPhoneButtonId(R.id.phone_btn)
                .setGoogleButtonId(R.id.google_btn)
                .build()
        createCustomAuthentication()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Create custom authentication -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun createCustomAuthentication(){
        val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
        launcher.launch(signInIntent)
    }

    //----------------------- Launcher method because startActivityForResult is deprecated ---------

    private val launcher = registerForActivityResult(FirebaseAuthUIActivityResultContract()){ result ->
        onSignInResult(result)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            Log.d(ContentValues.TAG, "$response, Welcome ${getCurrentUser()}")
            KToasty.success(this, "Connection successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Log.w(ContentValues.TAG, "signInWithCredential:failure", response!!.error)
            KToasty.error(this, "Authentication failed", Toast.LENGTH_SHORT).show()
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Firebase database ------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize firebase database -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun firebaseDatabase(){
        database = Firebase.database
        initializeRider()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize driver ----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeRider(){
        riderInformationReference = database.getReference(RIDER_INFORMATION)
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Check user from Firebase ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun checkUserFromFirebase() {
        riderInformationReference
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()){
                            val currentRider = snapshot.getValue(Rider::class.java)
                            goToHomeActivity(currentRider)
                        }
                        else{
                            showRegisterLayout()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        KToasty.error(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Show register layout -------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showRegisterLayout() {
        builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)

        val firstNameEt =  itemView.findViewById<View>(R.id.first_name_et) as TextInputEditText
        val lastNameEt =  itemView.findViewById<View>(R.id.last_name_et) as TextInputEditText
        val phoneNumberEt =  itemView.findViewById<View>(R.id.phone_number_et) as TextInputEditText
        val registerBtn =  itemView.findViewById<View>(R.id.register_btn) as Button

        if (getCurrentUser()!!.phoneNumber != null && !TextUtils.isDigitsOnly(getCurrentUser()!!.phoneNumber)){
            phoneNumberEt.setText(getCurrentUser()!!.phoneNumber)
        }

        builder.setView(itemView)
        dialog = builder.create()
        dialog.show()

        registerBtn.setOnClickListener {
            when {
                TextUtils.isDigitsOnly(firstNameEt.text.toString()) -> {
                    firstNameEt.error = getString(R.string.first_name_error)
                }
                TextUtils.isDigitsOnly(lastNameEt.text.toString()) -> {
                    lastNameEt.error = getString(R.string.last_name_error)
                }
                TextUtils.isDigitsOnly(phoneNumberEt.text.toString()) -> {
                    phoneNumberEt.error = getString(R.string.phone_number_error)
                }
                else -> {
                    val firstName = firstNameEt.text.toString()
                    val lastName = lastNameEt.text.toString()
                    val phoneNumber = phoneNumberEt.text.toString()

                    rider = Rider(null, firstName, lastName, phoneNumber)
                    checkRegistration()
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Check Registration ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun checkRegistration() {
        Log.d("Path Fire:", auth.currentUser!!.uid)
        riderInformationReference.child(auth.currentUser!!.uid)
                .setValue(rider)
                .addOnSuccessListener {
                    KToasty.success(this, "Registration successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    goToHomeActivity(rider)
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    KToasty.error(this, "$e.message", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    binding.progressBar.visibility = View.GONE
                }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Home activity --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToHomeActivity(currentRider: Rider?) {
        Common.currentRider = currentRider
        startActivity(Intent(this, HomeActivity::class.java))
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Firebase cloud messaging -----------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Update token ---------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun updateToken(){
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }
                val token = task.result
                UserUtils.updateToken(this, token)

                val msg = "Token value: $token"
                Log.d(ContentValues.TAG, msg)
            })
            .addOnFailureListener { e ->
                KToasty.error(this, e.message!! , Toast.LENGTH_LONG).show()
            }
    }
}