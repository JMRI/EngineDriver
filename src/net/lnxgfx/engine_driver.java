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
import android.view.View;
import android.widget.EditText;
import java.text.NumberFormat;
import java.text.ParseException;
import java.net.*;
import java.io.*;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Button;
import android.view.MotionEvent;
import android.os.Message;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;

public class engine_driver extends Activity
{
  public class function_button_touch_listener implements View.OnTouchListener
  {
    int function;
    boolean is_toggle_type; //True if the button is a toggle on/toggle off type (for example the head light).
    boolean toggled;

    public function_button_touch_listener(int new_function, boolean new_toggle_type)
    {
      function=new_function;
      is_toggle_type=new_toggle_type;
      if(function==function_button.DIRECTION) { toggled=true; }
      else { toggled=false; }
    }

    public boolean onTouch(View v, MotionEvent event)
    {
      switch(event.getAction())
      {
        case MotionEvent.ACTION_DOWN:
        {
          Message function_msg=Message.obtain();
          if(function==function_button.DIRECTION) { function_msg.what=message_type.DIRECTION; }
          else { function_msg.what=message_type.FUNCTION; }
          function_msg.arg1=function; //Don't care if this is a direction button.
          if(is_toggle_type)
          {
            toggled=!toggled;
            //The toggle/latch functionality is taken care of by the WiThrottle server. This might not be useful in certain
            //cases, but for now I'll let it stand.
            //function_msg.arg2=(toggled ? 1 : 0);
          }
          if(function==function_button.DIRECTION)
          {
            function_msg.arg2=(toggled ? 1 : 0);
            //Set the direction button's text value. TODO: internationalize this.
            Button dir_button=(Button)findViewById(R.id.button_dir);
            dir_button.setText(toggled ? "Fwd" : "Rev");
          }
          else {function_msg.arg2=1; }
          threaded_application app=(threaded_application)getApplication();
          app.thread.comm_msg_handler.sendMessage(function_msg);
          //Change the appearance of toggleable buttons to show the current function state.
          if(is_toggle_type && function!=function_button.DIRECTION)
          {
            if(toggled)
            {
              Drawable button_pressed=getResources().getDrawable(R.drawable.btn_default_pressed);
              v.setBackgroundDrawable(button_pressed);
            }
            else
            {
              Drawable button_pressed=getResources().getDrawable(R.drawable.btn_default_normal);
              v.setBackgroundDrawable(button_pressed);
            }
          }
        }
        break;
        case MotionEvent.ACTION_UP:
          //if(!is_toggle_type)
          //{
          if(function!=function_button.DIRECTION)
          {
            Message function_msg=Message.obtain();
            function_msg.what=message_type.FUNCTION;
            function_msg.arg1=function;
            function_msg.arg2=0;
            threaded_application app=(threaded_application)getApplication();
            app.thread.comm_msg_handler.sendMessage(function_msg);
          }
        break;
      }
      return(false);
    };
  }

  public class throttle_listener implements SeekBar.OnSeekBarChangeListener
  {
    @Override
    public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser)
    {
      Message velocity_msg=Message.obtain();
      velocity_msg.what=message_type.VELOCITY;
      velocity_msg.arg1=speed;
      threaded_application app=(threaded_application)getApplication();
      app.thread.comm_msg_handler.sendMessage(velocity_msg);
      TextView speed_label=(TextView)findViewById(R.id.speed_value_label);
      speed_label.setText(Integer.toString(speed));
    }

    @Override
    public void onStartTrackingTouch(SeekBar sb) { }

    @Override
    public void onStopTrackingTouch(SeekBar sb) { }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.throttle);

    //Set callbacks for the widgets.
    Button button=(Button)findViewById(R.id.button_0);
    function_button_touch_listener fbtl=new function_button_touch_listener(0, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_1);
    fbtl=new function_button_touch_listener(1, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_2);
    fbtl=new function_button_touch_listener(2, false);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_3);
    fbtl=new function_button_touch_listener(3, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_4);
    fbtl=new function_button_touch_listener(4, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_5);
    fbtl=new function_button_touch_listener(5, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_6);
    fbtl=new function_button_touch_listener(6, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_7);
    fbtl=new function_button_touch_listener(7, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_8);
    fbtl=new function_button_touch_listener(8, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_9);
    fbtl=new function_button_touch_listener(9, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_10);
    fbtl=new function_button_touch_listener(10, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_11);
    fbtl=new function_button_touch_listener(11, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_12);
    fbtl=new function_button_touch_listener(12, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_13);
    fbtl=new function_button_touch_listener(13, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_14);
    fbtl=new function_button_touch_listener(14, true);
    button.setOnTouchListener(fbtl);
    button=(Button)findViewById(R.id.button_dir);
    fbtl=new function_button_touch_listener(function_button.DIRECTION, true);
    button.setOnTouchListener(fbtl);
/*    EditText entry=(EditText)findViewById(R.id.host_ip);
    entry.setOnFocusChangeListener(new entry_listener());
    entry=(EditText)findViewById(R.id.port);
    entry.setOnFocusChangeListener(new entry_listener());
    entry=(EditText)findViewById(R.id.loco_address);
    entry.setOnFocusChangeListener(new entry_listener());*/

    SeekBar sb=(SeekBar)findViewById(R.id.speed);
    sb.setMax(126);
    sb.setOnSeekBarChangeListener(new throttle_listener());
  }
}
