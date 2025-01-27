package com.mizunoto.hpconv

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import com.mizunoto.hpconv.ToastLength.LONG
import com.mizunoto.hpconv.ToastLength.SHORT
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

const val DOMAIN = "www.healthplanet.jp"
const val REDIRECT_URI = "https://www.healthplanet.jp/success.html"

enum class Save(val key: String) {
    CLIENT_ID("clientId"),
    CLIENT_SECRET("clientSecret"),
    TOKEN("token"),
    REFRESH_TOKEN("refresh_token"),
    TOKEN_EXPIRES_IN("token_expires_in"),
    LAST_LOAD("lastLoad")
}

/**
 * トーストの長さ
 * [Toast.LENGTH_SHORT]と[Toast.LENGTH_LONG]の代用
 * @property SHORT 短い
 * @property LONG 長い
 */
enum class ToastLength {
    SHORT,
    LONG
}

/**
 * トーストを表示する
 * @param context Context
 * @param text 表示するテキスト
 * @param length トーストの長さ
 */
fun showToast(
    context: Context,
    text: String,
    length: ToastLength
) {
    Log.d("showToast", "context:$context, text:$text, length:$length")
    val toastLength: Int =
        if (length == SHORT) {
            Toast.LENGTH_SHORT
        } else {
            Toast.LENGTH_LONG
        }
    Toast.makeText(context, text, toastLength).show()
}

fun createDialog(
    context: Context,
    title: String,
    message: String,
    positiveButton: String = "OK",
    positiveListener: DialogInterface.OnClickListener? = null,
    negativeButton: String? = null,
    negativeListener: DialogInterface.OnClickListener? = null,
    dismissListener: DialogInterface.OnDismissListener? = null
): AlertDialog {
    Log.w("createDialog", "context:$context, title:$title, message:$message")
    val builder = AlertDialog.Builder(context)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(positiveButton, positiveListener)
    if (negativeListener != null) builder.setNegativeButton(negativeButton, negativeListener)
    if (dismissListener != null) builder.setOnDismissListener(dismissListener)
    val dialog = builder.create()
    return dialog
}

/**
 * 文字列の保存
 * @param context Context
 * @param key キー
 * @param value 値
 */
fun saveValue(context: Context, key: String, value: Any) {
    val sharedPref = context.getSharedPreferences("HPConv", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            else -> throw IllegalArgumentException("Unsupported type")
        }
        apply()
    }
}

/**
 * 文字列の取得
 * @param context Context
 * @param key キー
 * @return 値
 */
fun getString(context: Context, key: String): String {
    val sharedPref = context.getSharedPreferences("HPConv", Context.MODE_PRIVATE)
    return sharedPref.getString(key, "").toString()
}

/**
 * Intの取得
 * @param context Context
 * @param key キー
 * @return 値
 */
fun getInt(context: Context, key: String): Int {
    val sharedPref = context.getSharedPreferences("HPConv", Context.MODE_PRIVATE)
    return sharedPref.getInt(key, 0)
}

/**
 * Longの取得
 * @param context Context
 * @param key キー
 * @return 値
 */
fun getLong(context: Context, key: String): Long {
    val sharedPref = context.getSharedPreferences("HPConv", Context.MODE_PRIVATE)
    return sharedPref.getLong(key, 0L)
}

/**
 * APIの呼び出し
 * @param context Context
 * @param from 開始日
 * @param to 終了日
 * @return APIのレスポンス
 */
fun loadAPI(context: Context, from: Instant, to: Instant = Instant.now()): JSONArray {
    val token = getToken(context)
    val tokenExpiresIn = getLong(context, Save.TOKEN_EXPIRES_IN.key)
    if (tokenExpiresIn < Instant.now().toEpochMilli()) {
        createDialog(
            context,
            "tokenの期限が切れています。",
            "tokenを再取得してください。"
        ).show()
    }
    val dataArray = JSONArray()

    val thread = Thread {
        var fromEpochMilli = from.toEpochMilli()
        var toEpochMilli: Long
        // 60日間隔で処理を行う
        do {
            toEpochMilli = fromEpochMilli + 90L * 24 * 60 * 60 * 1000
            if (toEpochMilli > to.toEpochMilli()) toEpochMilli = to.toEpochMilli()
            val url = makeUri(
                "status/innerscan.json", mapOf(
                    "access_token" to token,
                    "date" to "1",
                    "from" to parseQueryDate(fromEpochMilli),
                    "to" to parseQueryDate(toEpochMilli),
                    "tag" to "6021,6022"
                )
            )
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            try {
                connection.connect()
                val responseCode = connection.responseCode
                Log.d("LoadByDate", responseCode.toString())
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d("LoadByDate", response)
                    val json = JSONObject(response)
                    for (i in 0 until json.getJSONArray("data").length()) {
                        val data = json.getJSONArray("data").getJSONObject(i)
                        dataArray.put(data)
                    }
                }
            } catch (e: IOException) {
                Log.d("LoadByDate", "通信に失敗しました。$e")
            } catch (e: Exception) {
                Log.d("LoadByDate", "エラーが発生しました。$e")
            } finally {
                connection.disconnect()
            }
            fromEpochMilli = toEpochMilli
        } while (to.toEpochMilli() - fromEpochMilli > 0L)
    }
    thread.start()
    thread.join()
    return dataArray
}

