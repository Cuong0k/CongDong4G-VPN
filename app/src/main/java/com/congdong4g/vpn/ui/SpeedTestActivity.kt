package com.congdong4g.vpn.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.congdong4g.vpn.databinding.ActivitySpeedTestBinding
import com.congdong4g.vpn.model.SpeedTestResult
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class SpeedTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpeedTestBinding
    private var isRunning = false
    private var testJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnStartTest.setOnClickListener {
            if (isRunning) {
                stopTest()
            } else {
                startTest()
            }
        }
    }

    private fun startTest() {
        isRunning = true
        binding.btnStartTest.text = "DỪNG"
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Đang kiểm tra..."

        resetResults()

        testJob = lifecycleScope.launch {
            try {
                // Test Ping
                binding.tvStatus.text = "Đang đo ping..."
                val ping = testPing()
                binding.tvPing.text = "${ping}ms"

                // Test Download
                binding.tvStatus.text = "Đang đo tốc độ tải xuống..."
                val downloadSpeed = testDownload()
                binding.tvDownload.text = String.format("%.2f Mbps", downloadSpeed)

                // Test Upload
                binding.tvStatus.text = "Đang đo tốc độ tải lên..."
                val uploadSpeed = testUpload()
                binding.tvUpload.text = String.format("%.2f Mbps", uploadSpeed)

                // Done
                binding.tvStatus.text = "Hoàn thành!"

                // Save result
                val result = SpeedTestResult(
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    ping = ping,
                    serverName = "Speed Test Server"
                )

            } catch (e: CancellationException) {
                binding.tvStatus.text = "Đã dừng"
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvStatus.text = "Lỗi: ${e.message}"
            } finally {
                isRunning = false
                binding.btnStartTest.text = "BẮT ĐẦU"
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun stopTest() {
        testJob?.cancel()
        isRunning = false
        binding.btnStartTest.text = "BẮT ĐẦU"
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.text = "Đã dừng"
    }

    private fun resetResults() {
        binding.tvPing.text = "---"
        binding.tvDownload.text = "---"
        binding.tvUpload.text = "---"
    }

    private suspend fun testPing(): Long = withContext(Dispatchers.IO) {
        try {
            val host = "8.8.8.8"
            val startTime = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 3 -W 3 $host")
            process.waitFor()
            val endTime = System.currentTimeMillis()
            (endTime - startTime) / 3
        } catch (e: Exception) {
            -1L
        }
    }

    private suspend fun testDownload(): Double = withContext(Dispatchers.IO) {
        try {
            // Test file URL (10MB)
            val testUrl = "https://speed.cloudflare.com/__down?bytes=10000000"
            val url = URL(testUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000

            val startTime = System.currentTimeMillis()
            val inputStream: InputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var totalBytes = 0L

            while (isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                totalBytes += bytesRead

                // Update progress
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 0) {
                    val speedMbps = (totalBytes * 8.0) / (elapsed * 1000)
                    withContext(Dispatchers.Main) {
                        binding.tvDownload.text = String.format("%.2f Mbps", speedMbps)
                    }
                }
            }

            inputStream.close()
            connection.disconnect()

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > 0) {
                (totalBytes * 8.0) / (elapsed * 1000) // Mbps
            } else {
                0.0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }

    private suspend fun testUpload(): Double = withContext(Dispatchers.IO) {
        try {
            // Simplified upload test
            val testUrl = "https://speed.cloudflare.com/__up"
            val url = URL(testUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000

            val dataSize = 5_000_000 // 5MB
            val data = ByteArray(dataSize)

            val startTime = System.currentTimeMillis()
            val outputStream = connection.outputStream
            outputStream.write(data)
            outputStream.flush()
            outputStream.close()

            connection.responseCode // Wait for response
            val elapsed = System.currentTimeMillis() - startTime

            connection.disconnect()

            if (elapsed > 0) {
                (dataSize * 8.0) / (elapsed * 1000) // Mbps
            } else {
                0.0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testJob?.cancel()
    }
}
