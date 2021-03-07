package org.cyclismo.opensweat

import java.io.IOException
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
            println(this.infoToShare)
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
        }

    }


}