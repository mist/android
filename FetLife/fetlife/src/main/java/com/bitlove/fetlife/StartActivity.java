package com.bitlove.fetlife;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bitlove.fetlife.inbound.onesignal.notification.OneSignalNotification;
import com.bitlove.fetlife.session.UserSessionManager;
import com.bitlove.fetlife.view.screen.resource.ConversationsActivity;
import com.bitlove.fetlife.view.screen.resource.FeedActivity;
import com.bitlove.fetlife.view.screen.standalone.LoginActivity;

import java.util.Locale;

/**
 * Default Start Activity to make Activity title and App name independent
 */
public class StartActivity extends Activity {

    public static final String TAG = "StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OneSignalNotification.Companion.clearNotifications(null, null);

        UserSessionManager userSessionManager = getFetLifeApplication().getUserSessionManager();
        if (userSessionManager.getCurrentUser() == null) {
            LoginActivity.startLogin(getFetLifeApplication());
        } else {

            String localeString = getFetLifeApplication().getUserSessionManager().getActiveUserPreferences().getString(getString(R.string.settings_key_general_language),null);
            if (localeString != null && ! localeString.isEmpty()) {
                Log.d(TAG, "onCreate: localeString = " + localeString + "; changing language");
                this.setLocale(localeString);
            } else {
                Log.d(TAG, "onCreate: localeString = " + localeString);
            }

            if (getFetLifeApplication().getUserSessionManager().getActiveUserPreferences().getBoolean(getString(R.string.settings_key_general_feed_as_start),false)) {
                FeedActivity.startActivity(this);
            } else {
                ConversationsActivity.startActivity(this, false);
            }
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    protected FetLifeApplication getFetLifeApplication() {
        return (FetLifeApplication) getApplication();
    }

    private void setLocale(String localeString){
        Context context = getApplicationContext();
        Resources res = context.getResources();
        // Change locale settings in the app.
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = new Locale(localeString);
        res.updateConfiguration(conf, dm);
    }
}
