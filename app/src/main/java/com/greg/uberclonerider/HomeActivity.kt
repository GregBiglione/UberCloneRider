package com.greg.uberclonerider

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navController: NavController
    private lateinit var headerView: View
    private lateinit var photo: CircleImageView
    //----------------------- Firebase storage -----------------------------------------------------
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val navigationHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navigationHost.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_home), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        setDriverInformation()
        clickOnNavItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Click on nav item -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnNavItem(){
        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_home){
                goToHomeActivity()
            }
            if (it.itemId == R.id.nav_log_out){
                showLogOutDialog()
            }
            true
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Log out dialog box ------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showLogOutDialog() {
        val logOutDialog = LogOutDialog()
        logOutDialog.show(supportFragmentManager, "LogOutDialogBox")
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Set rider information ---------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun setDriverInformation(){
        storageReference = FirebaseStorage.getInstance().reference
        headerView = navView.getHeaderView(0)
        val name = headerView.findViewById<View>(R.id.name_tv) as TextView
        val phoneNumber = headerView.findViewById<View>(R.id.phone_tv) as TextView
        //val rating = headerView.findViewById<View>(R.id.rating_tv) as TextView
        photo = headerView.findViewById<View>(R.id.photo) as CircleImageView

        name.text = Common.buildWelcomeMessage()
        phoneNumber.text = Common.currentRider!!.phoneNumber
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Home activity -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
    }
}