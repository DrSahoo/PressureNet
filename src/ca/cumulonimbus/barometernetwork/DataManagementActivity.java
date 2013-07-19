package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class DataManagementActivity extends Activity {

	Button buttonExportMyData;
	Button buttonClearMyData;
	Button buttonAdvancedAccess;
	Button buttonClearCache;

	TextView textMyData;
	TextView textDataCache;

	boolean mBound;
	Messenger mService = null;

	private Messenger mMessenger = new Messenger(new IncomingHandler());

	private void clearLocalCache() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_CLEAR_LOCAL_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
		}
	}

	public void getAllLocalObservations() {
		if (mBound) {
			CbApiCall api = new CbApiCall();
			api.setMinLat(-90);
			api.setMaxLat(90);
			api.setMinLon(-180);
			api.setMaxLon(180);
			api.setStartTime(0);
			api.setEndTime(System.currentTimeMillis());

			Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS,
					api);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
		}
	}

	private void askForCacheCounts() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_COUNT_API_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
		}
	}

	private void askForUserCounts() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_COUNT_LOCAL_OBS,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
		}
	}

	private void clearAPICache() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_CLEAR_API_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			System.out.println("dm bound");
			askForUserCounts();
			askForCacheCounts();
		}

		public void onServiceDisconnected(ComponentName className) {
			mMessenger = null;
			mBound = false;
		}
	};

	@Override
	protected void onResume() {
		if (mBound) {
			askForUserCounts();
			askForCacheCounts();
		} else {
			bindCbService();
		}
		super.onResume();
	}

	public void unBindCbService() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	public void bindCbService() {
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data_management);

		buttonExportMyData = (Button) findViewById(R.id.buttonExportMyData);
		buttonClearMyData = (Button) findViewById(R.id.buttonClearMyData);
		buttonAdvancedAccess = (Button) findViewById(R.id.buttonDataAccess);
		buttonClearCache = (Button) findViewById(R.id.buttonClearCache);

		textDataCache = (TextView) findViewById(R.id.textDataCacheDescription);
		textMyData = (TextView) findViewById(R.id.textMyDataDescription);

		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");

		TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);

		buttonExportMyData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getAllLocalObservations();
			}
		});

		buttonClearMyData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearLocalCache();
			}
		});

		buttonAdvancedAccess.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Open Live API sign up
				Uri uri = Uri
						.parse("http://pressurenet.cumulonimbus.ca/customers/livestream/");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});

		buttonClearCache.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearAPICache();
			}
		});

		bindCbService();
	}

	@Override
	protected void onPause() {
		unBindCbService();
		super.onPause();
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_COUNT_LOCAL_OBS_TOTALS:
				int count = msg.arg1;
				textMyData.setText("You have recorded and stored " + count
						+ " measurements.");
				break;
			case CbService.MSG_COUNT_API_CACHE_TOTALS:
				int countCache = msg.arg1;
				textDataCache.setText("You have cached " + countCache
						+ " measurements from our servers.");
				break;
			case CbService.MSG_LOCAL_RECENTS:
				ArrayList<CbObservation> recents = (ArrayList<CbObservation>) msg.obj;

				System.out.println("dma receiving local recents size " + recents.size());
				
				/*
				for (CbObservation obs : recents) {
					String data = obs.getTime() + ","
							+ obs.getObservationValue() + ","
							+ obs.getLocation().getLatitude() + ","
							+ obs.getLocation().getLongitude() + "\n";
				}
				*/

				Toast.makeText(
						getApplicationContext(),
						"TODO: save data data to: __, length "
								+ recents.size(), Toast.LENGTH_LONG).show();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

}
