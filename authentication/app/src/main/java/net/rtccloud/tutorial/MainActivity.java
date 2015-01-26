package net.rtccloud.tutorial;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.rtccloud.sdk.Logger;
import net.rtccloud.sdk.Rtcc;
import net.rtccloud.sdk.RtccEngine;
import net.rtccloud.sdk.event.RtccEventListener;
import net.rtccloud.sdk.event.global.AuthenticatedEvent;
import net.rtccloud.sdk.event.global.ConnectedEvent;
import net.rtccloud.sdk.event.global.EngineStatusEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private RequestQueue mRequestQueue;

    private View mConnectionContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.setGlobalLevel(Logger.LoggerLevel.VERBOSE);
        setContentView(R.layout.activity_main);
        buildActionBar();

        mRequestQueue = Volley.newRequestQueue(this);
        mConnectionContainer = findViewById(R.id.connection_container);
        findViewById(R.id.connection_btn).setOnClickListener(this);

        invalidate();
    }

    /**
     * Build the ActionBar and its custom view
     */
    private void buildActionBar() {
        ActionBar actionBar = getSupportActionBar();
        ProgressBar progressBar = new ProgressBar(this);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(progressBar, new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END));
    }

    /**
     * Base method to invalidate the UI
     */
    private void invalidate() {
        invalidateActionBar();
        invalidateContainers();
        supportInvalidateOptionsMenu();
    }

    private void invalidateActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(Rtcc.getEngineStatus().name());
        actionBar.setSubtitle(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED ? (Config.sUid + " ~ " + Config.sDisplayName) : null);
        actionBar.setDisplayShowCustomEnabled(isLoading());
    }

    private void invalidateContainers() {
        mConnectionContainer.setVisibility(Rtcc.getEngineStatus() == RtccEngine.Status.UNDEFINED ? View.VISIBLE : View.GONE);
    }

    private static boolean isLoading() {
        RtccEngine.Status status = Rtcc.getEngineStatus();
        return status == RtccEngine.Status.INITIALIZING || status == RtccEngine.Status.AUTHENTICATING || status == RtccEngine.Status.NETWORK_LOST || status == RtccEngine.Status.DISCONNECTING;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Rtcc.eventBus().register(this);
        if (TextUtils.isEmpty(Config.APP_ID) || TextUtils.isEmpty(Config.AUTH_URL)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error_title)
                    .setMessage(R.string.error_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Rtcc.eventBus().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        RtccEngine.Status status = Rtcc.getEngineStatus();
        menu.findItem(R.id.action_disconnect).setVisible(status == RtccEngine.Status.AUTHENTICATED || status == RtccEngine.Status.CONNECTED);
        menu.findItem(R.id.action_sdk).setTitle(Html.fromHtml("v<b>" + Rtcc.getVersionFull(this) + "</b>"));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                Rtcc.instance().disconnect();
                return true;
            case R.id.action_sdk:
                Toast.makeText(this, net.rtccloud.sdk.Build.BUILD_DATE, Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connection_btn:
                Util.hideSoftKeyboard(mConnectionContainer);
                initialize();
                break;
        }
    }

    @RtccEventListener
    public void onEngineStatusEvent(EngineStatusEvent event) {
        invalidate();
    }

    @RtccEventListener
    public void onConnectedEvent(ConnectedEvent event) {
        if (event.isSuccess()) {
            requestToken();
        } else {
            if (event.getError() != ConnectedEvent.Error.CLOSED) {
                Toast.makeText(this, event.getError().name(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RtccEventListener
    public void onAuthenticatedEvent(AuthenticatedEvent event) {
        if (event.isSuccess()) {
            Rtcc.instance().setDisplayName(Config.sDisplayName);
        } else {
            Toast.makeText(this, event.getError().name(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initialize the SDK with the provided inputs
     */
    private void initialize() {
        Config.sUid = ((EditText) findViewById(R.id.connection_uid)).getText().toString();
        Config.sDisplayName = ((EditText) findViewById(R.id.connection_display_name)).getText().toString();
        Rtcc.initialize(Config.APP_ID, MainActivity.this);
    }

    /**
     * Request the token through at AUTH_URL, and authenticate the user
     */
    private void requestToken() {
        String url = String.format(Config.AUTH_URL, Config.sUid);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Config.sToken = response.getString("token");
                    Rtcc.instance().authenticate(MainActivity.this, Config.sToken, RtccEngine.UserType.INTERNAL);
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                if (!TextUtils.isEmpty(Config.AUTH_PWD)) {
                    headers.put("Authorization", "Basic " + Base64.encodeToString(Config.AUTH_PWD.getBytes(), Base64.DEFAULT));
                }
                return headers;
            }
        };
        mRequestQueue.add(request);
    }
}
