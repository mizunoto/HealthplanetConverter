package com.mizunoto.hpconv

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * A simple [Fragment] subclass.
 * Use the [GetToken.newInstance] factory method to
 * create an instance of this fragment.
 */
class GetToken : Fragment() {

    private var args: Bundle? = null
    private var accessingToken = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            args = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_get_token, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = view.context

        val clientId = getString(context, Save.CLIENT_ID.key)
        val clientSecret = getString(context, Save.CLIENT_SECRET.key)

        // code取得ボタンの設定
        val codeIntentBtn = requireView().findViewById<Button>(R.id.codeIntentBtn)
        codeIntentBtn.setOnClickListener {
            val url = makeUri(
                "/oauth/auth", mapOf(
                    "client_id" to clientId,
                    "redirect_uri" to REDIRECT_URI,
                    "scope" to "innerscan",
                    "response_type" to "code"
                )
            )
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            return@setOnClickListener
        }

        // code入力欄の設定
        val codeInput = requireView().findViewById<EditText>(R.id.codeInput)
        codeInput.setOnClickListener {
            val clipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboardManager.hasPrimaryClip()) {
                val data = clipboardManager.primaryClip
                if (data != null && data.itemCount > 0) {
                    val item = data.getItemAt(0)
                    codeInput.setText(item.text.toString())
                }
            }
            return@setOnClickListener
        }

        // token取得ボタンの設定
        val getTokenBtn = requireView().findViewById<Button>(R.id.getTokenBtn)
        getTokenBtn.setOnClickListener {
            if (accessingToken) {
                createDialog(
                    context, "token取得エラー", "tokenの取得中です。\n少々お待ちください。"
                ).show()
                return@setOnClickListener
            }
            val code = codeInput.text.toString()
            // codeが未入力のとき
            if (code == "") {
                createDialog(context, "code取得エラー", "codeを入力してください。").show()
                return@setOnClickListener
            }
            // token取得処理
            val url = makeUri(
                "/oauth/token", mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "redirect_uri" to REDIRECT_URI,
                    "code" to code,
                    "grant_type" to "authorization_code"
                )
            )
            lateinit var response: String
            val getTokenThread = Thread {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Length", "0")
                try {
                    accessingToken = true
                    connection.connect()
                    val responseCode = connection.responseCode
                    Log.d("token取得", responseCode.toString())
                    if (responseCode == 200) {
                        response = connection.inputStream.bufferedReader().readText()
                        Log.d("token取得", response)
                    }
                } catch (e: IOException) {
                    Log.d("token取得", "エラーが発生しました。$e")
                } finally {
                    connection.disconnect()
                    accessingToken = false
                }
            }
            getTokenThread.start()
            getTokenThread.join()

            val json = JSONObject(response)
            if (json.has("error")) {
                Log.d("token取得", "エラーが発生しました。")
                createDialog(
                    context,
                    "token取得エラー",
                    "エラーが発生しました。\nコードの再取得を試してみてください。"
                ).show()
            } else {
                val token = json.getString("access_token")
                val token_expires_in = json.getString("expires_in")
                val refresh_token = json.getString("refresh_token")
                Log.d(
                    "token取得",
                    "token: $token, token_expires_in: $token_expires_in, refresh_token: $refresh_token"
                )
                saveValue(context, Save.TOKEN.key, token)
                saveValue(
                    context,
                    Save.TOKEN_EXPIRES_IN.key,
                    Instant.parse(Instant.now().toString()).plusSeconds(token_expires_in.toLong())
                        .toEpochMilli()
                )
                saveValue(context, Save.REFRESH_TOKEN.key, refresh_token)
                createDialog(
                    context,
                    "token取得成功",
                    "tokenを取得しました。",
                    "OK"
                ).show()
                return@setOnClickListener
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment GetToken.
         */
        @JvmStatic
        fun newInstance() = GetToken().apply {
            arguments = Bundle().apply {}
        }
    }
}