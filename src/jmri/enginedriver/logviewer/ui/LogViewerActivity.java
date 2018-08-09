package jmri.enginedriver.logviewer.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
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
	private LogReaderTask logReaderTask = null;
	private threaded_application mainapp;  // hold pointer to mainapp


	public void setTitleToIncludeThrotName()
	{
		SharedPreferences prefs  = getSharedPreferences("jmri.enginedriver_preferences", 0);
		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
//		setTitle("LogViewerActivity" + "    |    Throttle Name: " +
//				prefs.getString("throttle_name_preference", defaultName));
		setTitle(getApplicationContext().getResources().getString(R.string.logViewerTitle).replace("%1$s",defaultName));
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)getApplication();
		if(mainapp.isForcingFinish()) {		// expedite
			return;
		}

		mainapp.applyTheme(this);
		setContentView(R.layout.log_main);

		setTitleToIncludeThrotName();

		ArrayList<String> logarray = new ArrayList<>();
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
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.logviewer_menu, menu);
		mainapp.displayEStop(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
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
		String text = ((TextView)v).getText().toString();

		builder.setMessage(text);

		builder.show();
	}

	private int getLogColor(String type) {

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

		return Color.WHITE;
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
			if (logprocess != null)	logprocess.destroy();
		}
	}
}