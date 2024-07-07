package com.marcos.bitcoinkotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EstimateFeeActivity : AppCompatActivity(){
    private var title: TextView? = null
    private var feeDetailEditText: EditText? = null
    private var estimateTransactionFeeBtn: Button? = null
    private var bitcoin: Bitcoin? = null
    private var mWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.estimate_fee)
        setupContent()
    }
    private fun setupContent(){
        feeDetailEditText = findViewById(R.id.fee_detail)
        title = findViewById(R.id.title)
        estimateTransactionFeeBtn = findViewById(R.id.btn_estimate_fee)
        mWebView = findViewById(R.id.webView)
        bitcoin = Bitcoin(context = this, _webView = mWebView!!)
        estimateTransactionFeeBtn?.setOnClickListener{
            estimateTransactionFee()
            }
        }
    private fun estimateTransactionFee(){
        val onCompleted = {result : Boolean ->
            estimateBTCTransferFee()
        }
        if (bitcoin?.isSuccess == false) {
            bitcoin?.setup(true,onCompleted)
        } else  {
            estimateBTCTransferFee()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun estimateBTCTransferFee() {
        val onCompleted = {state : Boolean,
                           high: Double,
                           medium:Double,
                           low:Double,
                           error:String ->
            this.runOnUiThread {
                println("Estimate fee finised.")
                if (state){
                    val highFormatted = String.format("%.2f", high)
                    val mediumFormatted = String.format("%.2f", medium)
                    val lowFormatted = String.format("%.2f", low)
                    val text = "Send BTC have three estimated fee. \n high: $highFormatted Satoshis. \n medium: $mediumFormatted Satoshis. \n low: $lowFormatted Satoshis"
                    feeDetailEditText?.setText(text)
                } else {
                    feeDetailEditText?.setText(error)
                }
            }
        }
        bitcoin?.estimateBtcTransferFee(1,2,onCompleted = onCompleted)
        println("Estimate fee start.")

    }
}