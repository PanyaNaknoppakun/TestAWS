package com.example.testaws

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


class UploadActivity : Activity(){

    var accessKeyId = "AKIA106977822F85DC90"
    var secretAccessKey= "HSwI6odOQWSzK2N29brgSQ8yOsbCa4YUXxW43bc3"
    lateinit var progress: ProgressBar
    override  fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        applicationContext.startService(Intent(applicationContext, TransferService::class.java))
        progress = findViewById<ProgressBar>(R.id.upload_progress)
        upload2()
//        uploadWithTransferUtility()
    }

    fun upload2() {

        val file_name = intent.getStringExtra("file_name")
        val bucket_name = intent.getStringExtra("bucket_name")
        val secret_key = intent.getStringExtra("secret_key")
        val access_key = intent.getStringExtra("access_key")
        secret_key?.let {
            if (!it.isEmpty()) {
                accessKeyId = it
            }
        }
        access_key?.let {
            if (!it.isEmpty()) {
                secretAccessKey = it
            }
        }
        val credentials = BasicAWSCredentials(accessKeyId, secretAccessKey)

        val configuration = ClientConfiguration()
        configuration.maxErrorRetry = 3
        configuration.connectionTimeout = 60 * 1000 // 连接超时，默认15秒
        configuration.socketTimeout = 60 * 1000 // socket超时，默认15秒
        configuration.protocol = Protocol.HTTPS
        configuration.maxConnections = 10 // 最大并发请求，默认10个
        val region: Region = Region.getRegion(Regions.AP_SOUTHEAST_1)
        val s3Client = AmazonS3Client(credentials, region, configuration)
        s3Client.endpoint = "https://s3.ecsce.tsaw.dev/$bucket_name"

        val transferUtility = TransferUtility.builder()
                .s3Client(s3Client)
            .defaultBucket("")
            .context(applicationContext)
                .build()

        val file = File(applicationContext.filesDir, "$file_name.txt")
        try {
              val writer = BufferedWriter(FileWriter(file))
            writer.append("Hello World!")
            writer.close()
        }catch (  e: Exception){
            Log.e(TAG, e.message!!)
        }
          val uploadObserver = transferUtility.upload(
        "$file_name.txt",
        File(applicationContext.filesDir, "$file_name.txt"))

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(object : TransferListener {
            override  fun onStateChanged(id: Int, state: TransferState){
                if (TransferState.COMPLETED == state){
                    Toast.makeText(this@UploadActivity, "complete upload", Toast.LENGTH_LONG).show()
                    // Handle a completed upload.
                }
            }
            override  fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long){
                  val percentDonef = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
                  val percentDone = percentDonef.toInt()
                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                 + " bytesTotal: " + bytesTotal + " " + percentDone + "%")

            }
            override  fun onError(id: Int, ex: Exception){
                // Handle errors
                ex.printStackTrace()
                Log.d(TAG, "onError"+id);
            }
        })

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.state){
                                Toast.makeText(this@UploadActivity, "Handle a completed upload.", Toast.LENGTH_LONG).show()

            // Handle a completed upload.
        }
        Log.d(TAG, "Bytes Transferred: " + uploadObserver.bytesTransferred)
        Log.d(TAG, "Bytes Total: " + uploadObserver.bytesTotal)

    }
       fun uploadWithTransferUtility(){

           val accessKeyId = "AKIAA019EAF246123837"
           val secretAccessKey= "2C2z2qz1gPrXSbd9tHmkGWDlVsBMQ+IWU/TxbII+"
           val credentials = BasicAWSCredentials(accessKeyId, secretAccessKey)
           val client = AmazonS3Client(credentials, Region.getRegion("ap-southeast-1"))
           client.endpoint = "https://s3.ecsce.tsaw.dev"
          val transferUtility = TransferUtility.builder()
        .context(applicationContext)
        .awsConfiguration(AWSMobileClient.getInstance().configuration).defaultBucket("user1")
        .s3Client(client)
        .build()
          val file = File(applicationContext.filesDir, "sample.txt")
        try {
              val writer = BufferedWriter(FileWriter(file))
            writer.append("Howdy World!")
            writer.close()
        }catch (  e: Exception){
            Log.e(TAG, e.message!!)
        }
          val uploadObserver = transferUtility.upload(
        "public/sample.txt",
        File(applicationContext.filesDir, "sample.txt"))

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(object : TransferListener {
            override  fun onStateChanged(id: Int, state: TransferState){
                if (TransferState.COMPLETED == state){
                    Toast.makeText(this@UploadActivity, "complete upload", Toast.LENGTH_LONG).show()
                    // Handle a completed upload.
                }
            }
            @RequiresApi(Build.VERSION_CODES.N)
            override  fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long){
                  val percentDonef = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
                  val percentDone = percentDonef.toInt()
                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                 + " bytesTotal: " + bytesTotal + " " + percentDone + "%")
                progress.setProgress(percentDone, false)
            }
            override  fun onError(id: Int, ex: Exception){
                // Handle errors
            }
        })

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.state){
                                Toast.makeText(this@UploadActivity, "Handle a completed upload.", Toast.LENGTH_LONG).show()

            // Handle a completed upload.
        }
        Log.d(TAG, "Bytes Transferred: " + uploadObserver.bytesTransferred)
        Log.d(TAG, "Bytes Total: " + uploadObserver.bytesTotal)
    }
       companion object  {
        private  val  TAG = UploadActivity::class.java.simpleName
    }
}