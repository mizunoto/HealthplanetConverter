package com.mizunoto.hpconv

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A simple [Fragment] subclass.
 * Use the [Load.newInstance] factory method to
 * create an instance of this fragment.
 */
class Load : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_load, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = view.context
        val loadBtn = requireView().findViewById<Button>(R.id.loadBtn)

        loadBtn.setOnClickListener {
            val lastLoad = getLong(context, Save.LAST_LOAD.key)
            var from = Instant.now().minus(30, ChronoUnit.DAYS)
            if (lastLoad == 0L) {
                createDialog(
                    context,
                    "前回の記録がありません。",
                    "30日前からの記録を取得します。"
                ).show()
            } else {
                from = Instant.ofEpochMilli(lastLoad)
            }
            val data = loadAPI(context, from)
            Log.d("Load", data.toString());
            runBlocking {
                if (writeData(context, data)) {
                    showToast(context, "記録を更新しました。", ToastLength.SHORT)
                    saveValue(context, Save.LAST_LOAD.key, Instant.now().toEpochMilli())
                } else {
                    showToast(
                        context,
                        "更新する記録がありません。\n最終更新日:${from.atZone(ZoneId.systemDefault())}",
                        ToastLength.SHORT
                    )
                }
            }
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment load.
         */
        @JvmStatic
        fun newInstance() =
            Load().apply {
                arguments = Bundle().apply {
                }
            }
    }
}