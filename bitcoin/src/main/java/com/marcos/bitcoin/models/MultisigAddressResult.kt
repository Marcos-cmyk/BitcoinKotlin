package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * Multisig address generation result data model (non-Taproot address)
 * Used to receive multisig address generation results from JS
 */
data class MultisigAddressResult(
    val script: String,
    val p2shAddress: String,
    val p2wshAddress: String,
    val threshold: Int,
    val totalSigners: Int
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): MultisigAddressResult? {
            return try {
                MultisigAddressResult(
                    script = json.getString("script"),
                    p2shAddress = json.getString("p2shAddress"),
                    p2wshAddress = json.getString("p2wshAddress"),
                    threshold = json.getInt("threshold"),
                    totalSigners = json.getInt("totalSigners")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
