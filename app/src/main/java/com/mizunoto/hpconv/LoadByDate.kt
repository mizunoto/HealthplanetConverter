package com.mizunoto.hpconv

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import java.text.DecimalFormat
import java.time.Instant

/**
 * A simple [Fragment] subclass.
 * Use the [LoadByDate.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoadByDate : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_load_by_date, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fromInput: CalendarView = requireView().findViewById(R.id.fromInput)
        val toInput: CalendarView = requireView().findViewById(R.id.toInput)
        val loadByDateBtn: Button = requireView().findViewById(R.id.loadByDateBtn)

        fun swapDate(from: Instant, to: Instant) {
            fromInput.setDate(to.toEpochMilli(), true, true)
            fromInput.invalidate()
            toInput.setDate(from.toEpochMilli(), true, true)
            toInput.invalidate()
        }

        fromInput.setOnDateChangeListener { v, year, month, dayOfMonth ->
            val decimalFormat = DecimalFormat("00")
            v.date = Instant.parse(
                "${year}-${decimalFormat.format(month + 1)}-${decimalFormat.format(dayOfMonth)}T00:00:00Z"
            ).toEpochMilli()

            // 未来を指定した場合
            if (v.date > Instant.now().toEpochMilli()) {
                showToast(v.context, "未来を指定することはできません。", ToastLength.SHORT)
                v.setDate(Instant.now().toEpochMilli(), true, true)
                v.invalidate()
                return@setOnDateChangeListener
            }
            val from = Instant.ofEpochMilli(v.date)
            val to = Instant.ofEpochMilli(toInput.date)
            Log.d("LoadByDate", "fromDate:$from, toDate:$to")

            // fromDateがtoDateを超えた場合
            if (from > to) {
                Log.d("LoadByDate", "fromDateがtoDateを超えています。")
                swapDate(from, to)
                showToast(v.context, "開始が終了より後のため入れ替えました。", ToastLength.SHORT)
            }
        }

        toInput.setOnDateChangeListener { v, year, month, dayOfMonth ->
            val decimalFormat = DecimalFormat("00")
            v.date = Instant.parse(
                "${year}-${decimalFormat.format(month + 1)}-${decimalFormat.format(dayOfMonth)}T00:00:00Z"
            ).toEpochMilli()

            // 未来を指定した場合
            if (v.date > Instant.now().toEpochMilli()) {
                showToast(v.context, "未来を指定することはできません。", ToastLength.SHORT)
                v.setDate(Instant.now().toEpochMilli(), true, true)
                v.invalidate()
                return@setOnDateChangeListener
            }
            v.invalidate()
            val from = Instant.ofEpochMilli(fromInput.date)
            val to = Instant.ofEpochMilli(v.date)
            Log.d("LoadByDate", "fromDate:$from, toDate:$to")

            // fromDateがtoDateを超えた場合
            if (from > to) {
                Log.d("LoadByDate", "fromDateがtoDateを超えています。")
                swapDate(from, to)
                showToast(v.context, "終了が開始より先のため入れ替えました。", ToastLength.SHORT)
            }
        }

        loadByDateBtn.setOnClickListener {
            val context = view.context

            val from = Instant.ofEpochMilli(fromInput.date)
            val to = Instant.ofEpochMilli(toInput.date)
            Log.d("LoadByDate", "fromDate:$from, toDate:$to")

            val data = loadAPI(requireContext(), from, to)
            runBlocking {
                writeData(requireContext(), data)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment loadByDate.
         */
        @JvmStatic
        fun newInstance() =
            LoadByDate().apply {
                arguments = Bundle().apply {
                }
            }
    }
}