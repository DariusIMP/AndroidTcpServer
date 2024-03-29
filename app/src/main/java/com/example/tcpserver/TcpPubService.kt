package com.example.tcpserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class TcpPubService : Service() {

    companion object {
        private val TAG = TcpPubService::javaClass.name
        private const val PORT = 9876
        private val MSG = "01234567".toByteArray()
    }

    private var serverSocket: ServerSocket? = null
    private val working = AtomicBoolean(true)
    private val runnable = Runnable {
        var socket: Socket? = null
        try {
            serverSocket = ServerSocket(PORT)
            while (working.get()) {
                if (serverSocket != null) {
                    socket = serverSocket!!.accept()

                    Log.i(TAG, "New client: $socket")
                    val dataOutputStream = DataOutputStream(socket.getOutputStream())
                    val t: Thread = object: Thread() {
                        override fun run() {
                            try {
                                Log.i(TAG, "Publishing ${MSG.decodeToString()}...")
                                while (true) {
                                    dataOutputStream.write(MSG)
                                }
                            } catch (e: SocketException) {
                                Log.i(TAG, "Connection closed...")
                                dataOutputStream.close()
                            }
                        }
                    }
                    t.start()
                } else {
                    Log.e(TAG, "Couldn't create TCP Pub socket!")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                socket?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        startMeForeground()
        Thread(runnable).start()
    }

    override fun onDestroy() {
        working.set(false)
    }

    private fun startMeForeground() {
        val NOTIFICATION_CHANNEL_ID = packageName
        val channelName = "Tcp Server Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tcp Server is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }
}