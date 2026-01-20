package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * Batch transfer result data model
 * Used to receive batch transfer results from JS
 */
data class BatchTransferResult(
    val txid: String,
    val signedHex: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): BatchTransferResult? {
            return try {
                BatchTransferResult(
                    txid = json.getString("txid"),
                    signedHex = json.getString("signedHex")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
