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

import android.app.Activity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import android.widget.SimpleAdapter;
import android.widget.ListView;
import java.io.PrintWriter;
import java.io.File;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.IOException;
import android.util.Log;
import java.io.FileReader;
import android.view.View;
import android.os.Message;
import android.widget.EditText;
import android.widget.Button;
import android.os.Handler;
import android.content.Intent;
import android.widget.AdapterView;

public class connection_activity extends Activity
{
  ArrayList<HashMap<String, String> > connections_list;
  ArrayList<HashMap<String, String> > discovery_list;
  private SimpleAdapter connection_list_adapter;
  private SimpleAdapter discovery_list_adapter;

  ArrayList<String> ip_list;
  ArrayList<Integer> port_list;
  ArrayList<String> discovered_ip_list;
  ArrayList<Integer> discovered_port_list;

  //The IP address and port that are used to connect.
  String host_ip;
  int port;
  //Connect to the WiThrottle server.
  void connect()
  {
    Message connect_msg=Message.obtain();
    connect_msg.what=message_type.CONNECT;
    connect_msg.arg1=port;
    connect_msg.obj=new String(host_ip);
    threaded_application app=(threaded_application)this.getApplication();
    app.thread.comm_msg_handler.sendMessage(connect_msg);
  };

  void start_select_loco_activity()
  {
//    multicast_lock.release();
    Intent select_loco=new Intent().setClass(this, select_loco.class);
    startActivity(select_loco);
  };

  public enum server_list_type { DISCOVERED_SERVER, RECENT_CONNECTION }
  public class connect_item implements AdapterView.OnItemClickListener
  {
    server_list_type server_type;

    connect_item(server_list_type new_type) { server_type=new_type; }

    //When an item is clicked, connect to the given IP address and port.
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
      switch(server_type)
      {
        case DISCOVERED_SERVER:
          host_ip=new String(discovered_ip_list.get(position));
          port=discovered_port_list.get(position);
        break;
        case RECENT_CONNECTION:
          host_ip=new String(ip_list.get(position));
          port=port_list.get(position);
        break;
      }
      connect();
    };
  }

  public class button_listener implements View.OnClickListener
  {
    public void onClick(View v)
    {
      EditText entry=(EditText)findViewById(R.id.host_ip);
      host_ip=new String(entry.getText().toString());
      entry=(EditText)findViewById(R.id.port);
      port=new Integer(entry.getText().toString());
      connect();
    };
  }

  //Handle messages from the communication thread back to the UI thread.
  class ui_handler extends Handler
  {
    public void handleMessage(Message msg)
    {
      switch(msg.what)
      {
        case message_type.SERVICE_RESOLVED:
        {
          //Add this discovered service to the list.
          discovered_ip_list.add((String)msg.obj);
          discovered_port_list.add(msg.arg1);

          HashMap<String, String> hm=new HashMap<String, String>();
          hm.put("ip_address", discovered_ip_list.get(discovered_ip_list.size()-1));
          hm.put("port", discovered_port_list.get(discovered_port_list.size()-1).toString());
          discovery_list.add(hm);
          discovery_list_adapter.notifyDataSetChanged();
        }
        break;
        case message_type.CONNECTED:
          //Save the new list to the connections_list.txt file.
          File sdcard_path=Environment.getExternalStorageDirectory();
          File connections_list_file=new File(sdcard_path, "engine_driver/connections_list.txt");
          PrintWriter list_output;
          try
          {
            list_output=new PrintWriter(connections_list_file);
            //Add this connection to the head of connections list.
            list_output.format("%s:%d\n", host_ip, port);
            for(int i=0; i<ip_list.size(); i+=1)
            {
              if(host_ip.equals(ip_list.get(i)) && port_list.get(i)==port) { continue; }
              list_output.format("%s:%d\n", ip_list.get(i), port_list.get(i));
            }
            list_output.flush();
            list_output.close();
          }
          catch(IOException except)
          {
            Log.e("connection_activity", "Error creating a PrintWriter, IOException: "+except.getMessage());
          }

          start_select_loco_activity();
        break;
      }
    };
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.connection);

    threaded_application app=(threaded_application)this.getApplication();
    app.thread.ui_msg_handler=new ui_handler();
    //Set up a list adapter to allow adding discovered WiThrottle servers to the UI.
    discovery_list=new ArrayList<HashMap<String, String> >();
    discovery_list_adapter=new SimpleAdapter(this, discovery_list, R.layout.connections_list_item,
                                             new String[] {"ip_address", "port"},
                                             new int[] {R.id.ip_item_label, R.id.port_item_label});
    ListView discover_list=(ListView)findViewById(R.id.discovery_list);
    discover_list.setAdapter(discovery_list_adapter);
    discover_list.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));
    //Set up a list adapter to allow adding the list of recent connections to the UI.
    connections_list=new ArrayList<HashMap<String, String> >();
    connection_list_adapter=new SimpleAdapter(this, connections_list, R.layout.connections_list_item, new String[] {"ip_address", "port"},
                                   new int[] {R.id.ip_item_label, R.id.port_item_label});
    ListView conn_list=(ListView)findViewById(R.id.connections_list);
    conn_list.setAdapter(connection_list_adapter);
    conn_list.setOnItemClickListener(new connect_item(server_list_type.RECENT_CONNECTION));

    ip_list=new ArrayList<String>();
    port_list=new ArrayList<Integer>();
    discovered_ip_list=new ArrayList<String>();
    discovered_port_list=new ArrayList<Integer>();
    //Populate the ListView with the recent connections saved in a file. This will be stored in
    // /sdcard/engine_driver/connections_list.txt
    try
    {
      File sdcard_path=Environment.getExternalStorageDirectory();
      if(sdcard_path.canWrite())
      {
        //First, determine if the engine_driver directory exists. If not, create it.
        File engine_driver_dir=new File(sdcard_path, "engine_driver");
        if(!engine_driver_dir.exists()) { engine_driver_dir.mkdir(); }

        if(engine_driver_dir.exists() && engine_driver_dir.isDirectory())
        {
          //TODO: Fix things if the path is not a directory.
          File connections_list_file=new File(engine_driver_dir, "connections_list.txt");
          if(connections_list_file.exists())
          {
            BufferedReader list_reader=new BufferedReader(new FileReader(connections_list_file));
            while(list_reader.ready())
            {
              String line=list_reader.readLine();
              ip_list.add(line.substring(0, line.indexOf(':')));
              port_list.add(Integer.decode(line.substring(line.indexOf(':')+1, line.length())));
              HashMap<String, String> hm=new HashMap<String, String>();
              hm.put("ip_address", ip_list.get(ip_list.size()-1));
              hm.put("port", port_list.get(port_list.size()-1).toString());
              connections_list.add(hm);
            }
            connection_list_adapter.notifyDataSetChanged();
          }
        }

        //File gpxfile = new File(root, "gpxfile.gpx");
        //FileWriter gpxwriter = new FileWriter(gpxfile);
        //BufferedWriter out = new BufferedWriter(gpxwriter);
        //out.write("Hello world");
        //out.close();
      }
    }
    catch (IOException except) { Log.e("connection_activity", "Could not read file "+except.getMessage()); }

    //Set the button callback.
    Button button=(Button)findViewById(R.id.connect);
    button_listener click_listener=new button_listener();
    button.setOnClickListener(click_listener);

  }
}
