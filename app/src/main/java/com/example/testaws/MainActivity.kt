package com.example.testaws

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class MainActivity : Activity()  {
    var accessKeyId = "ASIAD212FDFDCB846FDF"
    var secretAccessKey= "MIglJhVFx2ZMQrnxe1H6XBpEV7dYMqvN4tj-3kAZJ-s"
    var sessionToken = "CgNkZXYSBXVzZXIxGhRBUk9BQzUyRDRDNjMxOEI1QUM3MSIbdXJuOmVjczppYW06OmRldjpyb2xlL3VzZXIxKhRBU0lBRDIxMkZERkRDQjg0NkZERjJQTWFzdGVyS2V5UmVjb3JkLTc1YzgyODZhZTFiYjkyYjcxZTRiY2QyZDgxNWFkM2VmY2RjMzllMThkNzhhZTk3ZWYxODBiODA2ZWQ2MTljMzE45LT2hZowUgV1c2VyMWjx_eGVBg"
    val URL_GET_TOKEN = "https://dddb-124-120-36-179.ap.ngrok.io/api/v1/user/sts"

    var progressdialog: ProgressDialog? = null

    lateinit var btn_upload: Button
    lateinit var btn_download: Button
    lateinit var bucket_name: EditText
    lateinit var access_key: EditText
    lateinit var file_name: EditText
    lateinit var userId: EditText

    lateinit var progress: ProgressBar
    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         btn_upload = findViewById<Button>(R.id.btn_upload)
         btn_download = findViewById<Button>(R.id.btn_download)
        bucket_name = findViewById<EditText>(R.id.bucket_name)
         access_key = findViewById<EditText>(R.id.access_key)
         file_name = findViewById<EditText>(R.id.file_name)
         userId = findViewById<EditText>(R.id.user_id)

         progress = findViewById<ProgressBar>(R.id.progress)
        progress.max = 100
        applicationContext.startService(Intent(applicationContext, TransferService::class.java))

        btn_upload.setOnClickListener {
            if (userId.text.toString().trim().isNotEmpty()) {
                getToken(object : Listener {
                    override fun completed() {
                        uploadWithTransferUtility()
                    }

                    override fun error() {
                    }
                })
            }
        }
        btn_download.setOnClickListener {
            if (userId.text.toString().trim().isNotEmpty()) {
                getToken(object : Listener {
                    override fun completed() {
                        downloadWithTransferUtility()
                    }

                    override fun error() {
                    }
                })
            }
        }
    }

    fun getToken(listener: Listener) {
//        showLoadingDialog()
        AndroidNetworking.post(URL_GET_TOKEN)
            .addHeaders("x-user-id", userId.text.toString().trim())
            .setTag("URL_GET_TOKEN")
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    response?.let {
                         accessKeyId = it.optString("accessKeyId","")
                         val expiration = it.optString("expiration","")
                         secretAccessKey = it.optString("secretAccessKey","")
                         sessionToken = it.optString("sessionToken","")
                    }
                    hideLoadingDialog()
                    listener.completed()
                }

                override fun onError(anError: ANError?) {
                    hideLoadingDialog()
                    anError?.let {
                        it.printStackTrace()
                        Toast.makeText(this@MainActivity, "Upload Complete", Toast.LENGTH_LONG).show()
                    }
                    listener.error()
                }
            })
    }
    fun uploadWithTransferUtility() {
        val file_name = file_name.text.toString().trim()
        val userId = userId.text.toString().trim()
        val bucketName = bucket_name.text.toString().trim()
//        secret_key?.let {
//            if (!it.isEmpty()) {
//                accessKeyId = it
//            }
//        }
//        access_key?.let {
//            if (!it.isEmpty()) {
//                secretAccessKey = it
//            }
//        }
        val credentials = BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken)

        val configuration = ClientConfiguration()
        configuration.maxErrorRetry = 3
        configuration.connectionTimeout = 60 * 1000 // 连接超时，默认15秒
        configuration.socketTimeout = 60 * 1000 // socket超时，默认15秒
        configuration.protocol = Protocol.HTTPS
        configuration.maxConnections = 10 // 最大并发请求，默认10个
        val region: Region = Region.getRegion(Regions.AP_SOUTHEAST_1)
        val s3Client = AmazonS3Client(credentials, region, configuration)
        s3Client.endpoint = "https://s3.ecsce.tsaw.dev/$bucketName"

        val transferUtility = TransferUtility.builder()
                .s3Client(s3Client)
            .defaultBucket("")
            .context(applicationContext)
                .build()

        val file = File(applicationContext.filesDir, "$file_name.txt")
        try {
              val writer = BufferedWriter(FileWriter(file))
            writer.append("Hello World! at"+ System.currentTimeMillis())
            writer.close()
        }catch (  e: Exception){
            Log.e(TAG, e.message!!)
        }
          val uploadObserver = transferUtility.upload(
        "$file_name.txt",
        File(applicationContext.filesDir, "$file_name.txt"))
        progress.visibility = View.VISIBLE
        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(object : TransferListener {
            override  fun onStateChanged(id: Int, state: TransferState){
                if (TransferState.COMPLETED == state){
                    Toast.makeText(this@MainActivity, "Upload Complete", Toast.LENGTH_LONG).show()
                    // Handle a completed upload.
                    progress.visibility = View.GONE
                }
            }
            override  fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long){

                  val percentDonef = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
                  val percentDone = percentDonef.toInt()
                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                 + " bytesTotal: " + bytesTotal + " " + percentDone + "%")
                progress.progress = percentDone
            }
            override  fun onError(id: Int, ex: Exception){
                // Handle errors
                ex.printStackTrace()
                Toast.makeText(this@MainActivity, "Upload error id: "+id +"msg: "+ ex.message, Toast.LENGTH_LONG).show()

                Log.d(TAG, "onError"+id);
                progress.visibility = View.GONE
            }
        })

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.state){
                                Toast.makeText(this@MainActivity, "Handle a completed upload.", Toast.LENGTH_LONG).show()

            // Handle a completed upload.
        }
        Log.d(TAG, "Bytes Transferred: " + uploadObserver.bytesTransferred)
        Log.d(TAG, "Bytes Total: " + uploadObserver.bytesTotal)

    }

    private fun downloadWithTransferUtility(){
        val file_name = file_name.text.toString().trim()
        val userId = userId.text.toString().trim()
        val bucketName = bucket_name.text.toString().trim()

        val credentials = BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken)

        val configuration = ClientConfiguration()
        configuration.maxErrorRetry = 3
        configuration.connectionTimeout = 60 * 1000 // 连接超时，默认15秒
        configuration.socketTimeout = 60 * 1000 // socket超时，默认15秒
        configuration.protocol = Protocol.HTTPS
        configuration.maxConnections = 10 // 最大并发请求，默认10个
        val region: Region = Region.getRegion(Regions.AP_SOUTHEAST_1)
        val s3Client = AmazonS3Client(credentials, region, configuration)
        s3Client.endpoint = "https://s3.ecsce.tsaw.dev/$bucketName"
        val transferUtility = TransferUtility.builder()
            .s3Client(s3Client)
            .defaultBucket("")
            .context(applicationContext)
            .build()
          val downloadObserver = transferUtility.download(
        "$file_name",
        File(applicationContext.filesDir, "$file_name"))
        progress.visibility = View.VISIBLE
        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(object : TransferListener {
            override  fun onStateChanged(id: Int, state: TransferState){
                if (TransferState.COMPLETED == state){
                    // Handle a completed upload.
                    Toast.makeText(this@MainActivity, "Download Complete", Toast.LENGTH_LONG).show()
                    progress.visibility = View.GONE
                }
            }
            override  fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long){
                  val percentDonef = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
                  val percentDone = percentDonef.toInt()
                Log.d("Your Activity", "   ID:$id   bytesCurrent: $bytesCurrent   bytesTotal: $bytesTotal $percentDone%")
                progress.progress = percentDone
            }
            override  fun onError(id: Int, ex: Exception){
                // Handle errors
                Toast.makeText(this@MainActivity, "Download error id: "+id +"msg: "+ ex.message, Toast.LENGTH_LONG).show()
                progress.visibility = View.GONE
            }
        })

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == downloadObserver.state){
            // Handle a completed upload.
        }
        Log.d("Your Activity", "Bytes Transferred: " + downloadObserver.bytesTransferred)
        Log.d("Your Activity", "Bytes Total: " + downloadObserver.bytesTotal)
    }
    companion object  {
        private  val  TAG = DownloadActivity::class.java.simpleName
    }

    private fun showLoadingDialog() {
        progressdialog = ProgressDialog(this.applicationContext)
        progressdialog?.setMessage("Please Wait....")
        progressdialog?.show()
    }

    private fun hideLoadingDialog() {
        try {
            progressdialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    interface Listener {
        fun completed()
        fun error()
    }
}