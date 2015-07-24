package es.uni_freiburg.de.wrifd;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class WrifdService extends Service {
    protected static final String LIVE_CARD_TAG = "WRIFD_SERVICE";
    public static String mUid;
    protected LiveCard mLiveCard;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothManager mBluetoothManager;
    protected RemoteViews mViews;
    protected WrifdDevice mWrifdDevice;
    protected SharedPreferences mPrefs;

    protected BroadcastReceiver onRfidStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(WrifdDevice.RFID_STATE_CHANGE_EXTRA, -1);

            log("set_state : " + state);
            switch (state) {
                case WrifdDevice.NO_BLUETOOTH:
                    mViews.setTextColor(R.id.radioactive, Color.rgb(127,127,127));
                    break;
                case WrifdDevice.BLUETOOTH_AVAIL:
                    mViews.setTextColor(R.id.radioactive, Color.rgb(255,0,0));
                    break;
                case WrifdDevice.CONNECTED:
                    mViews.setTextColor(R.id.radioactive, Color.rgb(0,255,0));
                    break;
            }

            mLiveCard.setViews(mViews);
        }
    };

    private Handler mHandler;
    private TimeSinceUpdater mTimeSinceUpdater;
    protected BroadcastReceiver onRfidDetected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WrifdService.mUid = intent.getStringExtra(WrifdDevice.RFID_DETECTED_UID_EXTRA);
            log("rfid detected" + mUid);
            displayMemnonic();
        }
    };
    private Intent menuIntent;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharePrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
            if (mUid == null || s == null)
                return;

            if (!s.equals(mUid))
                return;

            displayMemnonic();
        }
    };

    private void displayMemnonic() {
        String memnonic = mPrefs.getString(mUid.toString(), mUid.toString());
        mViews.setTextViewText(R.id.centerText, memnonic);

        mViews.setTextViewText(R.id.timesince, "just now");
        if (mTimeSinceUpdater!=null) mTimeSinceUpdater.cancel();
        mTimeSinceUpdater = new TimeSinceUpdater(1*1000);
        mLiveCard.setViews(mViews);
    }

    private void log(String s) {
        Log.d(WrifdService.class.getName(), s);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mPrefs = getApplicationContext().getSharedPreferences("uids", Context.MODE_PRIVATE);

        if (mLiveCard == null) {
            mViews = new RemoteViews(this.getPackageName(),R.layout.mylayout);

            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCard.setViews(mViews);

            mWrifdDevice = new WrifdDevice(this);

            registerReceiver(onRfidStateChange, new IntentFilter(WrifdDevice.RFID_STATE_CHANGE));
            registerReceiver(onRfidDetected, new IntentFilter(WrifdDevice.RFID_DETECTED));

            mViews.setTextViewText(R.id.centerText, "Please read a tag.");

            menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
            mPrefs.registerOnSharedPreferenceChangeListener(mSharePrefListener);
        } else {
            mLiveCard.navigate();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

    public void onBluetooth() {

    }

    private class TimeSinceUpdater implements Runnable{
        private final Handler mHandler;
        private int period;
        private int elapsed;

        public TimeSinceUpdater(int period) {
            this.period = period;
            this.elapsed = 0;
            this.mHandler = new Handler(Looper.getMainLooper());
            this.mHandler.postDelayed(this,period);
        }

        @Override
        public void run() {
            String s = "";
            elapsed += period;

            if (elapsed < 59 * 1000)
                s = String.format("%d seconds ago", elapsed/1000);
            else if (elapsed >= 60 * 1000 && elapsed < 2*60*1000) {
                period = 60*1000;
                s = String.format("%d minute ago", elapsed / (60*1000));
            } else if (elapsed < 60*60*1000) {
                period = 60*1000;
                s = String.format("%d minute ago", elapsed / (60*1000));
            } else if (elapsed >= 60*60*1000 && elapsed < 2*60*60*1000) {
                period = 60*60*1000;
                s = String.format("%d hour ago", elapsed / (60*60*1000));
            } else {
                period = 60*60*1000;
                s = String.format("%d hours ago", elapsed / (60*60*1000));
            }

            mViews.setTextViewText(R.id.timesince, s);
            if (mLiveCard == null)
                return;
            mLiveCard.setViews(mViews);
            mHandler.postDelayed(this, period);
        }

        public void cancel() {
            mHandler.removeCallbacks(this);
        }
    }
}
