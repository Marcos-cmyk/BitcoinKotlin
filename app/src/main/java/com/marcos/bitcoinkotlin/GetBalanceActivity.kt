package com.marcos.bitcoinkotlin
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GetBalanceActivity : AppCompatActivity(){
    private var title: TextView? = null
    private var balance: TextView? = null
    private var address: EditText? = null
    private var getBalanceBtn: Button? = null
    private var mWebView: WebView? = null
    private var bitcoin: Bitcoin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.balance_layout)
        setupContent()
    }
    private fun setupContent() {
        title = findViewById(R.id.title)
        balance = findViewById(R.id.balance)
        address = findViewById(R.id.address)
        getBalanceBtn = findViewById(R.id.btn_getBalance)
        mWebView =  findViewById(R.id.webView)
        bitcoin = Bitcoin(this, _webView = mWebView!!)
        getBalanceBtn?.setOnClickListener{
            getBalance()
        }
    }
    private fun getBalance() {
        val onCompleted = {result : Boolean ->
            println("bitcoin setup Completed------->>>>>")
            getBTCBalance()
        }
        if (bitcoin?.isSuccess == false) {
            bitcoin?.setup(true,onCompleted = onCompleted)
        }  else  {
            getBTCBalance()
        }
    }
    @SuppressLint("SetTextI18n")
    private fun getBTCBalance() {
        val address = address?.text.toString()
        if (address.isNotEmpty()) {
            val onCompleted = {state : Boolean, amount: String,error: String ->
                this.runOnUiThread {
                    if (state) {
                        val  titleTip = "BTC Balance: "
                        balance?.text = titleTip + amount
                    } else {
                        balance?.text = error
                    }
                }
            }
            balance?.text = "fetching..."
            bitcoin?.getBTCBalance(address,onCompleted = onCompleted)
        }
    }

}