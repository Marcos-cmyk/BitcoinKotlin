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
//import com.smithsophiav.web3kotlin.ETHMainNet
//import com.smithsophiav.web3kotlin.ETHWeb

class EstimateFeeActivity : AppCompatActivity(){
    private var title: TextView? = null
    private var hashValue: TextView? = null
    private var privateKeyEditText: EditText? = null
    private var receiveEditText: EditText? = null
    private var amountEditText: EditText? = null
    private var erc20TokenEditText: EditText? = null
    private var transferBtn: Button? = null
    private var detailBtn: Button? = null
    private var estimateTransactionFeeBtn: Button? = null
//    private var web3: ETHWeb? = null
    private var mWebView: WebView? = null
    private var type: String = ""
    private var chainType: String = "main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_layout)
        setupContent()
    }
    private fun setupContent(){
        erc20TokenEditText  = findViewById(R.id.erc20Token)
        privateKeyEditText = findViewById(R.id.private_key)
        receiveEditText = findViewById(R.id.receive_address)
        amountEditText = findViewById(R.id.amount)
        title = findViewById(R.id.title)
        hashValue = findViewById(R.id.hashValue)
        transferBtn = findViewById(R.id.btn_transfer)
        estimateTransactionFeeBtn = findViewById(R.id.btn_estimateTransactionFee)
        detailBtn = findViewById(R.id.btn_detail)
        mWebView = findViewById(R.id.webView)
//        web3 = ETHWeb(context = this, _webView = mWebView!!)
        estimateTransactionFeeBtn?.setOnClickListener{

        }
        }
    }
//    private fun estimateTransactionFee(){
//        val onCompleted = {result : Boolean ->
//            estimateBTCTransferFee()
//        }
//        if (web3?.isWeb3LoadFinished == false) {
//            web3?.setup(true,onCompleted)
//        } else  {
//            estimateBTCTransferFee()
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun estimateBTCTransferFee() {
//        val toAddress = receiveEditText?.text.toString()
//        val amount = amountEditText?.text.toString()
//        val senderAddress = "0x6648Ee1bc5a10856D72b197cC9bA23B7002AA8F1"
//        if (toAddress.isNotEmpty() && amount.isNotEmpty()) {
//            val onCompleted = {state : Boolean,
//                               estimateETHTransactionFee: String,
//                               gasEstimate:String,
//                               gasPrice:String,
//                               error:String ->
//                this.runOnUiThread {
//                    if (state){
//                        hashValue?.text = estimateETHTransactionFee + "ETH"
//                    } else {
//                        hashValue?.text = error
//                    }
//                }
//            }
//            val providerUrl = if(chainType == "main") ETHMainNet else "https://sepolia.infura.io/v3/fe816c09404d406f8f47af0b78413806"
//
//            web3?.estimateETHTransactionFee(toAddress,senderAddress,amount,providerUrl,onCompleted = onCompleted)
//        }
//    }
//}