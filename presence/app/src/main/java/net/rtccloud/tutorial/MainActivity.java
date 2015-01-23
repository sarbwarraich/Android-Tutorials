package net.rtccloud.tutorial;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import net.rtccloud.sdk.event.global.RegistrationEvent;
import net.rtccloud.sdk.event.presence.PresenceRequestEvent;
import net.rtccloud.sdk.event.presence.PresenceUpdateEvent;
import net.rtccloud.sdk.event.roster.RosterEvent;
import net.rtccloud.tutorial.model.Presence;
import net.rtccloud.tutorial.model.Roster;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private RequestQueue mRequestQueue;

    private View mConnectionContainer;
    private View mPresenceContainer;

    private EditText mPresenceUid;
    private ListView mPresenceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.setGlobalLevel(Logger.LoggerLevel.VERBOSE);
        setContentView(R.layout.activity_main);
        buildActionBar();

        mRequestQueue = Volley.newRequestQueue(this);
        mConnectionContainer = findViewById(R.id.connection_container);
        mPresenceContainer = findViewById(R.id.presence_container);
        mPresenceUid = (EditText) findViewById(R.id.presence_uid);
        findViewById(R.id.connection_btn).setOnClickListener(this);
        findViewById(R.id.add_roster_btn).setOnClickListener(this);
        findViewById(R.id.check_presence_btn).setOnClickListener(this);
        findViewById(R.id.check_roster_btn).setOnClickListener(this);

        ArrayAdapter<Presence> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Presence.values());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) findViewById(R.id.presence_spinner);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED) {
                    Rtcc.instance().presence().set(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final ArrayAdapter<Roster.RosterEntry> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Roster.get());
        mPresenceList = (ListView) findViewById(R.id.presence_list);
        mPresenceList.setEmptyView(findViewById(R.id.presence_list_empty));
        mPresenceList.setAdapter(listAdapter);
        mPresenceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Roster.remove(listAdapter.getItem(position));
                listAdapter.notifyDataSetChanged();
            }
        });

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
        mPresenceContainer.setVisibility(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED ? View.VISIBLE : View.GONE);
    }

    private static boolean isLoading() {
        RtccEngine.Status status = Rtcc.getEngineStatus();
        return status == RtccEngine.Status.INITIALIZING || status == RtccEngine.Status.AUTHENTICATING || status == RtccEngine.Status.NETWORK_LOST || status == RtccEngine.Status.DISCONNECTING;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Rtcc.eventBus().register(this);
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
                Roster.clear();
                ((ArrayAdapter) mPresenceList.getAdapter()).notifyDataSetChanged();
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
            case R.id.add_roster_btn:
                addToRoster();
                break;
            case R.id.check_presence_btn:
                checkPresence();
                break;
            case R.id.check_roster_btn:
                checkRoster();
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

    private void addToRoster() {
        final String uid = mPresenceUid.getText().toString();
        Roster.add(uid);
        Rtcc.instance().roster().add(uid);
        ((ArrayAdapter) mPresenceList.getAdapter()).notifyDataSetChanged();
    }

    private void checkPresence() {
        Rtcc.instance().presence().request(mPresenceUid.getText().toString());
    }

    private void checkRoster() {
        Rtcc.instance().presence().request();
    }

    @RtccEventListener
    public void onRegistrationEvent(RegistrationEvent event) {
        Toast.makeText(this, event.getStatus().name(), Toast.LENGTH_SHORT).show();
    }

    @RtccEventListener
    public void onRosterEvent(RosterEvent event) {
        Toast.makeText(this, "RosterEvent: " + (event.getDelta() > 0 ? "+" : "") + event.getDelta() + " size: " + event.getSize(), Toast.LENGTH_SHORT).show();
    }

    @RtccEventListener
    public void onPresenceRequestEvent(PresenceRequestEvent event) {
        if (event.isRoster()) {
            Roster.updateRoster(event.get());
            Toast.makeText(this, R.string.roster_updated, Toast.LENGTH_SHORT).show();
        } else {
            Roster.update(event.get());
            StringBuilder sb = new StringBuilder(getString(R.string.requested_presence));
            for (Map.Entry<String, Integer> entry : event.get().entrySet()) {
                sb.append("\n").append(entry.getKey()).append(" ~ ").append(Presence.fromOrdinal(entry.getValue()));
            }
            Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
        }
        ((ArrayAdapter) mPresenceList.getAdapter()).notifyDataSetChanged();
    }

    @RtccEventListener
    public void onPresenceUpdateEvent(PresenceUpdateEvent event) {
        Roster.update(event.getUid(), event.getValue());
        ((ArrayAdapter) mPresenceList.getAdapter()).notifyDataSetChanged();
    }
}
