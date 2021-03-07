package org.cyclismo.opensweat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*
import java.util.concurrent.locks.ReentrantLock


class MainActivity : AppCompatActivity() {

    /* ****************************************************
      Primary view for the class.

      This class handles all the button presses that occur
      in the main view of the interface. There are two fan
      buttons and a button to query the current fan status.

      There are also two textviews that change depending
      on whether or not a fan controller was found after
      a UDP broadcast was sent out on the subnet asking
      for a response.

     ****************************************************** */

    val portNumber: Int = 3517                                      // The port that UDP will use to make out going queries.
    val fanStatus: Array<Boolean> = arrayOf<Boolean>(false,false)   // Each element of the array indicates whether or not the corresponding fan is on or off
    val fanButtons: MutableList<Button> = arrayListOf<Button>()     // Each element of the list is a pointer to the corresponding fan toggle button
    val incomingUDP: ReceiveUDP = ReceiveUDP(this.portNumber)       // This class is used to send out a UDP broadcast message.
    val lock: ReentrantLock = ReentrantLock()                       // A mutex to make sure the text buttons are changed in a safe manner.
                                                                    // The UDP receiver operates in a separate thread, and at one point
                                                                    // this was used to ensure a thread safe operation, but it should probably be removed now.

    override protected fun onStart() {
        // Function called when everything is set up and the app has started up.
        super.onStart()

        // Start up a separate thread that will listen on the appropriate UDP port
        // for an incoming message from a fan controller. Set the current context
        // on the listener so it can send an intent to this activity.
        this.incomingUDP.start()
        this.incomingUDP.setContext(applicationContext)

        //this.fanQueryStatus()
        // Set up a receiver so that when an incoming message is received
        // the UDP listener will have a way to let the main thread know
        // what information needs to be processed.
        val broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                if(intent != null)
                    updateFanStatus(intent.getStringExtra("query"))
            }
        }
        // Let the current class know that it can receive an intent of the type that indicates
        // that we have received a response from a fan controller.
        registerReceiver(broadCastReceiver, IntentFilter("org.cyclismo.opensweat.RECEIVED_QUERY"))

        // Set up a timer that will go off periodically. Each time the
        // timer goes off it will send out a broadcast to see if the
        // fan status should be changed.
        Timer().scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                //fanQueryStatus()
                this@MainActivity.runOnUiThread({
                    fanQueryStatus()
                })
            }
        },1000,120000)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Function that is called once the interface is set up, the
        // interface can be created and the callbacks can be created.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Specify the routine that should be called when the first fan button is pressed
        val fan1Button: Button = findViewById(R.id.Fan1Button)
        fan1Button.setOnClickListener {
            this.fanButtonOneClicked()
        }
        this.fanButtons.add(fan1Button)  // Add the button to the list of fan buttons

        // Specify the method that should be called when the second fan button is pressed
        val fan2Button: Button = findViewById(R.id.Fan2Button)
        fan2Button.setOnClickListener{
            this.fanButtonTwoClicked()
        }
        this.fanButtons.add(fan2Button) // Add the button to the list of fan buttons

        // Specify the method that is called when the query button is pressed.
        val fanQueryButton: Button = findViewById(R.id.FanQueryButton)
        fanQueryButton.setOnClickListener {
            this.fanQueryStatus()
        }

    }

    // Method called when the first fan button is pressed.
    fun fanButtonOneClicked()
    {
        this.fanButtonUpdate(0) // This is the button associated with the first entries in the lists.
    }

    // Method called when the second fan button is pressed.
    fun fanButtonTwoClicked()
    {
        this.fanButtonUpdate(1) // This is the button associated with the second entries in the lists.
    }

    // Method called to update the appearance and records associated with the given fan number.
    fun fanButtonUpdate(whichFan:Int)
    {
        // This routine updates everything associated with the given fan.
        // First toggle whether or not the fan is on or off.
        this.fanStatus[whichFan] = !this.fanStatus[whichFan]

        // Send a broadcast UDP message on the subnet to let the controller
        // know whether or not this fan should be on or off.
        // Update the text on the button accordingly.
        val sendUDP : SendBroadcastUDP = SendBroadcastUDP(this.portNumber,this.applicationContext)
        if(this.fanStatus[whichFan]) {
            // This fan should be on.
            this.fanButtons[whichFan].setText(getString(R.string.FanButtonOn))
            sendUDP.setMessage("//OpenSweat/fan/$whichFan/1")
        }
        else {
            // This fan should be off.
            this.fanButtons[whichFan].setText(getString(R.string.FanButtonOff))
            sendUDP.setMessage("//OpenSweat/fan/$whichFan/0")
        }

    }

    // Method called when the query button is pressed.
    fun fanQueryStatus() {
        // Send a broadcast UDP message on the subnet to tell the fan controller
        // that it should send back information about the fans' status.
        val sendUDP : SendBroadcastUDP = SendBroadcastUDP(this.portNumber,this.applicationContext)
        sendUDP.setMessage("//OpenSweat/query")

        // Change the text notice and assume that there is no known fan controller.
        // If we get a reply this text will be corrected.
        val fanController: TextView = findViewById(R.id.FanFound)
        fanController.setText(getString(R.string.NoFanFound))
    }

    // When a reply is sent back from the fan controller, the information sent is
    // processed in this method.
    fun updateFanStatus(passedInfo: String?) {
        lock.lock()  // Set the mutex. This should probably be deleted. We do not need it anymore.

        // Set up the regular expressions used to make decisions about what information was sent back.
        val hasPreamble: Regex = Regex("^//OpenSweat/status")
        val slashes: Regex = Regex("/")

        if(passedInfo != null) {
            // Some information was sent back.
            // First get the textViews that will hold the relvant information.
            val incomingText : TextView = findViewById(R.id.receiverText)
            val fanController: TextView = findViewById(R.id.FanFound)

            // Change the textViews to show what information has been passed and
            // indicate that a fan controller is available.
            incomingText.setText("Received: $passedInfo")
            fanController.setText(getString(R.string.FanFound))
            if(hasPreamble.containsMatchIn(passedInfo)) {
                // This contains information about the status of the fans.
                val parts = slashes.split(passedInfo)  // Break the string up into its parts separated by forward slashes.
                for (lupe in 4..(parts.size - 1)) {
                    // For each element after the preamble. Assume it is information about whether
                    // or not the associated fan is on or off.
                    if (parts[lupe] == "0") {
                        // This fan is off. Update its button and its stored status.
                        //println("$lupe is off")
                        this.fanButtons[lupe - 4].setText(getString(R.string.FanButtonOff))
                        this.fanStatus[lupe - 4] = false
                        incomingText.setText("Fan $lupe off")
                    } else if (parts[lupe].length == 1) {
                        // This fan is on. Update its button and its stored data.
                        //println("$lupe is on")
                        this.fanButtons[lupe - 4].setText(getString(R.string.FanButtonOn))
                        this.fanStatus[lupe - 4] = true
                        incomingText.setText("Fan $lupe on")
                    }
                }

            }
        }

        lock.unlock()
    }

}

