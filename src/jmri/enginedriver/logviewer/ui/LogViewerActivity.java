package jmri.enginedriver.logviewer.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.about_page;
import jmri.enginedriver.connection_activity;
import jmri.enginedriver.power_control;
import jmri.enginedriver.preferences;
import jmri.enginedriver.routes;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.turnouts;
import jmri.enginedriver.web_activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

//import jmri.enginedriver.logviewer.R;

public class LogViewerActivity extends ListActivity{
	private LogStringAdaptor adaptor = null;
	private ArrayList<String> logarray = null;
	private LogReaderTask logReaderTask = null;
	private threaded_application mainapp;  // hold pointer to mainapp
	private Menu LMenu;


	public void setTitleToIncludeThrotName()
	{
		SharedPreferences prefs  = getSharedPreferences("jmri.enginedriver_preferences", 0);
		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
		setTitle("LogViewerActivity" + "    |    Throttle Name: " + 
				prefs.getString("throttle_name_preference", defaultName));
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)getApplication();
		if(mainapp.isForcingFinish()) {		// expedite
			return;
		}
		setContentView(R.layout.log_main);

		setTitleToIncludeThrotName();

		logarray = new ArrayList<String>();
		adaptor = new LogStringAdaptor(this, R.id.txtLogString, logarray);

		setListAdapter(adaptor);

		logReaderTask = new LogReaderTask();

		logReaderTask.execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(mainapp.isForcingFinish()) {		//expedite
			this.finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.logviewer_menu, menu);
		LMenu = menu;
		mainapp.displayEStop(LMenu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		Intent in;
		switch (item.getItemId()) {
		case R.id.throttle_mnu:
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.turnouts_mnu:
			this.finish();
			in=new Intent().setClass(this, turnouts.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.routes_mnu:
			in = new Intent().setClass(this, routes.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			this.finish();
			in=new Intent().setClass(this, web_activity.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.preferences_mnu:
			in=new Intent().setClass(this, preferences.class);
			startActivityForResult(in, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.power_control_mnu:
			this.finish();
			in=new Intent().setClass(this, power_control.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.about_mnu:
			this.finish();
			in=new Intent().setClass(this, about_page.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			mainapp.sendEStopMsg();
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onDestroy() {
		logReaderTask.stopTask();

		super.onDestroy();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final AlertDialog.Builder builder = new AlertDialog.Builder(LogViewerActivity.this);
		String text = ((String) ((TextView)v).getText());

		builder.setMessage(text);

		builder.show();
	}

	private int getLogColor(String type) {
		int color = Color.WHITE;

		/*  some of these colors do not show up well
  		if(type.equals("D"))
		{
			color = Color.rgb(0, 0, 200);
		}
		else if(type.equals("W"))
		{
			color = Color.rgb(128, 0, 0);
		}
		else if(type.equals("E"))
		{
			color = Color.rgb(255, 0, 0);;
		}
		else if(type.equals("I"))
		{
			color = Color.rgb(0, 128, 0);;
		}
		 */		

		return color;
	}

	private class LogStringAdaptor extends ArrayAdapter<String>{
		private List<String> objects = null;

		public LogStringAdaptor(Context context, int textviewid, List<String> objects) {
			super(context, textviewid, objects);

			this.objects = objects;
		}

		@Override
		public int getCount() {
			return ((null != objects) ? objects.size() : 0);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public String getItem(int position) {
			return ((null != objects) ? objects.get(position) : null);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if(null == view)
			{
				LayoutInflater vi = (LayoutInflater)LogViewerActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.logitem, null);
			}

			String data = objects.get(position);

			if(null != data)
			{
				TextView textview = (TextView)view.findViewById(R.id.txtLogString);
				String type = data.substring(0, 1);
				//				String line = data.substring(2);
				//				textview.setText(line);
				textview.setText(data);
				textview.setTextColor(getLogColor(type));
				return view;
			}
			return null;

		}
	}

	private class LogReaderTask extends AsyncTask<Void, String, Void>
	{
		private final String[] LOGCAT_CMD = new String[] { "logcat", "Engine_Driver:D", "*:S" };
		//		private final int BUFFER_SIZE = 1024;

		private boolean isRunning = true;
		private Process logprocess = null;
		private BufferedReader reader = null;
		private String line = "";
		//		private String lastLine = "";

		@Override
		protected Void doInBackground(Void... params) {
			try {
				logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);
			} catch (IOException e) {
				e.printStackTrace();

				isRunning = false;
			}

			try {
				//				reader = new BufferedReader(new InputStreamReader(
				//						logprocess.getInputStream()),BUFFER_SIZE);
				reader = new BufferedReader(new InputStreamReader(
						logprocess.getInputStream()));
			}
			catch(IllegalArgumentException e){
				e.printStackTrace();

				isRunning = false;
			}

			line = "";
			//			lastLine = new String;

			try {
				while(isRunning)
				{
					line = reader.readLine();
					publishProgress(line);
				}
			} 
			catch (IOException e) {
				e.printStackTrace();

				isRunning = false;
			}

			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			//			if ((values[0] != null) && !values[0].equals(lastLine)) {
			if ((values[0] != null)) {
				adaptor.add(values[0]);
			}
			//			lastLine = values[0];
		}

		public void stopTask(){
			isRunning = false;
			logprocess.destroy();			
		}
	}
}