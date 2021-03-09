/* ****************************************************************
    OpenSweat
    Allows an android app to turn a switch on and off via the network.
    Copyright (C) 2021  Kelly Black

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

 ****************************************************************** */



package org.cyclismo.opensweat

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import java.io.IOException
import java.net.*

/* ****************************************************************
    Class used to send a broadcast UDP message on the whole subnet.

    This is run as a separate thread. It is for taking a given
    message and then broadcasting it via UDP on a given port.
   **************************************************************** */

class SendBroadcastUDP  constructor(
        private val port: Int,
        private val applicationContent: Context
) : Thread()  {

    var infoToShare: String = ""    // The message to be sent.
    fun setMessage(msg: String) {
        // Specify what the message is, and then start a separate thread to broadcast the message.
        this.infoToShare = msg
        this.start()
    }

    public override fun run()
    {
        // Method called when an object of this type is told to run a separate thread.
        // This will send a broadcast UDP message on the whole subnet.
        try {
            println("sending: $this.infoToShare")

            // Specify which subnet to use for the broadcast.
            //val me: InetAddress? = InetAddress.getByName("192.168.1.135")
            //val me: InetAddress? = this.getBroadcastAddress()
            val me: InetAddress? = InetAddress.getByName("255.255.255.255")

            // Create the socket that will be used to send the message.
            // Set it to broadcast on the subnet.
            val multiCast = DatagramSocket(this.port)
            multiCast.reuseAddress = true
            multiCast.broadcast = true

            // Convert the information to be sent into a format that can be
            // used by the send routine. Then send that bad boy to the world.
            // We love it, therefore we will set it free.
            val data = this.infoToShare.toByteArray()
            val packet = DatagramPacket(data, data.size, me, this.port)
            multiCast.send(packet)
            multiCast.close()

        } catch (e: IOException) {
            //println("IO Exception ${e.message}")
        } catch (e: SocketException) {
            //println("Socket Exception ${e.message}")
        }

    }

    // Method to determine the broadcast address for the subnet.
    // It is not currently used and is a copy/paste rip off.
    // Not even sure if it works, but it is currently not needed.
    // It may be useful in the future so we will hord it and keep
    // the precious for ourselves.
    fun getBroadcastAddress() : InetAddress {
        // Code example found at https://stackoverflow.com/questions/17308729/send-broadcast-udp-but-not-receive-it-on-other-android-devices
        val wifi: WifiManager? = this.applicationContent.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp: DhcpInfo? = wifi?.dhcpInfo

        if(dhcp != null) {
            val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
            return InetAddress.getByAddress(quads)
        }

        return(InetAddress.getLocalHost())
    }

}