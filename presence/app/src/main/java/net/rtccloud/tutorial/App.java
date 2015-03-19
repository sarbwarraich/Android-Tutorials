package net.rtccloud.tutorial;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import net.rtccloud.sdk.Logger;

import android.app.Application;

public class App extends Application {

    public static String sUid;

    public static String sDisplayName;

    public static String sToken;

    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.setGlobalLevel(Logger.LoggerLevel.VERBOSE);
        mRequestQueue = Volley.newRequestQueue(this);
    }

    public RequestQueue requestQueue() {
        return mRequestQueue;
    }

}
