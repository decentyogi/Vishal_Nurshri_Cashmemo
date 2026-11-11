package com.vishalnursery.cashmemo

import android.app.Application
import com.vishalnursery.cashmemo.data.local.CashmemoDatabase
import com.vishalnursery.cashmemo.data.local.SettingsDataStore
import com.vishalnursery.cashmemo.data.ocr.GeminiVisionService
import com.vishalnursery.cashmemo.data.ocr.OcrScannerService
import com.vishalnursery.cashmemo.data.repository.CashmemoRepository

class CashmemoApplication : Application() {

    val database by lazy { CashmemoDatabase.getInstance(this) }
    val settingsDataStore by lazy { SettingsDataStore(this) }
    val repository by lazy { CashmemoRepository(database, settingsDataStore) }
    val ocrService by lazy { OcrScannerService(this) }
    val geminiVisionService by lazy { GeminiVisionService(this) }
}
