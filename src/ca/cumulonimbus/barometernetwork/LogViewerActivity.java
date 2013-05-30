package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
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

public class LogViewerActivity extends Activity {

	TextView logText;

	boolean mBound;
	Messenger mService = null;

	private Messenger mMessenger = new Messenger(new IncomingHandler());

	Button oneHour;
	Button sixHours;
	Button oneDay;
	Button oneWeek;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);

			getRecents(24);
		}

		public void onServiceDisconnected(ComponentName className) {
			mMessenger = null;
			mBound = false;
		}
	};

	public void getRecents(long hoursAgo) {
		if (mBound) {
			CbApiCall api = new CbApiCall();
			api.setMinLat(-90);
			api.setMaxLat(90);
			api.setMinLon(-180);
			api.setMaxLon(180);
			api.setStartTime(System.currentTimeMillis()
					- (hoursAgo * 60 * 60 * 1000));
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

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_LOCAL_RECENTS:
				ArrayList<CbObservation> recents = (ArrayList<CbObservation>) msg.obj;
				try {

					String rawLog = "";
					for (CbObservation obs : recents) {
						Calendar c = Calendar.getInstance();
						c.setTimeInMillis(obs.getTime());
						String dateString = c.getTime().toLocaleString();
						DecimalFormat df = new DecimalFormat("####.00");
						String valueString = df.format(obs
								.getObservationValue());

						rawLog += dateString + ": " + valueString + "\n";
					}
					logText = (TextView) findViewById(R.id.editLog);
					logText.setText(rawLog);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
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

	class DataTabsListener implements ActionBar.TabListener {
		public Fragment fragment;

		public DataTabsListener(Fragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			Toast.makeText(getApplicationContext(), "Reselected!",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.replace(R.id.fragment_container, fragment);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.remove(fragment);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.logviewer);
		super.onCreate(savedInstanceState);

		// ActionBar gets initiated
		ActionBar actionbar = getActionBar();
		// Tell the ActionBar we want to use Tabs.
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// initiating both tabs and set text to it.
		ActionBar.Tab chartTab = actionbar.newTab().setText("Chart");
		ActionBar.Tab logTab = actionbar.newTab().setText("Log");

		// create the two fragments we want to use for display content
		Fragment chartFragment = new ChartFragment();
		Fragment logFragment = new LogFragment();

		// set the Tab listener. Now we can listen for clicks.
		chartTab.setTabListener(new DataTabsListener(chartFragment));
		logTab.setTabListener(new DataTabsListener(logFragment));

		// add the two tabs to the actionbar
		actionbar.addTab(chartTab);
		actionbar.addTab(logTab);

		oneHour = (Button) findViewById(R.id.buttonOneHour);
		sixHours = (Button) findViewById(R.id.buttonSixHours);
		oneDay = (Button) findViewById(R.id.buttonOneDay);
		oneWeek = (Button) findViewById(R.id.buttonOneWeek);

		oneHour.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getRecents(1);
			}
		});

		sixHours.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getRecents(6);
			}
		});
		oneDay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getRecents(24);
			}
		});
		oneWeek.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getRecents(24 * 7);
			}
		});

		bindCbService();
	}

	@Override
	protected void onPause() {
		unBindCbService();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unBindCbService();
		super.onDestroy();
	}

}
