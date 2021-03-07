package org.cyclismo.opensweat

import android.content.Context
import android.content.Intent
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

/* ****************************************************************
    Class used to receive a UDP message sent from a fan controller.

    This is run as a separate thread. It will wait for a UDP message
    and send an intent to the main thread if it hears anything juicy.
   **************************************************************** */

class ReceiveUDP constructor(
        private val port: Int
) : Thread() {

    // We need the app's context in order to send the intent to the main thread.
    private lateinit var parentContext: Context

    public fun setContext(context: Context) {
        // setter function to specify the context for the app.
        // If this is not called before starting this thing up,
        // then we crash and burn and our widows are left to
        // pick up their shattered lives and eventually move on.
        this.parentContext = context
    }

    // Method that is called when an object is told to start a
    // separate thread.
    public override fun run() {
        // As long as running is set, the loop to wait and
        // receive information will run.
        var running: Boolean = true;
        val buffer: ByteArray = ByteArray(256)
        var socket: DatagramSocket

        try {
            // Set up some data to receive.
            socket = DatagramSocket(this.port+1)
        } catch (e: IOException) {
            //running = false
            //println("IO Exception ${e.message}")
            return

        } catch (e: SocketException) {
            //running = false
            //println("Socket Exception ${e.message}")
            return

        }

        while (running) {
            // Wait and see if anybody out there is lonely enough to send some good information our way.
            val packet: DatagramPacket = DatagramPacket(buffer,buffer.size)
            socket.receive(packet)

            // We must have received something! Somebody loves us!
            // Get all of the relevant information and then let the main thread know
            // what information was passed along.
            //val address: InetAddress = packet.address
            //val incomingPort: Int = packet.port
            val info: String = String(packet.data,0,packet.length)
            val statusQueryIntent : Intent = Intent("org.cyclismo.opensweat.RECEIVED_QUERY")
            statusQueryIntent.putExtra("query",info)
            parentContext.sendBroadcast(statusQueryIntent)
        }

        socket.close()
    }

}
