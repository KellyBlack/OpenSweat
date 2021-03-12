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

import java.io.IOException
import java.lang.Exception
import java.net.*

/* ****************************************************************
    Class used to send a UDP message to a specific host.

    This has not been tested and is not used.... It was originally
    thought it would be used, but things change and we moved on.
    This is the abandoned orphan left to live life on its own.
    So just kick it to the side and ignore it until we can find
    some utilitarian purpose for it later.

   **************************************************************** */


class SendTargetUDP  constructor(
        private val port: Int,
        private val address: InetAddress
) : Thread()  {

    var infoToShare: String = ""
    fun setMessage(msg: String) {
        this.infoToShare = msg
        this.start()
    }

    public override fun run()
    {
        try {
            val sendSocket = DatagramSocket(this.port)
            sendSocket.reuseAddress = true

            val data = this.infoToShare.toByteArray()
            val packet = DatagramPacket(data, data.size, this.address, this.port)
            sendSocket.send(packet)
            sendSocket.close()

        } catch (e: IOException) {
            //println("IO Exception ${e.message}")
        } catch (e: SocketException) {
            //println("Socket Exception ${e.message}")
        } catch(e: Exception) {
            //print("General exception ${e.message)")
        }

    }


}