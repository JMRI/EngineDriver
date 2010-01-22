/*Copyright (C) 2010 Jason M'Sadoques
  jlyonm@gmail.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.lnxgfx;

import net.lnxgfx.message_type;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.net.*;
import java.io.*;
import android.util.Log;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
//it is a part of the Application.
public class threaded_application extends Application
{

  class comm_thread extends Thread
  {
    String host_ip; //The IP address (TODO: test/allow host names) if the WiThrottle server.
    int port; //The TCP port that the WiThrottle server is running on (TODO: Add ZeroConf capability).
    int loco_address; //The Address of the locomotive being controlled (TODO: Allow multiple engines at the same time).
    //Communications variables.
    Socket client_socket;
    InetAddress host_address;
    PrintWriter output_pw;

    class comm_handler extends Handler
    {
      //All of the work of the communications thread is initiated from this function.
      public void handleMessage(Message msg)
      {
        switch(msg.what)
        {
          //Connect to the WiThrottle server.
          case message_type.CONNECT:
            //The IP address is stored in the obj as a String, the port is stored in arg1.
            host_ip=new String((String)msg.obj);
            port=msg.arg1;

            try { host_address=InetAddress.getByName(host_ip); }
            catch(UnknownHostException except)
            {
              Log.e("comm_handler.handleMessage", "Error getting InetAddress, UnknownHostException: "+except.getMessage());
              //TODO: Handle this exception.
            }

            try { client_socket=new Socket(host_address, port); }
            catch(IOException except)
            {
              Log.e("comm_handler.handleMessage", "Error creating a new Socket with IP "+host_ip+" and port "+port+": "+except.getMessage());
              //TODO: Handle this exception.
            }

            try { output_pw=new PrintWriter(new OutputStreamWriter(client_socket.getOutputStream()), true); }
            catch(IOException except)
            {
              Log.e("comm_handler.handleMessage", "Error creating a PrintWriter, IOException: "+except.getMessage());
              //TODO: Handle this exception.
            }
            //TODO: Give the throttle a better name. I'm not sure if multiple devices can have the same name at the same time.
            output_pw.println("NDroid");
            Message connection_message=Message.obtain();
            connection_message.what=message_type.CONNECTED;
            ui_msg_handler.sendMessage(connection_message);
          break;
          //Disconnect from the WiThrottle server.
          case message_type.DISCONNECT:
            output_pw.println("Q");
            try { client_socket.close(); }
            catch(IOException except)
            {
              Log.e("com_handler.handleMessage", "Error closing the Socket, IOException: "+except.getMessage());
              //TODO: Handle this exception.
            }
          break;
          //Set up an engine to control. The address of the engine is given in arg1, and the address type (long or short) is given in arg2.
          case message_type.LOCO_ADDR:
            loco_address=msg.arg1;
            output_pw.format("CT"+(msg.arg2==address_type.LONG ? "L" : "S")+"%d\n", loco_address);
            //In order to get the engine to start, I must set a direction and some non-zero velocity and then set the velocity to zero. TODO: Fix this bug
            //in the WiThrottle server.
            output_pw.print("TR1\nTV1\nTV0\n");
          break;
          case message_type.ERROR:
          break;
          //Adjust the locomotive's speed. arg1 holds the value of the speed to set. //TODO: Allow 14 and 28 speed steps (might need a change on the server size).
          case message_type.VELOCITY:
            output_pw.format("TV%d\n", msg.arg1);
          break;
          //Change direction. arg2 holds the direction to change to. The reason direction is in arg2 is for compatibility
          //with the function buttons.
          case message_type.DIRECTION:
            output_pw.format("TR%d\n", msg.arg2);
          break;
          //Set or unset a function. arg1 is the function number, arg2 is set or unset.
          case message_type.FUNCTION:
            output_pw.format("TF%d%d\n", msg.arg2, msg.arg1);
          break;
        }
      };
    }

    //For communication to the comm_thread.
    public comm_handler comm_msg_handler;
    //For communication back to the main UI thread.
    public Handler ui_msg_handler;

    public void run()
    {
      Looper.prepare();
      comm_msg_handler=new comm_handler();
      //TODO: Respond to incoming communication on the Socket. I think this is done by creating a custom Looper.
      Looper.loop();
    };
  }

  public comm_thread thread;

  public void onCreate()
  {
    thread=new comm_thread();
    thread.start();
  };
}
