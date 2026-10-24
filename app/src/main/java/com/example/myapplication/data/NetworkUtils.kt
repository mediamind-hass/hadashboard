package com.example.myapplication.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    fun bindProcessToLocalNetwork(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val builder = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            connectivityManager.requestNetwork(builder.build(), object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    Log.d(TAG, "Successfully bound process to local Wi-Fi/Ethernet network via NetworkUtils (.removeCapability(NET_CAPABILITY_INTERNET))")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error binding process to local network: ${e.localizedMessage}", e)
        }
    }
}
