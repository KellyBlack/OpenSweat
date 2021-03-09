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

#include "ESP8266WiFi.h"
#include <WiFiUdp.h>

//#define DEBUG

#ifdef DEBUG
#include "./wifiParams.h"
#else
// First define the wifi paramters.
char ssid[] = "SSID GOES HERE";        // your network SSID (name)
char pass[] = "PASSWORD GOES HERE";    // your network password
#endif

// Define the udp communication variables and parameters
unsigned int localPort = 3517;      // local port to listen on - 
                                    // not in wide spread use: 
                                    // https://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers
WiFiUDP Udp;

// Define the character buffers that will be used to share information.
#define MESSAGE_LENGTH 256
char packetBuffer[MESSAGE_LENGTH] = "//OpenSweat/fan/0/1";      //buffer to hold incoming packet
char replyBuffer[MESSAGE_LENGTH]  = "//OpenSweat/status/";       // a string to send back

// Define the pins that we are using to connect the switches and relays.
#define D5 14
#define D6 12
#define D7 13
#define D8 15

// Define the variables used to keep track of the status of the fans.
int fanStatus[2] = {0,0};     // Array to keep track of each fan.
                              // Ex: if fanStatus[1]=0 then fan 1 is off.
                              //     if fanStatus[0]=1 then fan 0 is on.
int switchStatus[2] = {0,0};  // Array to keep track of each local switch.
                              // Ex: if switchStatus[1]=0 then switch 1 is off.
                              //     if switchStatus[0]=1 then switch 0 is on.
int fanPins[2] = {D6,D8};     // Array to keep track of the pins that are
                              // connected to the two relays.
                              // Ex: fanPin[0] is the pin used to turn fan 0 on/off
int switchPins[2] = {D5,D7};  // Array to keep track of the pins that are
                              // connected to the two switchs.
                              // Ex: switchPin[0] is the pin used to monitor switch 0



void setup() {
  // put your setup code here, to run once:
#ifdef DEBUG
  Serial.begin(115200);
  Serial.print("Sketch Starting....");
#endif

  // Set the pin modes that will be used to change the fans.
  int lupe = sizeof(switchPins)/sizeof(switchPins[0]);
  while(lupe > 0)
  {
    lupe -= 1;
    pinMode(switchPins[lupe], INPUT);
    pinMode(fanPins[lupe], OUTPUT);
    digitalWrite(fanPins[lupe],LOW);
  }
 
  WiFi.disconnect(true);
  delay(1000);

  WiFi.begin(ssid, pass);
#ifdef DEBUG
  Serial.println(WiFi.status());
  Serial.print("Connecting");
#endif
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
#ifdef DEBUG
    Serial.print(".");
#endif
  }
#ifdef DEBUG
  Serial.println();
  Serial.println(WiFi.softAPIP());
  Serial.println(WiFi.localIP());
#endif
  Udp.begin(localPort);
#ifdef DEBUG
  Serial.println("Server started");
#endif


}

void loop() {
  // This is the code that gets executed at each time step.

  // Loop through each pin associated with a switch.
  int lupe = sizeof(switchPins)/sizeof(switchPins[0]);
  while(lupe > 0)
  {
    lupe -= 1;

    // Check to see the status of the pin associated with the switch.
    if (digitalRead(switchPins[lupe]) == LOW)
    {
      // The switch is in the off position.
      if(switchStatus[lupe]==1)
      {
          // The switch used to be in the on position - so it changed.
          // Turn off the fan and update the status of the switch 
          // and the fan.
#ifdef DEBUG
          Serial.print("Switch " );
          Serial.print(lupe);
          Serial.println(" changed to off");
#endif
          digitalWrite(fanPins[lupe],LOW);
          switchStatus[lupe] = 0;
          fanStatus[lupe] = 0;
      }
    }
    else
    {
      // The switch is in the on position.
      if(switchStatus[lupe]==0)
      {
          // The switch used to be in the off position - so it changed.
          // Turn on the fan and update the status of the switch 
          // and the fan.
#ifdef DEBUG
          Serial.print("Switch " );
          Serial.print(lupe);
          Serial.println(" changed to on");
#endif
          digitalWrite(fanPins[lupe],HIGH);
          switchStatus[lupe] = 1;
          fanStatus[lupe] = 1;
      }
    }

  }
  

  int packetSize = Udp.parsePacket();
  if (packetSize) {
    // Something was sent in via udp
    // Read it and convert to a nicely formatted character string.
    int len = Udp.read(packetBuffer, MESSAGE_LENGTH-1);
    if (len > 0) packetBuffer[len] = 0;

#ifdef DEBUG
    Serial.print("Received(IP/Size/Data): ");
    Serial.println(Udp.remoteIP());
    Serial.println(packetSize);
    Serial.println(packetBuffer);
#endif

    processIncomingMessage(packetBuffer,replyBuffer,Udp);
  }
     
  delay(500);
}

void processIncomingMessage(char *packetBuffer,char *replyBuffer,WiFiUDP Udp)
{
    char *location;
    char status[2];

  // First - check to see if the string that was passed
  // Starts off with the officially sanctioned preamble
  // approved by the OpenSweat Foundation (OSF)
  location = strstr(packetBuffer,"//OpenSweat/");
  if(location == packetBuffer)
  {
    // The string that was passed appears to be an OpenSweat Request.
    // Check to see what the request is.

    location = strstr(packetBuffer,"//OpenSweat/query");
    if(location) 
    {
      // This is a request to query the status of the fans.
      // Send a reply

      // Prepare the reply with its preamble.
#ifdef DEBUG
      Serial.println("Request Status");
#endif
      strcpy(replyBuffer,"//OpenSweat/status/");

      //For each fan add a 0/1 depending on whether or not it is on.
      status[1] = 0;
      int numberFans = sizeof(fanStatus)/sizeof(fanStatus[0]);
      int lupe;
      for(lupe=0;lupe<numberFans;++lupe)
      {
        // concatenate the status of the next fan to the status string
        status[0] = fanStatus[lupe]+'0';
        strcat(replyBuffer,status);
        strcat(replyBuffer,"/");
      }
#ifdef DEBUG
      Serial.println(replyBuffer);
      Serial.println(Udp.remotePort());
#endif
      Udp.beginPacket(Udp.remoteIP(),1+localPort);
      Udp.write(replyBuffer);
      Udp.endPacket();
    }
    else
    {
      // This is not a status request. Check to see if it is a request
      // to change a fan.
      location = strstr(packetBuffer,"//OpenSweat/fan/");
      if(location)
      {
        // This is a request to change the power status of one of the fans.
        if(strlen(location)>18)
        {
          int fanNumber  = packetBuffer[16]-'0'; // Determine which fan to change
          int fanRunning = packetBuffer[18]-'0'; // Determine whether the fan will 
                                                 // be on (1) or off (0).
          if(fanNumber < sizeof(fanStatus)/sizeof(fanStatus[0])) 
          {
            // It is safe to make a change to the fan status
            fanStatus[fanNumber] = (fanRunning != 0);
            if (fanStatus[fanNumber]==0)
            {
              // Need to turn the fan off.
#ifdef DEBUG
              Serial.println("Turn off fan");
#endif
              digitalWrite(fanPins[fanNumber],LOW);
            }
            else
            {
              // Need to turn the fan on
#ifdef DEBUG
              Serial.println("Turn on fan");
#endif
              digitalWrite(fanPins[fanNumber],HIGH);              
            }
          }
        }
        
      }
    }
    
  }

}
