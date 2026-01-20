package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * Wallet address information
 * Contains Legacy, Segwit, and Taproot addresses
 */
data class BitcoinAddresses(
    val legacy: String,
    val segwit: String,
    val taproot: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): BitcoinAddresses? {
            return try {
                BitcoinAddresses(
                    legacy = json.getString("legacy"),
                    segwit = json.getString("segwit"),
                    taproot = json.getString("taproot")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert to JSONObject
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("legacy", legacy)
            put("segwit", segwit)
            put("taproot", taproot)
        }
    }
}
