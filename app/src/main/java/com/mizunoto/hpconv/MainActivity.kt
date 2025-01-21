package com.mizunoto.hpconv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val codeTitle: TextView = findViewById(R.id.codeTitle)
        codeTitle.setOnClickListener {
            val codeUrl =
                "https://www.healthplanet.jp/oauth/auth.do?client_id=27569.kHm3xLBeVA.apps.healthplanet.jp&redirect_uri=https://www.healthplanet.jp/success.html&scope=innerscan&response_type=code"
            var https = ""

            if (!codeUrl.startsWith("http://") && !codeUrl.startsWith("https://")) {
                https = "https://"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(https + codeUrl)
            }

            startActivity(intent)
        }
    }
}