/**
 * データの書き込み
 * @param context Context
 * @param dataJSONArray 書き込むデータ
 * @return 書き込み内容の有無
 */
suspend fun writeData(context: Context, dataJSONArray: JSONArray): Boolean {
    Log.d("writeData", "dataJSONArray:$dataJSONArray")
    val client = HealthConnectClient.getOrCreate(context)
    val weightList = mutableListOf<WeightRecord>()
    val bodyFatList = mutableListOf<BodyFatRecord>()
    for (i in 0 until dataJSONArray.length()) {
        val date = parseDataDateToInstant(dataJSONArray.getJSONObject(i).getString("date"))
        if (dataJSONArray.getJSONObject(i).getString("tag") == "6021") {
            val weightData = dataJSONArray.getJSONObject(i).getString("keydata").toDouble()
            val record = WeightRecord(date, ZoneOffset.of("+09:00"), Mass.kilograms(weightData))
            weightList.add(record)
        } else if (dataJSONArray.getJSONObject(i).getString("tag") == "6022") {
            val bodyFatData = dataJSONArray.getJSONObject(i).getString("keydata").toDouble()
            val record = BodyFatRecord(date, ZoneOffset.of("+09:00"), Percentage(bodyFatData))
            bodyFatList.add(record)
        }
    }
    val insertWeight = client.insertRecords(weightList.toList())
    val insertBodyFat = client.insertRecords(bodyFatList.toList())
    Log.d("writeData", "insertWeight:${insertWeight.recordIdsList}")
    Log.d("writeData", "insertBodyFat:${insertBodyFat.recordIdsList}")
    return insertWeight.recordIdsList.isNotEmpty() || insertBodyFat.recordIdsList.isNotEmpty()
}

/**
 * Uriの生成
 * @param path パス
 * @param query クエリ
 * @return Uri
 */
fun makeUri(path: String, query: Map<String, String>): String {
    val rtn = Uri.Builder().scheme("https").authority(DOMAIN).path(path).apply {
        query.forEach { (key, value) ->
            appendQueryParameter(key, value)
        }
    }.build().toString()
    return rtn
}

/**
 * Instantをクエリに対応するようにフォーマット
 * @param instant Instant
 * @return フォーマットされた日付
 */
fun parseQueryDate(instant: Instant): String {
    val decimalFormat = DecimalFormat("00")
    val zoned = instant.atZone(ZoneId.systemDefault())
    var rtn = ""
    rtn += decimalFormat.format(zoned.year)
    rtn += decimalFormat.format(zoned.monthValue)
    rtn += decimalFormat.format(zoned.dayOfMonth)
    rtn += decimalFormat.format(zoned.hour)
    rtn += decimalFormat.format(zoned.minute)
    rtn += decimalFormat.format(zoned.second)
    return rtn
}

/**
 * EpochMilliをクエリに対応するようにフォーマット
 * @param epochMilli EpochMilli
 * @return フォーマットされた日付
 */
fun parseQueryDate(epochMilli: Long): String {
    val instant = Instant.ofEpochMilli(epochMilli)
    return parseQueryDate(instant)
}

/**
 * Dataの日付をInstantに変換
 * @param dataDate Dataの日付
 * @return Instant
 */
fun parseDataDateToInstant(dataDate: String): Instant {
    val year = dataDate.substring(0, 4)
    val month = dataDate.substring(4, 6)
    val day = dataDate.substring(6, 8)
    val hour = dataDate.substring(8, 10)
    val minute = dataDate.substring(10, 12)
    return Instant.parse("${year}-${month}-${day}T${hour}:${minute}:00+09:00")
}

/**
 * tokenの有効期限を確認して有効であればtokenを返す
 * @param context Context
 * @return token or null
 */
fun getToken(context: Context): String {
    val tokenExpiresIn = getLong(context, Save.TOKEN_EXPIRES_IN.key)
    return if (tokenExpiresIn < Instant.now().toEpochMilli()) {
        getString(context, Save.REFRESH_TOKEN.key)
    } else getString(context, Save.TOKEN.key)
}