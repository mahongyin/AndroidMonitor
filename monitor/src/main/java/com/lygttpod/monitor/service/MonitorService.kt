package com.lygttpod.monitor.service

import com.android.local.service.annotation.Request
import com.android.local.service.annotation.Page
import com.android.local.service.annotation.ServicePort
import com.lygttpod.monitor.MonitorHelper
import com.lygttpod.monitor.data.MonitorData
import com.lygttpod.monitor.enum.SPValueType
import com.lygttpod.monitor.mock.MockHelper
import com.lygttpod.monitor.utils.ServiceDataProvider
import com.lygttpod.monitor.utils.lastUpdateDataId
import com.lygttpod.monitor.weaknetwork.WeakNetworkHelper

@ServicePort(port = 9527)
abstract class MonitorService {

    @Page("index")
    fun showMonitorPage() = "monitor_index.html"

    @Page("sp_index")
    fun showSpPage() = "sp_index.html"

    @Page("mqtt_index")
    fun showMqttPage() = "mqtt_index.html"

    @Request("query")
    fun queryMonitorData(limit: Int, offset: Int): MutableList<MonitorData> {
        return ServiceDataProvider.getMonitorDataList(limit, offset)
    }

    @Request("clean")
    fun cleanMonitorData() {
        MonitorHelper.deleteAll()
    }

    @Request("autoFetch")
    fun autoFetchData(lastFetchId: Long): MutableList<MonitorData> {
        while (true) {
            Thread.sleep(1000)
            return if (lastFetchId != lastUpdateDataId || lastFetchId == 0L) {
                ServiceDataProvider.getMonitorDataByLastId(lastFetchId)
            } else {
                mutableListOf()
            }
        }
    }

    @Request("sharedPrefs")
    fun getSharedPrefsFilesData() = MonitorHelper.getSharedPrefsFilesData()

    @Request("getSharedPrefsByFileName")
    fun getSharedPrefsByFileName(fileName: String) = MonitorHelper.getSpFile(fileName)

    @Request("updateSpValue")
    fun updateSpValue(fileName: String, key: String, value: String, valueType: String) {
        val realValue = when (valueType) {
            SPValueType.Int.value -> value.toIntOrNull()
            SPValueType.Double.value -> value.toDoubleOrNull()
            SPValueType.Long.value -> value.toLongOrNull()
            SPValueType.Float.value -> value.toFloatOrNull()
            SPValueType.Boolean.value -> value.toBoolean()
            else -> value
        }
        MonitorHelper.updateSpValue(fileName, key, realValue)
    }

    @Request("setWeakNetConfig")
    fun configWeak(weakType: String) {
        WeakNetworkHelper.configWeak(weakType)
    }

    @Request("setMockConfig")
    fun setMockConfig(mockBaseUrl: String, mockPath: String, mockResponse: String) {
        MockHelper.configMock(mockBaseUrl, mockPath, mockResponse)
    }

    @Request("openMockService")
    fun openMockService(isOpen: Boolean) {
        MockHelper.isOpen = isOpen
    }
}