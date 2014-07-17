package com.PoliMi.VideoPTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import com.PoliMi.VideoPTest.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
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
	private String server_url="http://pastebin.com/";
	private String test_url= server_url + "raw.php?i=BUZDE0Pj";
	private String plugUsbURL = server_url+"/power/on";
	private String unPlugUsb = server_url+"power/off";
	private String report = server_url+"completed-test";
	//the content resolver used as a handle to the system's settings 
    private ContentResolver cResolver;
    
    //a window object, that will store a reference to the current window  
    private Window window;  
    
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
    private static final String TAG_CREATED_AT = "create_at";
    private static final String TAG_UPDATED_AT = "updated_at";
    private static final String TAG_STATUS = "status";

    //Test values
 	private int id;
	private int test_id;
	private String url;
	private float length;
	private String network_type;
	private int brightness;
	private int signal_strength;
	private int volume;
	private String started;
	private String completed;
	private String created_at;
	private String updated_at;
	private String status="ok";

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.myText);
        ActionBar actionBar = getSupportActionBar();
        //Initializing receiver for low battery
        registerReceiver(mBatInfoReceiver, new IntentFilter(
        	    Intent.ACTION_BATTERY_LOW));
    }
    
    @Override
    protected void onStart(){
    	super.onStart();	
        // Check if battery is > 50%
        Context context = getApplicationContext();        
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);        
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = (level * 100) / (float)scale;
        
        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    	boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        
    	if (isCharging == false){
    		if(batteryPct <= 50)
        		plugUsb(); //if we are not charging and the battery lvl is under the 50% we ask for power
    	}
    	//if we are here we are charging, so we have to check if the battery is above or under 50%
    	//if under 50% wait for reaching the 80%
    	else if(batteryPct <= 50){    		
    		while(batteryPct<=80){
            	batteryStatus = context.registerReceiver(null, ifilter);        
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryPct = (level * 100) / (float)scale;
            		}        	
        }
    	//if above 50% we unplug the usb and we cna start with testing
    	else unPlugUsb();
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
                   server_url = new String(value);
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
	        .setTitle("BATTERY IS TOO DAMN LOW!")
	        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) { 
	            	dialog.dismiss();
	            	}
	            })
	         .setIcon(android.R.drawable.ic_dialog_alert)
		     .show();
            plugUsb();
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
			Intent youtubeIntent = new Intent(this, YoutubePlayerActivity.class);
			youtubeIntent.putExtra("url",url);
			startActivityForResult(youtubeIntent,0);}
		else{//instantiate the mediaPlayerActivity
        Intent mediaPlayerIntent = new Intent(this, MediaPlayerActivity.class);
        mediaPlayerIntent.putExtra("url", url);
        startActivityForResult(mediaPlayerIntent,0);    
		}
		
	}
	
	//Checks if the test was completed or not
	@Override
	  protected void onActivityResult(int requestCode, int resultCode, Intent data)
	  {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (resultCode == RESULT_CANCELED) 	
	    	plugUsb();
	    else  new sendReport().execute();

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
 
            // Making a request to url and getting response. If the Url is valid the parsing is started, if not a popup inform the user to inset a valid URL
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
                    	
                    	JSONObject dataJson = testJson.getJSONObject(TAG_DATA);
                    	id = dataJson.getInt(TAG_ID);
                        url = dataJson.getString(TAG_URL);                        
                        network_type = dataJson.getString(TAG_NETWORK);
                        brightness = dataJson.getInt(TAG_BRIGHTNESS);
                        //Converting brightness from % to a value from 0 to 255
                    	brightness = (brightness * 255 / 100);
                        signal_strength = dataJson.getInt(TAG_SIGNAL_STR);
                        volume = dataJson.getInt(TAG_VOLUME);
                        max_volume = audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC);//getting the max possible value of volume 
                    	volume = (volume * max_volume / 100);//converting the volume from 5 to a value from 0 to max_value
                        started = dataJson.getString(TAG_STARTED);
                        completed = dataJson.getString(TAG_COMPLETED);
                        created_at = dataJson.getString(TAG_CREATED_AT);
                        updated_at = dataJson.getString(TAG_UPDATED_AT);
                    
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
            super.onPostExecute(result);
            Context context = getApplicationContext();
            CharSequence text = "Test retrieved!";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
            runTest(textView);
        	}else{
        		new AlertDialog.Builder(MainActivity.this)
    	        .setTitle("An error as occourred")
    	        .setMessage("The server URL is invalid or incorrect. Please go to settings and enter a valid one")
    	        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
    //tested and working
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
    
    //connects to serve to turn usb power on
    //Returns only when batter has reached 80% of charge
    public void plugUsb(){
    	boolean unPlug = false;
    	Context context = getApplicationContext();
        CharSequence text = "Connecting to server for USB plug";
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(context, text, duration).show();
//    	HttpClient httpclient = new DefaultHttpClient();
//        try {
//			HttpResponse response = httpclient.execute(new HttpGet(plugUsb));
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//       Context context = getApplicationContext();
//    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//    	Intent batteryStatus = context.registerReceiver(null, ifilter);
//    	int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
//    	float batteryPct = level;
//      while(batteryPct<=80){
//        	batteryStatus = context.registerReceiver(null, ifilter);        
//            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
//            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
//            batteryPct = (level * 100) / (float)scale;
//        		}
//      unPlugUsb();
    	
        
    }
    
    //connects to server and ask for usb unplug.returns only when USB has been unplugged
    public void unPlugUsb(){
    	
//    	while(true){
//    	HttpClient httpclient = new DefaultHttpClient();
//		try {
//		HttpResponse response = httpclient.execute(new HttpGet(unPlugUsb));
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//    	Context context = getApplicationContext();        
//        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        Intent batteryStatus = context.registerReceiver(null, ifilter);
//        
//    	// Are we charging / charged?
//        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//    	boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
//        if (isCharging == false)
//        		return;
//    	}
//    	
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
		protected Void doInBackground(Void... arg0){
			Context context = getApplicationContext();
	    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	    	Intent batteryStatus = context.registerReceiver(null, ifilter);
	    	int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	    	int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	    	String imei = null;

	    	float batteryPct = (level * 100)/ scale;
	    	float brightness =  (getWindow().getAttributes().screenBrightness / 255) * 100;
	    	
	    	//Getting device's IMEI
	    	TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	    	imei = mngr.getDeviceId();   
	    	
	    	//Now we execute a shell command to execute dumpsys and get all the info about the battery
	    	StringBuffer output = new StringBuffer();
	        Process p;
	        try {
	          p = Runtime.getRuntime().exec("adb shell dumpsys battery");//<<<---------check command if works
	          p.waitFor();
	          BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	          String line = "";
	          while ((line = reader.readLine())!= null) {
	            output.append(line + "\n");
	          }
	          
	        } catch (Exception e) {
	          e.printStackTrace();
	        }	        	       
	        String data = output.toString();
	        
	    	//Creates the json file to send back to server
	    	JSONObject testComplete = new JSONObject();
	    	try {
	    		testComplete.put("status", "complete");
				testComplete.put("Battery Level", batteryPct);
				testComplete.put("imei","imei"); //passing a unique identifier, we use IMEI in this case
				testComplete.put("brightness", brightness); //passing actual brightness
							
			} catch (JSONException e) {
				e.printStackTrace();
			}
	    	
	    	//setting up http connection and sending test info to server
	    	HttpClient client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
            HttpResponse response;	   
			try {
				 HttpPost post = new HttpPost("http://posttestserver.com/post.php");
				 StringEntity se = new StringEntity(testComplete.toString()+data);
                 se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                 post.setEntity(se);
                 response = client.execute(post);
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
