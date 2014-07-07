package com.example.VideoPTest;

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
import android.text.Editable;
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
	
	private static final String DEBUG_TAG = "HttpExample";
	private TextView textView;
	private String server_url="http://pastebin.com/";
	private String powerOnURL="/power/on";
	//the content resolver used as a handle to the system's settings 
    private ContentResolver cResolver;
    
    //a window object, that will store a reference to the current window  
    private Window window;  
    
    //Test JSONObject
    private JSONObject testJson;
    
    //JSON Node names
    private static final String TAG_ID = "id";
    private static final String TAG_TEST_ID = "test_id";
    private static final String TAG_URL = "media";
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
        
        
    	if (isCharging == false && batteryPct <= 50)
        		reChargeBattery();        	
    	else if(batteryPct <= 50){
    		
    		while(batteryPct<=80){
            	batteryStatus = context.registerReceiver(null, ifilter);        
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryPct = (level * 100) / (float)scale;
            		}        	
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
            reChargeBattery();
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
    
   
	//Start video stream in a different view
	public void streamVideo(View view){		
		Context context = getApplicationContext();
       Intent mediaPlayerIntent = new Intent(this, MediaPlayerActivity.class);
       mediaPlayerIntent.putExtra("url", url);
       startActivityForResult(mediaPlayerIntent,0);     
		
	}
	
	//Checks if the test was completed or not
	@Override
	  protected void onActivityResult(int requestCode, int resultCode, Intent data)
	  {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (resultCode == RESULT_CANCELED) 	
	    	reChargeBattery();
	    else  new getTest().execute();

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
				jsonStr = sh.makeServiceCall(server_url+"raw.php?i=j9ts4KKH", ServiceHandler.GET);
				flag = true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			}
			if(flag == true){
            Log.d("Response: ", "> " + jsonStr);
 
            if (jsonStr != null) {
                try {
                    testJson = new JSONObject(jsonStr);
                    AudioManager audioManager;
                	audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
                	int max_volume = 0;    	
                         
                		//status = testJson.getString(TAG_STATUS);
                    	id = testJson.getInt(TAG_ID);
                    	test_id = testJson.getInt(TAG_TEST_ID);
                        url = testJson.getString(TAG_URL);
                        network_type = testJson.getString(TAG_NETWORK);
                        brightness = testJson.getInt(TAG_BRIGHTNESS);
                        //Converting brightness from % to a value from 0 to 255
                    	brightness = (brightness * 255 / 100);
                        signal_strength = testJson.getInt(TAG_SIGNAL_STR);
                        volume = testJson.getInt(TAG_VOLUME);
                        max_volume = audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC);//getting the max possible value of volume 
                    	volume = (volume * max_volume / 100);//converting the volume from 5 to a value from 0 to max_value
                        started = testJson.getString(TAG_STARTED);
                        completed = testJson.getString(TAG_COMPLETED);
                        created_at = testJson.getString(TAG_CREATED_AT);
                        updated_at = testJson.getString(TAG_UPDATED_AT);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            pDialog.dismiss();
			}
            return null;            
        }
 
        @Override
        protected void onPostExecute(Void result) {
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
    public void reChargeBattery(){
    	Context context = getApplicationContext();
        CharSequence text = "Connecting to server for USB plug";
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(context, text, duration).show();
//    	HttpClient httpclient = new DefaultHttpClient();
//        try {
//			HttpResponse response = httpclient.execute(new HttpGet(powerOnURL));
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//        Context context = getApplicationContext();
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
