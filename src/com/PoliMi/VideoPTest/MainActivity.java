package com.PoliMi.VideoPTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements SurfaceHolder.Callback, OnPreparedListener {


	private TextView textView;
	private String server_url="http://git.rmdesign.it:8080";
	private String test_url= server_url + "/test";
	private String plugUsbURL = server_url+"/power/on";
	private String unPlugUsb = server_url+"/power/off";
	private String startTest = server_url+"/start-test";
	private String report = server_url+"/completed-test";
 

	//Test JSONObject
	private JSONObject testJson;

	//JSON Node names
	private static final String TAG_ID = "id";
	private static final String TAG_DATA = "data";
	private static final String TAG_URL = "media";
	private static final String TAG_LENGTH = "max_length";
	private static final String TAG_BRIGHTNESS = "brightness";
	private static final String TAG_NETWORK = "network";
	private static final String TAG_SIGNAL_STR = "signal_strenght";
	private static final String TAG_VOLUME = "volume";
	private static final String TAG_STARTED = "started";
	private static final String TAG_COMPLETED = "completed";
	private static final String TAG_CREATED_AT = "created_at";
	private static final String TAG_UPDATED_AT = "updated_at";
	private static final String TAG_STATUS = "status";

	//Test values
	private int id;
	private int test_id;
	private String url;
	private String network_type;
	private int brightness;
	private int signal_strength;
	private int volume;
	private int batteryLevel;
	private String started;
	private String completed;
	private String created_at;
	private String updated_at;
	private String status="ok";
	private int max_length;
	private int voltage_before;
	
	String batteryLog;
	
	//Variable for resources
	Resources res;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView = (TextView) findViewById(R.id.myText);
		ActionBar actionBar = getSupportActionBar();
		//Initializing receiver for low battery
		registerReceiver(mBatInfoReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_LOW));
		res = getResources();
	}

	@Override
	protected void onStart(){
		super.onStart();	


		Context context = getApplicationContext();        
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);        
		batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float batteryPct = (batteryLevel * 100) / (float)scale;

		// Are we charging / charged?
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

		//We firt check if we are not charging. If so, we only need to see if we are above or under 14%
		if (isCharging == false){
			if(batteryPct <= 14)
				new plugUsb().execute(); //if we are not charging and the battery lvl is under the 14% we ask for power
		}
		//This else will be triggered if we are charging, so we have to check if the battery is above or under 50%		
		else
		{ 
			if(batteryPct < 80)    	
				//if under 80% wait for reaching the 80%
				new charging().execute();
			else 
				new unPlugUsb().execute();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			final EditText input = new EditText(this);
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("Set Server URL")
			.setView(input)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					server_url = new String("http://"+value);
					test_url= server_url + "/test";
					plugUsbURL = server_url+"/power/on";
					unPlugUsb = server_url+"/power/off";
					report = server_url+"/completed/test";
					startTest = server_url+"/start-test";
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing.
				}
			}).show();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}

	//Receiver for low battery state
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra("level", 0);
			// CONNECT TO SERVER AND ASK FOR POWER RECONNECTION
			new AlertDialog.Builder(context)
			.setTitle("Low Battery, test aborted")
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) { 
					dialog.dismiss();
				}
			})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();
			new plugUsb().execute();
		}	        
	};

	//method called upon button click
	public void startTest(View view){
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			int duration = Toast.LENGTH_SHORT;
			Toast.makeText(getApplicationContext(), "Connection found", duration).show();
			new getTest().execute();
		} else {
			new waitConnection().execute();       
		}
	}


	//Checks what is the type of URL(youtube or other) and instantiate the relative view to show it
	public void streamVideo(View view){		

		Context context = getApplicationContext();
		Pattern p = Pattern.compile(".*?youtube\\.(com|it|fr).*");
		Matcher m = p.matcher(url);
		if(m.matches()) {
			//instantiate the youtube activity
			url = url.substring(32, 43); //the youtube player only needs the youtube code, that is 32 chars after the start of the youtube address
			Intent youtubeIntent = new Intent(this, YoutubePlayerActivity.class);
			youtubeIntent.putExtra("url",url);
			youtubeIntent.putExtra("max_length", max_length);
			startActivityForResult(youtubeIntent,0);}
		else{//instantiate the mediaPlayerActivity
			Intent mediaPlayerIntent = new Intent(this, MediaPlayerActivity.class);
			mediaPlayerIntent.putExtra("url", url);
			mediaPlayerIntent.putExtra("max_length", max_length);
			startActivityForResult(mediaPlayerIntent,0);    
		}

	}

	//Checks if the test was completed or not
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		//We take the batteryLog
		if (resultCode == RESULT_CANCELED) 	
			new plugUsb().execute();
		else  
			{	
				batteryLog = new String(data.getStringExtra("batteryLog"));
				new sendReport().execute();
			}

	}

	/**
	 * Async task class used to wait for connection
	 * */
	private class waitConnection extends AsyncTask<Void, Void, Void>{

		ProgressDialog pDialog;


		@Override
		protected void onPreExecute(){
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("No connection available! Searching for a connection...");
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected Void doInBackground(Void... arg0){
			ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			networkInfo = connMgr.getActiveNetworkInfo();
			while(networkInfo == null || !networkInfo.isConnected())
				networkInfo = connMgr.getActiveNetworkInfo();
			return null;			
		}

		@Override
		protected void onPostExecute(Void result){
			int duration = Toast.LENGTH_SHORT;
			Toast.makeText(getApplicationContext(), "Connection found", duration).show();	
			new getTest().execute();
		}

	}	

	/**
	 * Async task class to check connectivity and get json by making HTTP call
	 * */
	private class getTest extends AsyncTask<Void, Void, Void> {

		AlertDialog pDialog;

		boolean flag = false;

		//Checks for internet connection. If no connection available waits until one is available
		@Override
		protected void onPreExecute() {
			super.onPreExecute();      	
			// Showing progress dialog
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Please wait,retrieving test info...");
			pDialog.setCancelable(false);
			pDialog.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// Creating service handler class instance
			ServiceHandler sh = new ServiceHandler();

			// Making a request to url and getting response. If the Url is valid the parsing is started, if not a popup inform the user to insert a valid URL
			String jsonStr=null;
			try {
				jsonStr = sh.makeServiceCall(test_url, ServiceHandler.GET);
				flag = true;
			} catch (Exception e) {
				flag = false;			
			}
			if(flag == true){
				Log.d("Response: ", "> " + jsonStr);

				if (jsonStr != null) {
					try {

						testJson = new JSONObject(jsonStr);
						AudioManager audioManager;
						audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
						int max_volume = 0;                     
						status = testJson.getString(TAG_STATUS); 
						if (status.equals("success"))
						{
							JSONObject dataJson = testJson.getJSONObject(TAG_DATA);
							id = dataJson.getInt(TAG_ID);
							//We must save the id into the GlobalVariable class in order not to loose it when video activity is called
							GlobalVariables globalVariables = (GlobalVariables) getApplication();
							globalVariables.setID(id);
							url = dataJson.getString(TAG_URL);                        
							network_type = dataJson.getString(TAG_NETWORK);
							brightness = dataJson.getInt(TAG_BRIGHTNESS);
							//Converting brightness from % to a value from 0 to 255
							brightness = (brightness * 255 / 100);
							signal_strength = dataJson.getInt(TAG_SIGNAL_STR);
							volume = dataJson.getInt(TAG_VOLUME);
							max_volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//getting the max possible value of volume 
							volume = (volume * max_volume / 100);//converting the volume from % to a value from 0 to max_value
							started = dataJson.getString(TAG_STARTED);
							completed = dataJson.getString(TAG_COMPLETED);
							created_at = dataJson.getString(TAG_CREATED_AT);
							updated_at = dataJson.getString(TAG_UPDATED_AT);
							max_length = dataJson.getInt(TAG_LENGTH);
							max_length = max_length * 1000;//conversion from seconds to milliseconds
							IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
							Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);        
							batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
							voltage_before = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
							//We must save the actual battery info into the GlobalVariable class in order not to loose it when video activity is called
							globalVariables.setStartingBatteryLvl(batteryLevel);
							globalVariables.setVoltage_before(voltage_before);							
							
							//Marking test as started to the server
							LinkedHashMap<String, Comparable> testComplete = new LinkedHashMap<String, Comparable>();
							testComplete.put("test-id", id);
							JSONObject testReport = new JSONObject(testComplete);
							HttpClient client = new DefaultHttpClient();
							HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
							HttpResponse response;	   
							try {
								HttpPost post = new HttpPost(startTest);
								StringEntity se = new StringEntity(testReport.toString());
								se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
								post.setEntity(se);
								response = client.execute(post);
							} catch (MalformedURLException e) {				
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					Log.e("ServiceHandler", "Couldn't get any data from the url");
					flag = false;
				}
				pDialog.dismiss();
			}
			return null;            
		}

		@Override
		protected void onPostExecute(Void result) {
			//if flag is true the test can start, otherwise prompt a error message to the user
			if(flag == true){
				if(status.equals("success")){
					super.onPostExecute(result);
					Context context = getApplicationContext();
					CharSequence text = "Test retrieved!";
					int duration = Toast.LENGTH_SHORT;
					Toast.makeText(context, text, duration).show();					
					runTest(textView);
				}
				else{
					new AlertDialog.Builder(MainActivity.this)
					.setTitle("An error as occourred")
					.setMessage("The server was not able to provide a valid test")
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) { 
							dialog.dismiss();
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				}
			}else{
				new AlertDialog.Builder(MainActivity.this)
				.setTitle("An error as occourred")
				.setMessage("The server URL is invalid or incorrect. Please go to settings and enter a valid one")
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) { 
						dialog.dismiss();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();

			}
			pDialog.dismiss();
		}



	}

	//Running streaming test
	//Setting brightness, network and volume first and then streaming the video
	public void runTest(View view){

		AlertDialog pDialog = null;
		AudioManager audioManager;

		//Setting brightness
		android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS,brightness);

		//Setting volume
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);

		//Setting specific connection type
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		Context context = getApplicationContext();
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (network_type.equals("wifi")){
			if(wifi.isWifiEnabled() == false){    	
				wifi.setWifiEnabled(true);//Turn on Wifi    			
			}
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			pDialog = ProgressDialog.show(MainActivity.this, "", "Connecting. Please wait...", true); 
			while (!mWifi.isConnected()){
				//Waiting for connection
				try {
					Thread.sleep(100);
					mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			if(wifi.isWifiEnabled() == true){    			
				wifi.setWifiEnabled(false);//Turn off Wifi    				
			}
			pDialog = ProgressDialog.show(MainActivity.this, "", "Connecting. Please wait...", true); 
			NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			while (!mMobile.isConnected()){
				//Waiting for connection
				try {
					Thread.sleep(100);
					mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}    		
		}

		pDialog.dismiss();
		streamVideo(textView);
	}

	//connects to server to turn usb power on
	//Returns only when batter has reached 80% of charge
	private class plugUsb extends AsyncTask<Void, Void, Void> {

		AlertDialog pDialog;

		boolean flag = false;

		//Checks for internet connection. If no connection available waits until one is available
		@Override
		protected void onPreExecute() {
			super.onPreExecute();      	
			// Showing progress dialog
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Connecting for power plug in and charging.....");
			pDialog.setCancelable(false);
			pDialog.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			HttpClient httpclient = new DefaultHttpClient();
			try {
				HttpResponse response = httpclient.execute(new HttpGet(plugUsbURL));
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			float batteryPct = level;
			while(batteryPct<=80){
				batteryStatus = getApplicationContext().registerReceiver(null, ifilter);        
				level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				batteryPct = (level * 100) / (float)scale;
				//This sleep is made in order to avoid too much battery consumption during charge
				try {
					Thread.sleep(res.getInteger(R.integer.sleepMsCharge));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null; 
		}

		@Override
		protected void onPostExecute(Void result) {
			//if flag is true the test can start, otherwise prompt a error message to the user          	
			pDialog.dismiss();
			new unPlugUsb().execute(); 
		}

	}


	//	//connects to server and ask for usb unplug.returns only when USB has been unplugged
	private class unPlugUsb extends AsyncTask<Void, Void, Void> {

		AlertDialog pDialog;

		boolean flag = false;

		//Checks for internet connection. If no connection available waits until one is available
		@Override
		protected void onPreExecute() {
			super.onPreExecute();      	
			// Showing progress dialog
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Connecting server for power unplug.....");
			pDialog.setCancelable(false);
			pDialog.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			//We need to keep calling the server until the power USB has been unplugged.This cycle will keep doing the http call
			while(true){
				HttpClient client = new DefaultHttpClient();
				HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
				HttpResponse response;	   
				try {
					HttpGet get = new HttpGet(unPlugUsb);
					response = client.execute(get);
				} catch (MalformedURLException e) {				
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Context context = getApplicationContext();        
				IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
				Intent batteryStatus = context.registerReceiver(null, ifilter);

				// Are we charging / charged?
				int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
				//After each call we check if the status has changed to not charging,If so we can exit from the cycle
				if (isCharging == false)
					return null;
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			//				//if flag is true the test can start, otherwise prompt a error message to the user          	
			pDialog.dismiss();
			new getTest().execute();
		}   	
	}


	/**
	 * Async task class used to send the report via HTTP POST through a JSON object
	 * */
	private class sendReport extends AsyncTask<Void, Void, Void>{

		ProgressDialog pDialog;


		@Override
		protected void onPreExecute(){
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Test completed. Sending report to server");
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			Context context = getApplicationContext();

			//Getting all info about the battery via Intent receiver
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = context.registerReceiver(null, ifilter);
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
			int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
			int tech = batteryStatus.getIntExtra(BatteryManager.EXTRA_TECHNOLOGY, -1);
			int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
			String imei = null;

			//Getting info about network(WIFI and 3G)
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);//wifi manager to check if wifi in enabled
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);//mobile network manager
			String wifi_status = null, mobile_status = null;
			String wifiSSID = null, mobileType = null;
			int wifiSpeed = -1;
			int wifiStrength = -1, mobileStrength = -1;
			if (wifiManager.isWifiEnabled()){
				NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);//wifi network info to get wifi status
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();//wifi connection info to retrieve informations like SSID & speed
				if (mWifi.isConnected()) 
					wifi_status = "Connected";
				else 
					wifi_status = "Not connected";
				wifiSSID = wifiInfo.getSSID();
				wifiSpeed = wifiInfo.getLinkSpeed();
				wifiStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 100);//calculating signal level on a 100 scale
			}
			else{	    	
				NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);//mobile network info to get mobile status

				if(mMobile.isConnected())
					mobile_status = "Connected";
				else
					mobile_status = "Not connected";

				TelephonyManager teleMan = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				int networkType = teleMan.getNetworkType();
				switch (networkType) {
				case TelephonyManager.NETWORK_TYPE_1xRTT: mobileType = "1xRTT";
				case TelephonyManager.NETWORK_TYPE_CDMA: mobileType = "CDMA";
				case TelephonyManager.NETWORK_TYPE_EDGE: mobileType = "EDGE";
				case TelephonyManager.NETWORK_TYPE_EHRPD: mobileType = "eHRPD";
				case TelephonyManager.NETWORK_TYPE_EVDO_0: mobileType = "EVDO rev. 0";
				case TelephonyManager.NETWORK_TYPE_EVDO_A: mobileType = "EVDO rev. A";
				case TelephonyManager.NETWORK_TYPE_EVDO_B: mobileType = "EVDO rev. B";
				case TelephonyManager.NETWORK_TYPE_GPRS: mobileType = "GPRS";
				case TelephonyManager.NETWORK_TYPE_HSDPA: mobileType = "HSDPA";
				case TelephonyManager.NETWORK_TYPE_HSPA: mobileType = "HSPA";
				case TelephonyManager.NETWORK_TYPE_HSPAP: mobileType = "HSPA+";
				case TelephonyManager.NETWORK_TYPE_HSUPA: mobileType = "HSUPA";
				case TelephonyManager.NETWORK_TYPE_IDEN: mobileType = "iDen";
				case TelephonyManager.NETWORK_TYPE_LTE: mobileType = "LTE";
				case TelephonyManager.NETWORK_TYPE_UMTS: mobileType = "UMTS";
				case TelephonyManager.NETWORK_TYPE_UNKNOWN: mobileType = "Unknown";
				}

			}   	

			//Getting audio volume
			AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
			int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
			int max_volume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//getting the max possible value of volume
			volume_level = (volume_level * 100 / max_volume);//converting the volume from % to a value from 0 to max_value

			float batteryPct = (level * 100)/ scale;

			//getting brightness
			float brightness=0;
			try {
				brightness = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS) * 100 / 255;//getting battery lvl and calculating % of it
			} catch (SettingNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			//Getting device's IMEI
			TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			imei = mngr.getDeviceId();

			//Getting test id by the globalVariables class
			GlobalVariables globalVariables = (GlobalVariables) getApplication();

			//We now use a linkedHashMap to create a json object based on that hashmap
			LinkedHashMap<String, Comparable> testComplete = new LinkedHashMap<String, Comparable>();

			testComplete.put("status", "complete");				
			testComplete.put("imei",imei); //passing a unique identifier, we use IMEI in this case
			testComplete.put("brightness", brightness); //passing actual brightness
			testComplete.put("volume", volume_level);
			testComplete.put("battery used", (globalVariables.getStartingBatteryLvl() - batteryPct));
			testComplete.put("voltage", voltage);
			testComplete.put("temperature",temperature);
			testComplete.put("health", health);
			testComplete.put("technology", tech);
			testComplete.put("wifi status", wifi_status);
			testComplete.put("SSID", wifiSSID);
			testComplete.put("speed", wifiSpeed);
			testComplete.put("signal strength", wifiStrength);
			testComplete.put("mobile status", mobile_status);
			testComplete.put("mobile network type", mobileType);
			testComplete.put("test_id", globalVariables.getID());
			testComplete.put("voltage_before", globalVariables.getVoltage_before());
			testComplete.put("data", batteryLog);

			JSONObject testReport = new JSONObject(testComplete);

			//setting up http connection and sending test info to server
			HttpClient client = new DefaultHttpClient();
			HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
			HttpResponse response;	   
			try {
				HttpPost post = new HttpPost(report);
				StringEntity se = new StringEntity(testReport.toString());
				se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
				post.setEntity(se);
				response = client.execute(post);
				if(true);
			} catch (MalformedURLException e) {				
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;

		}

		@Override
		protected void onPostExecute(Void result){
			pDialog.dismiss();
			int duration = Toast.LENGTH_SHORT;
			Toast.makeText(getApplicationContext(), "Report correctly sent", duration).show();	
			new getTest().execute();
		}

	}

	
	//Returns only when batter has reached 80% of charge
	private class charging extends AsyncTask<Void, Void, Void> {

		AlertDialog pDialog;

		boolean flag = false;

		//Checks for internet connection. If no connection available waits until one is available
		@Override
		protected void onPreExecute() {
			super.onPreExecute();      	
			// Showing progress dialog
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Charging...");
			pDialog.setCancelable(false);
			pDialog.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			float batteryPct = level;
			while(batteryPct<=80){
				batteryStatus = getApplicationContext().registerReceiver(null, ifilter);        
				level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				batteryPct = (level * 100) / (float)scale;
				//This sleep is made in order to avoid too much battery consumption during charge
				try {
					Thread.sleep(res.getInteger(R.integer.sleepMsCharge));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null; 
		}

		@Override
		protected void onPostExecute(Void result) {
			//if flag is true the test can start, otherwise prompt a error message to the user          	
			pDialog.dismiss();
			new unPlugUsb().execute();
		}

	}


	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}


	@Override
	public void surfaceCreated(SurfaceHolder arg0) {


	}


	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onPrepared(MediaPlayer arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPause(){
		super.onPause();
		unregisterReceiver(mBatInfoReceiver);
	}

	@Override
	public void onResume(){
		super.onResume();
		registerReceiver(mBatInfoReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_LOW));
	}


}
