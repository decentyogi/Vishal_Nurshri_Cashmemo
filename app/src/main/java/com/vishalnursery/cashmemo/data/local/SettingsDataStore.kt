package com.vishalnursery.cashmemo.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "vishal_cashmemo_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SHOP_NAME_HINDI = stringPreferencesKey("shop_name_hindi")
        val SHOP_NAME_ENGLISH = stringPreferencesKey("shop_name_english")
        val TAGLINE = stringPreferencesKey("tagline")
        val PHONE = stringPreferencesKey("phone")
        val EMAIL = stringPreferencesKey("email")
        val ADDRESS_LINE1 = stringPreferencesKey("address_line_1")
        val ADDRESS_LINE2 = stringPreferencesKey("address_line_2")
        val GSTIN = stringPreferencesKey("gstin")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val MEMO_PREFIX = stringPreferencesKey("memo_prefix")
        val NEXT_MEMO_NUMBER = intPreferencesKey("next_memo_number")
        val DEFAULT_TAX_PCT = doublePreferencesKey("default_tax_pct")
        val ENABLE_TAX = booleanPreferencesKey("enable_tax")
        val TAX_TYPE = stringPreferencesKey("tax_type")
        val UPI_VPA = stringPreferencesKey("upi_vpa")
        val UPI_DEFAULT_NOTE = stringPreferencesKey("upi_default_note")
        val SIGNER_NAME = stringPreferencesKey("signer_name")
        val TERMS_NOTE = stringPreferencesKey("terms_note")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val TRANSLATION_MODE = stringPreferencesKey("translation_mode")
        val BILL_EXPORT_LAYOUT = stringPreferencesKey("bill_export_layout")
    }

    private val defaults = BusinessSettings()

    val settingsFlow: Flow<BusinessSettings> = context.settingsDataStore.data.map { prefs ->
        BusinessSettings(
            shopNameHindi = prefs[Keys.SHOP_NAME_HINDI] ?: defaults.shopNameHindi,
            shopNameEnglish = prefs[Keys.SHOP_NAME_ENGLISH] ?: defaults.shopNameEnglish,
            tagline = prefs[Keys.TAGLINE] ?: defaults.tagline,
            phone = prefs[Keys.PHONE] ?: defaults.phone,
            email = prefs[Keys.EMAIL] ?: defaults.email,
            addressLine1 = prefs[Keys.ADDRESS_LINE1] ?: defaults.addressLine1,
            addressLine2 = prefs[Keys.ADDRESS_LINE2] ?: defaults.addressLine2,
            gstin = prefs[Keys.GSTIN]?.trim()?.takeUnless { it.isBlank() || it.equals("NaN", ignoreCase = true) } ?: defaults.gstin,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: defaults.currencySymbol,
            memoPrefix = prefs[Keys.MEMO_PREFIX] ?: defaults.memoPrefix,
            nextMemoNumber = prefs[Keys.NEXT_MEMO_NUMBER] ?: defaults.nextMemoNumber,
            defaultTaxPercentage = prefs[Keys.DEFAULT_TAX_PCT] ?: defaults.defaultTaxPercentage,
            enableTax = prefs[Keys.ENABLE_TAX] ?: defaults.enableTax,
            taxType = prefs[Keys.TAX_TYPE] ?: defaults.taxType,
            upiVpa = prefs[Keys.UPI_VPA]?.trim() ?: defaults.upiVpa,
            upiDefaultNote = prefs[Keys.UPI_DEFAULT_NOTE] ?: defaults.upiDefaultNote,
            signerName = prefs[Keys.SIGNER_NAME] ?: defaults.signerName,
            termsNote = prefs[Keys.TERMS_NOTE]?.takeIf { it.isNotBlank() } ?: defaults.termsNote,
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL] ?: defaults.serverBaseUrl,
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: defaults.autoSyncEnabled,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: defaults.geminiApiKey,
            geminiModel = prefs[Keys.GEMINI_MODEL]?.takeIf { it.isNotBlank() } ?: defaults.geminiModel,
            translationMode = TranslationMode.fromWire(prefs[Keys.TRANSLATION_MODE]),
            billExportLayout = BillExportLayout.fromWire(prefs[Keys.BILL_EXPORT_LAYOUT])
        )
    }

    suspend fun getSettingsOnce(): BusinessSettings = settingsFlow.first()

    suspend fun saveSettings(settings: BusinessSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME_HINDI] = settings.shopNameHindi
            prefs[Keys.SHOP_NAME_ENGLISH] = settings.shopNameEnglish
            prefs[Keys.TAGLINE] = settings.tagline
            prefs[Keys.PHONE] = settings.phone
            prefs[Keys.EMAIL] = settings.email
            prefs[Keys.ADDRESS_LINE1] = settings.addressLine1
            prefs[Keys.ADDRESS_LINE2] = settings.addressLine2
            prefs[Keys.GSTIN] = settings.gstin.trim().takeUnless { it.equals("NaN", ignoreCase = true) } ?: ""
            prefs[Keys.CURRENCY_SYMBOL] = settings.currencySymbol.ifBlank { "₹" }
            prefs[Keys.MEMO_PREFIX] = settings.memoPrefix
            prefs[Keys.NEXT_MEMO_NUMBER] = settings.nextMemoNumber.coerceAtLeast(1)
            prefs[Keys.DEFAULT_TAX_PCT] = settings.defaultTaxPercentage
            prefs[Keys.ENABLE_TAX] = settings.enableTax
            prefs[Keys.TAX_TYPE] = settings.taxType
            prefs[Keys.UPI_VPA] = settings.upiVpa.trim()
            prefs[Keys.UPI_DEFAULT_NOTE] = settings.upiDefaultNote.trim()
            prefs[Keys.SIGNER_NAME] = settings.signerName
            prefs[Keys.TERMS_NOTE] = settings.termsNote.ifBlank { defaults.termsNote }
            prefs[Keys.SERVER_BASE_URL] = settings.serverBaseUrl.trim()
            prefs[Keys.AUTO_SYNC_ENABLED] = settings.autoSyncEnabled
            prefs[Keys.GEMINI_API_KEY] = settings.geminiApiKey.trim()
            prefs[Keys.GEMINI_MODEL] = settings.geminiModel.trim().ifBlank { defaults.geminiModel }
            prefs[Keys.TRANSLATION_MODE] = settings.translationMode.wireValue
            prefs[Keys.BILL_EXPORT_LAYOUT] = settings.billExportLayout.wireValue
        }
    }

    suspend fun incrementNextMemoIfNeeded(usedMemoNumber: String) {
        val digits = usedMemoNumber.filter { it.isDigit() }.toIntOrNull() ?: return
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.NEXT_MEMO_NUMBER] ?: defaults.nextMemoNumber
            if (digits >= current) {
                prefs[Keys.NEXT_MEMO_NUMBER] = digits + 1
            }
        }
    }
}
