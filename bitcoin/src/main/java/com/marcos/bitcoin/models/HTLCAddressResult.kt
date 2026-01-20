package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * HTLC address generation result data model
 * Used to receive HTLC address generation results from JS
 */
data class HTLCAddressResult(
    val address: String,
    val redeemScript: String,
    val lockHeight: Int,
    val secretHex: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): HTLCAddressResult? {
            return try {
                HTLCAddressResult(
                    address = json.getString("address"),
                    redeemScript = json.getString("redeemScript"),
                    lockHeight = json.getInt("lockHeight"),
                    secretHex = json.getString("secretHex")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
