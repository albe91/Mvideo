package com.PoliMi.VideoPTest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.PoliMi.VideoPTest.util.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MediaPlayerActivity extends Activity implements SurfaceHolder.Callback, OnPreparedListener,OnCompletionListener {
	
	private SurfaceHolder vidHolder;
	private SurfaceView vidSurface;
	private MediaPlayer mediaPlayer;
	AlertDialog dialog;
	private String videoUrl;
	private int max_length;
	
	private checkBattery batteryAsync;
	
	//Variable for resources
	Resources res;
	
	//This is the hashmap used to save the battery changes. Placed here in order to send it back to previous activity
	LinkedHashMap<String, String> batteryLog = new LinkedHashMap<String, String>();
	
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		setContentView(R.layout.activity_media_player);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		Intent intent = getIntent();
		videoUrl = intent.getExtras().getString("url");
		max_length = intent.getExtras().getInt("max_length");
		vidSurface = (SurfaceView) findViewById(R.id.surfView);
		vidHolder = vidSurface.getHolder();
		vidHolder.addCallback(this);		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, vidSurface,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					@Override
					public void onVisibilityChange(boolean visible) {
						// TODO Auto-generated method stub
						
					}

				});

		// Set up the user interaction to manually show or hide the system UI.
		vidSurface.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});	

		//Initializing receiver for low battery
		registerReceiver(mBatInfoReceiver, new IntentFilter(
        	    Intent.ACTION_BATTERY_LOW));
	}
	
	 //Receiver for low battery state
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);         
            Intent returnIntent = new Intent();
            MediaPlayerActivity.this.setResult(RESULT_CANCELED,returnIntent);
            finish();
        }	        
    };
    
	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		
		dialog.dismiss();
		mediaPlayer.start();
		batteryAsync = new checkBattery();
		batteryAsync.execute();
		if (max_length != 0){
			new CountDownTimer(max_length, 1000) {

			     @Override
				public void onTick(long millisUntilFinished) {
			        
			     }
	
			     @Override
				public void onFinish() {
			    	batteryAsync.cancel(true);	
			    	JSONObject temp = new JSONObject(batteryLog);
					String batteryResults = temp.toString();
					Intent returnIntent = new Intent();
					returnIntent.putExtra("batteryLog", batteryResults);
					MediaPlayerActivity.this.setResult(RESULT_OK, returnIntent);// tells previous activity the test was complted correctly			
					Context context = getApplicationContext();
					CharSequence text = "Max video length reached";
					int duration = Toast.LENGTH_LONG;
					Toast.makeText(context, text, duration).show();
					finish();
			     }
			}.start();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
        try {
		    mediaPlayer = new MediaPlayer();
		    mediaPlayer.setDisplay(vidHolder);
		    mediaPlayer.setDataSource(videoUrl);
		    mediaPlayer.prepareAsync();
		    mediaPlayer.setOnPreparedListener(this);
		    mediaPlayer.setOnCompletionListener(this);
		    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);		
			dialog = ProgressDialog.show(MediaPlayerActivity.this, "", "Connecting. Please wait...", true); 

		} 
		catch(Exception e){
		    e.printStackTrace();
		}
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub	
		mediaPlayer.stop();
	}
	
	@Override
	public void onCompletion(MediaPlayer mp){
		batteryAsync.cancel(true);
		JSONObject temp = new JSONObject(batteryLog);
		String batteryResults = temp.toString();
		Intent returnIntent = new Intent();
    	returnIntent.putExtra("batteryLog", batteryResults);
        MediaPlayerActivity.this.setResult(RESULT_OK,returnIntent);//tells previous activity the test was completed correctly
		finish();
		
	}
	
	//This asynctask registers every battery changes and saves them with relative timestamp
	private class checkBattery extends AsyncTask<Void, Void, Void> {
      	
		float oldBatteryLvl = 0;
		String dateFormat ="yyyy-MM-dd HH:mm" ;
			
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			while (true) {
				if (!isCancelled()) {
					IntentFilter ifilter = new IntentFilter(
							Intent.ACTION_BATTERY_CHANGED);
					Intent batteryStatus = getApplicationContext()
							.registerReceiver(null, ifilter);
					int level = batteryStatus.getIntExtra(
							BatteryManager.EXTRA_LEVEL, -1);
					int scale = batteryStatus.getIntExtra(
							BatteryManager.EXTRA_SCALE, -1);
					float newBatteryLvl = (level * 100) / (float) scale;

					// We will now have to check if battery level is the same as
					// the old one. if so, we do not save it, on the contrary
					// yes.
					if (!(newBatteryLvl == oldBatteryLvl)) {
						oldBatteryLvl = newBatteryLvl;
						// Getting the timestamp
						SimpleDateFormat sdf = new SimpleDateFormat(dateFormat,
								Locale.US);
						Date date = new Date();
						// We use an hashmap to save all the couples
						// timestamp/BatteryLvl
						batteryLog.put(sdf.format(date),
								String.valueOf(newBatteryLvl));
					}

					try {
						Thread.sleep(res
								.getInteger(R.integer.sleepMsBatteryCheck));
					} catch (InterruptedException e) {
						cancel(true);
						e.printStackTrace();
					}
				} else
					return null;
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			
		}

	}
	
	
	//Disable back press button
	@Override
	public void onBackPressed() {
	}
}
