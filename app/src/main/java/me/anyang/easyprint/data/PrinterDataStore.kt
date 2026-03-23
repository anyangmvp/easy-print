package me.anyang.easyprint.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.printerDataStore: DataStore<Preferences> by preferencesDataStore(name = "printers")

@Serializable
data class SavedPrinter(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val type: String,
    val addedTime: Long = System.currentTimeMillis()
)

class PrinterDataStore(private val context: Context) {
    private val printersKey = stringPreferencesKey("saved_printers")

    val savedPrinters: Flow<List<SavedPrinter>> = context.printerDataStore.data
        .map { preferences ->
            val jsonString = preferences[printersKey] ?: "[]"
            try {
                Json.decodeFromString<List<SavedPrinter>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun savePrinter(printer: SavedPrinter) {
        context.printerDataStore.edit { preferences ->
            val currentList = try {
                val jsonString = preferences[printersKey] ?: "[]"
                Json.decodeFromString<List<SavedPrinter>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // 如果已存在相同ID的打印机，先移除
            currentList.removeAll { it.id == printer.id }
            // 添加到列表开头
            currentList.add(0, printer)

            preferences[printersKey] = Json.encodeToString(currentList)
        }
    }

    suspend fun deletePrinter(printerId: String) {
        context.printerDataStore.edit { preferences ->
            val currentList = try {
                val jsonString = preferences[printersKey] ?: "[]"
                Json.decodeFromString<List<SavedPrinter>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.removeAll { it.id == printerId }
            preferences[printersKey] = Json.encodeToString(currentList)
        }
    }

    suspend fun updatePrinterIp(printerId: String, newIp: String) {
        context.printerDataStore.edit { preferences ->
            val currentList = try {
                val jsonString = preferences[printersKey] ?: "[]"
                Json.decodeFromString<List<SavedPrinter>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            val index = currentList.indexOfFirst { it.id == printerId }
            if (index != -1) {
                val oldPrinter = currentList[index]
                currentList[index] = oldPrinter.copy(
                    id = "$newIp:${oldPrinter.port}",
                    ip = newIp,
                    name = "HP/Samsung Printer ($newIp)"
                )
                preferences[printersKey] = Json.encodeToString(currentList)
            }
        }
    }

    suspend fun clearAllPrinters() {
        context.printerDataStore.edit { preferences ->
            preferences[printersKey] = "[]"
        }
    }
}