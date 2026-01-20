package com.marcos.bitcoin.models

import org.json.JSONObject

/**
 * Fee rate information data model
 */
data class FeeRates(
    val high: Int,
    val medium: Int,
    val low: Int
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): FeeRates? {
            return try {
                FeeRates(
                    high = json.getInt("high"),
                    medium = json.getInt("medium"),
                    low = json.getInt("low")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Fee estimation result data model
 * Used to receive fee estimation results from JS
 */
data class FeeEstimateResult(
    val high: Long,
    val medium: Long,
    val low: Long,
    val size: Int,
    val rates: FeeRates
) {
    companion object {
        /**
         * Initialize from JSONObject (for parsing JS returned data)
         */
        fun fromJson(json: JSONObject): FeeEstimateResult? {
            return try {
                val ratesJson = json.getJSONObject("rates")
                val rates = FeeRates.fromJson(ratesJson) ?: return null
                
                FeeEstimateResult(
                    high = json.getLong("high"),
                    medium = json.getLong("medium"),
                    low = json.getLong("low"),
                    size = json.getInt("size"),
                    rates = rates
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * High fee rate (BTC)
     */
    val highInBTC: Double
        get() = high / 100_000_000.0
    
    /**
     * Medium fee rate (BTC)
     */
    val mediumInBTC: Double
        get() = medium / 100_000_000.0
    
    /**
     * Low fee rate (BTC)
     */
    val lowInBTC: Double
        get() = low / 100_000_000.0
}
