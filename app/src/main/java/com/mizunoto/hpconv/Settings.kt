package com.mizunoto.hpconv

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

/**
 * A simple [Fragment] subclass.
 * Use the [Settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class Settings : Fragment() {

    private var args: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            args = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = view.context

        // API設定の取得
        val clientIdPrev = getString(context, Save.CLIENT_ID.key)
        val clientSecretPrev = getString(context, Save.CLIENT_SECRET.key)

        // API設定の表示
        val clientIdInput = requireView().findViewById<EditText>(R.id.clientIdInput)
        clientIdInput.setText(clientIdPrev)
        val clientSecretInput = requireView().findViewById<EditText>(R.id.clientSecretInput)
        clientSecretInput.setText(clientSecretPrev)

        // API設定の保存
        val saveSettings = requireView().findViewById<Button>(R.id.saveSettings)
        saveSettings.setOnClickListener {
            Log.d("Settings", "saveSettings clicked")

            // API設定の取得
            val clientIdNew = clientIdInput.text.toString()
            val clientSecretNew = clientSecretInput.text.toString()

            when {

                // API設定が未入力のとき
                clientIdNew == "" || clientSecretNew == "" -> {
                    showToast(context, "API設定が未入力です。", ToastLength.SHORT)
                    // Debug
                    saveAPISettings(context, "", "")
                    return@setOnClickListener
                }

                // API設定が変更されていないとき
                clientIdPrev == clientIdNew && clientSecretPrev == clientSecretNew -> {
                    showToast(context, "API設定は変更されていません。", ToastLength.SHORT)
                    return@setOnClickListener
                }

                // API設定が変更されているとき
                clientIdPrev != "" || clientSecretPrev != "" -> {
                    Log.d("Settings", "API設定が変更されています。")
                    val positiveListener = DialogInterface.OnClickListener { dialog, _ ->
                        saveAPISettings(context, clientIdNew, clientSecretNew)
                        dialog.dismiss()
                    }
                    val negativeListener = DialogInterface.OnClickListener { dialog, _ ->
                        clientIdInput.setText(clientIdPrev)
                        clientSecretInput.setText(clientSecretPrev)
                        dialog.dismiss()
                    }
                    val dialog = createDialog(
                        view.context,
                        "API設定の変更",
                        "API設定を変更しますか？",
                        "はい",
                        positiveListener,
                        "いいえ",
                        negativeListener
                    )
                    dialog.show()
                }

                // API初期設定(のはず)
                else -> {
                    saveAPISettings(context, clientIdNew, clientSecretNew)
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment Settings.
         */
        @JvmStatic
        fun newInstance() =
            Settings().apply {
                arguments = Bundle().apply {
                }
            }
    }

    private fun saveAPISettings(context: Context, clientId: String, clientSecret: String) {
        Log.d("Settings", "API設定を更新します。")
        saveValue(context, Save.CLIENT_ID.key, clientId)
        saveValue(context, Save.CLIENT_SECRET.key, clientSecret)
        createDialog(context, "API設定の保存", "API設定を保存しました。").show()
    }
}
