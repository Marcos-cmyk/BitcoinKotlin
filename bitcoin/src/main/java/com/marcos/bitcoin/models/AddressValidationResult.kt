package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * Address validation result data model
 * Used to receive address validation results from JS
 */
data class AddressValidationResult(
    val isValid: Boolean,
    val type: String,
    val network: String
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): AddressValidationResult? {
            return try {
                AddressValidationResult(
                    isValid = json.getBoolean("isValid"),
                    type = json.getString("type"),
                    network = json.getString("network")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get address type display name
     */
    val typeDisplayName: String
        get() = when (type) {
            "p2pkh" -> "P2PKH (Legacy)"
            "p2sh" -> "P2SH (Script Hash)"
            "p2wpkh" -> "P2WPKH (Segwit)"
            "p2wsh" -> "P2WSH (Segwit Script)"
            "p2tr" -> "P2TR (Taproot)"
            "legacy" -> "Legacy"
            "unknown" -> "Unknown Type"
            else -> type
        }
    
    /**
     * Get network type display name
     */
    val networkDisplayName: String
        get() = when (network) {
            "mainnet" -> "Mainnet"
            "testnet" -> "Testnet"
            "unknown" -> "Unknown Network"
            else -> network
        }
}
