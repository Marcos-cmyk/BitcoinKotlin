package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * One-click transfer result data model
 * Used to receive transfer results from JS
 */
data class OneClickTransferResult(
    val txid: String,
    val signedHex: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): OneClickTransferResult? {
            return try {
                OneClickTransferResult(
                    txid = json.getString("txid"),
                    signedHex = json.getString("signedHex")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
