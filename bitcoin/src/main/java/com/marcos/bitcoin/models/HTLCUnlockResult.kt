package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * HTLC unlock and transfer result data model
 * Used to receive HTLC unlock results from JS
 */
data class HTLCUnlockResult(
    val txid: String,
    val signedHex: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): HTLCUnlockResult? {
            return try {
                HTLCUnlockResult(
                    txid = json.getString("txid"),
                    signedHex = json.getString("signedHex")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
