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

class TransferActivity : AppCompatActivity(){
    private var title: TextView? = null
    private var hashValue: TextView? = null
    private var privateKeyEditText: EditText? = null
    private var receiveEditText: EditText? = null
    private var amountEditText: EditText? = null
    private var transferBtn: Button? = null
    private var detailBtn: Button? = null
    private var bitcoin: Bitcoin? = null
    private var mWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transfer_layout)
        setupContent()
    }
    private fun setupContent(){
        privateKeyEditText = findViewById(R.id.private_key)
        receiveEditText = findViewById(R.id.receive_address)
        amountEditText = findViewById(R.id.amount)
        title = findViewById(R.id.title)
        hashValue = findViewById(R.id.hashValue)
        transferBtn = findViewById(R.id.btn_transfer)
        detailBtn = findViewById(R.id.btn_detail)
        mWebView = findViewById(R.id.webView)
        bitcoin = Bitcoin(context = this, _webView = mWebView!!)
        transferBtn?.setOnClickListener{
            transfer()
        }
        detailBtn?.setOnClickListener{
            val hash = hashValue?.text.toString()
            if (hash.length < 20) { return@setOnClickListener}
            val urlString = "https://blockchair.com/bitcoin/transaction/$hash"
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(urlString))
                startActivity(intent)
            } catch (e: Exception) {
                println("The current phone does not have a browser installed")
            }
        }
    }

    private fun transfer(){
        val onCompleted = {result : Boolean ->
            btcTransfer()
        }
        if (bitcoin?.isSuccess == false) {
            bitcoin?.setup(true,onCompleted)
        } else  {
            btcTransfer()
        }
    }
    private fun btcTransfer() {
        val privateKey = privateKeyEditText?.text.toString()
        val toAddress = receiveEditText?.text.toString()
        val amount = amountEditText?.text.toString()

        val outputs: MutableList<HashMap<String, String>> = mutableListOf(
            hashMapOf("address" to toAddress, "amount" to "0.00001")
        )
        outputs.add(hashMapOf("address" to "secondAddress", "amount" to "0.00001"))

        println("Support a Bitcoin address sending to multiple Bitcoin addresses simultaneously.")

        if (toAddress.isNotEmpty() && amount.isNotEmpty() && privateKey.isNotEmpty()) {
            val onCompleted = {state : Boolean, hash: String,error:String ->
                println("btcTransfer Finished.")
                this.runOnUiThread {
                    if (state){
                        hashValue?.text = hash
                    } else {
                        hashValue?.text = error
                    }
                }
            }
            val fee = 2000.0
            bitcoin?.transfer(privateKey,outputs,fee,onCompleted = onCompleted)
            println("btcTransfer start.")
        }
    }
}