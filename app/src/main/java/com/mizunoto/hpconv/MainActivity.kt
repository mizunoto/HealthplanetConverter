package com.mizunoto.hpconv

import android.health.connect.HealthPermissions
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.Instant


class MainActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val context = this
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        val permissions = setOf(
            HealthPermissions.WRITE_WEIGHT, HealthPermissions.WRITE_BODY_FAT
        )
        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                result.entries.forEach {
                    Log.d("requestPermissions", "${it.key} : ${it.value}")
                }
            }
        requestPermissions.launch(permissions.toTypedArray())

        val btmMenu = findViewById<BottomNavigationView>(R.id.btmMenu)
        val navController = findNavController(R.id.navHostMain)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.loadFragment,
                R.id.loadByDateFragment,
                R.id.getTokenFragment,
                R.id.settingsFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        btmMenu.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val clientId = getString(this, Save.CLIENT_ID.key)
            val clientSecret = getString(this, Save.CLIENT_SECRET.key)
            val tokenExpiresIn = getLong(this, Save.TOKEN_EXPIRES_IN.key)
            Log.d("tokenの期限", Instant.ofEpochMilli(tokenExpiresIn).toString())

            // API設定が未設定のとき
            if ((clientId == "" || clientSecret == "") && destination.id != R.id.settingsFragment) {
                createDialog(
                    this,
                    "API設定が設定されていません。",
                    "API設定を完了してから再度お試しください。",
                    "OK", null, null, null,
                    { _ ->
                        if (destination.id == R.id.loadFragment)
                            disableLoad()
                        if (destination.id == R.id.loadByDateFragment)
                            disableLoadByDate()
                        if (destination.id == R.id.getTokenFragment)
                            disableGetToken()
                    }
                ).show()
            }

            // tokenの期限が切れているとき
            else if (
                System.currentTimeMillis() > tokenExpiresIn &&
                destination.id != R.id.settingsFragment &&
                destination.id != R.id.getTokenFragment
            ) {
                // tokenの更新を促す処理
                createDialog(
                    this,
                    "tokenの期限が切れています。",
                    "tokenの期限が切れています。\n再度取得してください。",
                    "OK", null, null, null, { _ ->
                        if (destination.id == R.id.loadFragment)
                            disableLoad()
                        if (destination.id == R.id.loadByDateFragment)
                            disableLoadByDate()
                    }
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            Log.d(
                "onRequestPermissionsResult",
                "requestCode:$requestCode, permissions:$permissions, grantResults:$grantResults"
            )
        }
    }

    private fun disableLoad() {
        val loadBtn = findViewById<Button>(R.id.loadBtn)
        if (loadBtn != null) loadBtn.isEnabled = false
    }

    private fun disableLoadByDate() {
        val loadByDateBtn = findViewById<Button>(R.id.loadByDateBtn)
        if (loadByDateBtn != null) loadByDateBtn.isEnabled = false
    }

    private fun disableGetToken() {
        val codeIntentBtn = findViewById<Button>(R.id.codeIntentBtn)
        if (codeIntentBtn != null) codeIntentBtn.isEnabled = false
        val codeInput = findViewById<EditText>(R.id.codeInput)
        if (codeInput != null) codeInput.isEnabled = false
        val getTokenBtn = findViewById<Button>(R.id.getTokenBtn)
        if (getTokenBtn != null) getTokenBtn.isEnabled = false
    }
}