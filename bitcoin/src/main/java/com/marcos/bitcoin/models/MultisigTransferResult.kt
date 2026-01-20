package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * Multisig transfer result data model
 * Used to receive multisig transfer results from JS
 */
data class MultisigTransferResult(
    val txid: String,
    val signedHex: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): MultisigTransferResult? {
            return try {
                MultisigTransferResult(
                    txid = json.getString("txid"),
                    signedHex = json.getString("signedHex")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
