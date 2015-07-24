package es.uni_freiburg.de.wrifd;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.view.WindowUtils;

import java.util.ArrayList;

/**
 * Created by phil on 7/23/15.
 */
public class MenuActivity extends Activity {
    private final Handler mHandler = new Handler();
    private boolean mFromLiveCardVoice;
    private boolean isFinishing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       mFromLiveCardVoice =
                getIntent().getBooleanExtra(LiveCard.EXTRA_FROM_LIVECARD_VOICE, false);
        if (mFromLiveCardVoice) {
            // When activated by voice from a live card, enable voice commands. The menu
            // will automatically "jump" ahead to the items (skipping the guard phrase
            // that was already said at the live card).
            //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mFromLiveCardVoice) {
            openOptionsMenu();
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (isMyMenu(featureId)) {
            getMenuInflater().inflate(R.menu.wrifdmenu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (isMyMenu(featureId)) {
            // Don't reopen menu once we are finishing. This is necessary
            // since voice menus reopen themselves while in focus.
            return !isFinishing;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    private boolean isMyMenu(int featureId) {
        return featureId == Window.FEATURE_OPTIONS_PANEL ||
               featureId == WindowUtils.FEATURE_VOICE_COMMANDS;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.stop:
            stopService(new Intent(this, WrifdService.class));
            return true;
        case R.id.relabel:
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "please provide a label.");
            startActivityForResult(intent, 123);
            return true;
        case R.id.delete:
            if (WrifdService.mUid != null) {
                SharedPreferences prefs = getApplicationContext()
                        .getSharedPreferences("uids", Context.MODE_PRIVATE);
                prefs.edit().remove(WrifdService.mUid).commit();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        ArrayList<String> matches = data
                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("uids", Context.MODE_PRIVATE);

        if (matches == null)
            return;

        String label = matches.get(0);

        if (WrifdService.mUid == null  || label == null)
            return;

        boolean result = prefs.edit().putString(WrifdService.mUid,label).commit();
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
