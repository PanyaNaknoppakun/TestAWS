package com.example.testaws

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File


class DownloadActivity : Activity() {
    var accessKeyId = "AKIA5F598893E83111F3"
    var secretAccessKey= "L9CEZU3sNzKN3zOFVLcmuDQNUjJCFmx07v/LiPQ/"
    override  fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        applicationContext.startService(Intent(applicationContext, TransferService::class.java))
        downloadWithTransferUtility()

    }
    private fun downloadWithTransferUtility(){

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
          val downloadObserver = transferUtility.download(
        "$file_name.txt",
        File(applicationContext.filesDir, "$file_name.txt"))

        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(object : TransferListener{
            override  fun onStateChanged(id: Int, state: TransferState){
                if (TransferState.COMPLETED == state){
                    // Handle a completed upload.
                    Toast.makeText(this@DownloadActivity, "Download Complete", Toast.LENGTH_LONG).show()

                }
            }
            override  fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long){
                  val percentDonef = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
                  val percentDone = percentDonef.toInt()
                Log.d("Your Activity", "   ID:$id   bytesCurrent: $bytesCurrent   bytesTotal: $bytesTotal $percentDone%")
            }
            override  fun onError(id: Int, ex: Exception){
                // Handle errors
                Toast.makeText(this@DownloadActivity, "Download Complete", Toast.LENGTH_LONG).show()
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
}