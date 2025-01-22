package com.mizunoto.hpconv

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord

abstract class HPConvHealthConnect(context: Context) {
    private val healthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun record(recordList: List<WeightRecord>) {
        healthConnectClient.insertRecords(recordList)
    }
}
