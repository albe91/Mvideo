

package com.PoliMi.VideoPTest;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayerView;

/**
 * Sample activity showing how to properly enable custom fullscreen behavior.
 * <p>
 * This is the preferred way of handling fullscreen because the default fullscreen implementation
 * will cause re-buffering of the video.
 */
public class YoutubePlayerActivity extends YouTubeFailureRecoveryActivity implements
    View.OnClickListener,
    CompoundButton.OnCheckedChangeListener,
    YouTubePlayer.OnFullscreenListener,
    YouTubePlayer.OnInitializedListener {

  private static final int PORTRAIT_ORIENTATION = Build.VERSION.SDK_INT < 9
      ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

  private LinearLayout baseLayout;
  private YouTubePlayerView playerView;
  private YouTubePlayer player;
  private View otherViews;
  
  private String videoCode;
  private int max_length;

  private boolean fullscreen;
  
  //This variable is used to control either if the video is running of not(since isPlayer method seems to be unreliable)
  private boolean controlPlaying;
  private boolean controlAsync;
  
  private checkBattery batteryAsync;
  
//Variable for resources
	Resources res;
  
//This is the hashmap used to save the battery changes. Placed here in order to send it back to previous activity
  LinkedHashMap<String, String> batteryLog = new LinkedHashMap<String, String>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    res= getResources();
    
    setContentView(R.layout.activity_youtube_player);
    baseLayout = (LinearLayout) findViewById(R.id.layout);
    playerView = (YouTubePlayerView) findViewById(R.id.player); 
    otherViews = findViewById(R.id.other_views);

    playerView.initialize(DeveloperKey.DEVELOPER_KEY, this);
    
    Intent intent = getIntent();
    videoCode = intent.getExtras().getString("url");
	max_length = intent.getExtras().getInt("max_length");
	
    doLayout();
    
  //Initializing receiver for low battery
  		registerReceiver(mBatInfoReceiver, new IntentFilter(
          	    Intent.ACTION_BATTERY_LOW));
  }

  @Override
  public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
      boolean wasRestored) {
    this.player = player;
    
    /** add listeners to YouTubePlayer instance **/
	player.setPlayerStateChangeListener(playerStateChangeListener);
	player.setPlaybackEventListener(playbackEventListener);

    setControlsEnabled();
    // Specify that we want to handle fullscreen behavior ourselves.
    player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
    player.setOnFullscreenListener(this);
    if (!wasRestored) {
      player.setFullscreen(!fullscreen);
      player.cueVideo(videoCode);
    }
  }

  @Override
  protected YouTubePlayer.Provider getYouTubePlayerProvider() {
    return playerView;
  }

  @Override
  public void onClick(View v) {
    player.setFullscreen(!fullscreen);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int controlFlags = player.getFullscreenControlFlags();
    if (isChecked) {
      // If you use the FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE, your activity's normal UI
      // should never be laid out in landscape mode (since the video will be fullscreen whenever the
      // activity is in landscape orientation). Therefore you should set the activity's requested
      // orientation to portrait. Typically you would do this in your AndroidManifest.xml, we do it
      // programmatically here since this activity demos fullscreen behavior both with and without
      // this flag).
      setRequestedOrientation(PORTRAIT_ORIENTATION);
      controlFlags |= YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE;
    } else {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
      controlFlags &= ~YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE;
    }
    player.setFullscreenControlFlags(controlFlags);
  }

  private void doLayout() {
    LinearLayout.LayoutParams playerParams =
        (LinearLayout.LayoutParams) playerView.getLayoutParams();
    if (fullscreen) {
      // When in fullscreen, the visibility of all other views than the player should be set to
      // GONE and the player should be laid out across the whole screen.
      playerParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
      playerParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;

      otherViews.setVisibility(View.GONE);
    } else {
      // This layout is up to you - this is just a simple example (vertically stacked boxes in
      // portrait, horizontally stacked in landscape).
      otherViews.setVisibility(View.VISIBLE);
      ViewGroup.LayoutParams otherViewsParams = otherViews.getLayoutParams();
      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        playerParams.width = otherViewsParams.width = 0;
        playerParams.height = WRAP_CONTENT;
        otherViewsParams.height = MATCH_PARENT;
        playerParams.weight = 1;
        baseLayout.setOrientation(LinearLayout.HORIZONTAL);
      } else {
        playerParams.width = otherViewsParams.width = MATCH_PARENT;
        playerParams.height = WRAP_CONTENT;
        playerParams.weight = 0;
        otherViewsParams.height = 0;
        baseLayout.setOrientation(LinearLayout.VERTICAL);
      }
      setControlsEnabled();
    }
  }

  private void setControlsEnabled() {
   
  }

  @Override
  public void onFullscreen(boolean isFullscreen) {
    fullscreen = isFullscreen;
    doLayout();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    doLayout();
  }
  
  private PlaybackEventListener playbackEventListener = new PlaybackEventListener() {
	  
		@Override
		public void onBuffering(boolean arg0) {

		}

		@Override
		public void onPaused() {

		}

		@Override
		public void onPlaying() {
			//THe video must end if the timer reaches 0
			if (max_length != 0){
				new CountDownTimer(max_length, 1000) {

				     @Override
					public void onTick(long millisUntilFinished) {
				        
				     }
		
				     @Override
					public void onFinish() {
						controlPlaying = false;// we declare the video has done
												// playing by modifying control
												// value
						while (controlAsync == true) {
						}
						// We give back the results as a String
						JSONObject temp = new JSONObject(batteryLog);
						String batteryResults = temp.toString();
						Intent returnIntent = new Intent();
						returnIntent.putExtra("batteryLog", batteryResults);
						YoutubePlayerActivity.this.setResult(RESULT_OK,
								returnIntent);// tells previous activity the
												// test was completed correctly
						finish();
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
		public void onSeekTo(int arg0) {

		}

		@Override
		public void onStopped() {

		}

	};

	private PlayerStateChangeListener playerStateChangeListener = new PlayerStateChangeListener() {

		@Override
		public void onAdStarted() {
		}

		@Override
		public void onError(ErrorReason arg0) {

		}

		@Override
		public void onLoaded(String arg0) {
			controlPlaying = true;
			batteryAsync = new checkBattery();
			batteryAsync.execute();
			//once the video is loaded,we can play it
			player.play();
			
		}

		@Override
		public void onLoading() {
		}

		@Override
		public void onVideoEnded() {
			controlPlaying = false;//we declare the video has done playing by modifying control value
			while(controlAsync == true){				
			}
			//We give back the results as a String
			JSONObject temp = new JSONObject(batteryLog);
			String batteryResults = temp.toString();
			Intent returnIntent = new Intent();
	    	returnIntent.putExtra("batteryLog", batteryResults);
	        YoutubePlayerActivity.this.setResult(RESULT_OK,returnIntent);//tells previous activity the test was completed correctly
			finish();
		}

		@Override
		public void onVideoStarted() {

		}
	};
  
	 //Receiver for low battery state
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);         
            Intent returnIntent = new Intent();
            YoutubePlayerActivity.this.setResult(RESULT_CANCELED,returnIntent);
            finish();
        }	        
    };
    
  //This asynctask registers every battery changes and saves them with relative timestamp
  	private class checkBattery extends AsyncTask<Void, Void, Void> {
        	
  		float oldBatteryLvl = 0;
  		String dateFormat ="yyyy-MM-dd HH:mm" ;
  			
  		@Override
  		protected void onPreExecute() {
  			super.onPreExecute();
  			controlAsync = true; 			

  		}

  		@Override
  		protected Void doInBackground(Void... arg0) {
  			
  			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
					e.printStackTrace();
			}
  			while(controlPlaying == true)//control tells us if video is playing or not
  				{
  				IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
  				Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);        
  				int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
  				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
  				float newBatteryLvl = (level * 100) / (float)scale;
  				
  				//We will now have to check if battery level is the same as the old one. if so, we do not save it, on the contrary yes.
  				if (!(newBatteryLvl == oldBatteryLvl)){
  					oldBatteryLvl = newBatteryLvl;
  					//Getting the timestamp
  					SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
  					Date date = new Date();
  					//We use an hashmap to save all the couples timestamp/BatteryLvl(which is converted in string to avoid error in the conversion to JSON					
  					batteryLog.put(sdf.format(date),String.valueOf(newBatteryLvl));		
  				}
  				
  				try {
  					Thread.sleep(res.getInteger(R.integer.sleepMsBatteryCheck));
  				} catch (InterruptedException e) {
  					// TODO Auto-generated catch block
  					e.printStackTrace();
  				}
  			}
  			controlAsync = false;  			
  			return null;
  		}

  		@Override
  		protected void onPostExecute(Void result) {
  			
  		}

  	}
    
  //Disable back press button to avoid unwanted interruption to playback during tests
  	@Override
  	public void onBackPressed() {
  	}

}
