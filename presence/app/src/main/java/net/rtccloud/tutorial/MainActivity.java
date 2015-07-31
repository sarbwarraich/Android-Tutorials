package net.rtccloud.tutorial;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

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
import net.rtccloud.tutorial.adapter.PresenceListAdapter;
import net.rtccloud.tutorial.adapter.PresenceSpinnerAdapter;
import net.rtccloud.tutorial.model.Roster;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private View mConnectionContainer;

    private View mPresenceContainer;

    private EditText mPresenceUid;

    private Spinner mPresenceSpinner;

    private BaseAdapter mPresenceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buildActionBar();
        findViews();
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
     * Find views in Activity layout
     */
    private void findViews() {
        mConnectionContainer = findViewById(R.id.container_connection);
        findViewById(R.id.btn_connection).setOnClickListener(this);
        mPresenceContainer = findViewById(R.id.container_presence);
        mPresenceUid = (EditText) findViewById(R.id.txt_presence_uid);
        findViewById(R.id.btn_presence_set).setOnClickListener(this);
        findViewById(R.id.btn_roster_add).setOnClickListener(this);
        findViewById(R.id.btn_roster_remove).setOnClickListener(this);
        findViewById(R.id.btn_presence_check).setOnClickListener(this);
        findViewById(R.id.btn_presence_check_all).setOnClickListener(this);
        mPresenceSpinner = (Spinner) findViewById(R.id.spinner_presence);
        mPresenceSpinner.setAdapter(new PresenceSpinnerAdapter(this));
        mPresenceListAdapter = new PresenceListAdapter(this);
        ListView presenceList = (ListView) findViewById(R.id.list_presence);
        presenceList.setEmptyView(findViewById(R.id.list_presence_empty));
        presenceList.setAdapter(mPresenceListAdapter);
        presenceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPresenceUid.setText(((Roster.RosterEntry) mPresenceListAdapter.getItem(position)).uid);
            }
        });
    }

    /**
     * Invalidate the whole User Interface
     */
    private void invalidate() {
        invalidateActionBar();
        invalidateContainers();
        supportInvalidateOptionsMenu();
    }

    private void invalidateActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(Rtcc.getEngineStatus().name());
        actionBar.setSubtitle(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED ? (App.sUid + " ~ " + App.sDisplayName) : null);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        RtccEngine.Status status = Rtcc.getEngineStatus();
        menu.findItem(R.id.action_disconnect).setVisible(status == RtccEngine.Status.AUTHENTICATED || status == RtccEngine.Status.CONNECTED);
        menu.findItem(R.id.action_sdk).setTitle(Html.fromHtml("v<b>" + Rtcc.getVersionSDK() + "</b>"));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                Rtcc.instance().disconnect();
                return true;
            case R.id.action_sdk:
                Toast.makeText(this, Rtcc.getVersionFull(this) + "\n" + net.rtccloud.sdk.Build.BUILD_DATE, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Rtcc.eventBus().register(this);
        Util.detectConfigError(this);
    }

    @Override
    protected void onPause() {
        Rtcc.eventBus().unregister(this);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connection:
                Util.hideSoftKeyboard(mConnectionContainer);
                initialize();
                break;
            case R.id.btn_presence_set:
                setPresence();
                break;
            case R.id.btn_roster_add:
                addToRoster();
                break;
            case R.id.btn_roster_remove:
                removeFromRoster();
                break;
            case R.id.btn_presence_check:
                checkPresence();
                break;
            case R.id.btn_presence_check_all:
                checkRoster();
                break;
        }
    }

    /**
     * Initialize the SDK with the provided inputs
     */
    private void initialize() {
        App.sUid = ((EditText) findViewById(R.id.txt_connection_uid)).getText().toString();
        App.sDisplayName = ((EditText) findViewById(R.id.txt_connection_display_name)).getText().toString();
        Rtcc.initialize(Config.APP_ID, MainActivity.this);
    }

    /**
     * Request the token through at AUTH_URL, and authenticate the user
     */
    private void requestToken() {
        String url = String.format(Config.AUTH_URL, App.sUid);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    App.sToken = response.getString("token");
                    Rtcc.instance().authenticate(RtccEngine.UserType.internal(App.sToken));
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
        ((App) getApplication()).requestQueue().add(request);
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
            Util.hideSoftKeyboard(findViewById(R.id.txt_connection_uid));
            if (event.getError() != ConnectedEvent.Error.CLOSED) {
                Toast.makeText(this, event.getError().name(), Toast.LENGTH_SHORT).show();
            } else {
                Roster.clear();
                mPresenceListAdapter.notifyDataSetChanged();
            }
        }
    }

    @RtccEventListener
    public void onAuthenticatedEvent(AuthenticatedEvent event) {
        if (event.isSuccess()) {
            Rtcc.instance().setDisplayName(App.sDisplayName);
        } else {
            Toast.makeText(this, event.getError().name(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setPresence() {
        Rtcc.instance().presence().set(Integer.parseInt(String.valueOf(mPresenceSpinner.getSelectedItem())));
    }

    private void addToRoster() {
        Util.hideSoftKeyboard(mPresenceUid);
        final String uid = mPresenceUid.getText().toString();
        Roster.add(uid);
        Rtcc.instance().roster().add(uid);
        mPresenceListAdapter.notifyDataSetChanged();
    }

    private void removeFromRoster() {
        Util.hideSoftKeyboard(mPresenceUid);
        final String uid = mPresenceUid.getText().toString();
        Roster.remove(uid);
        mPresenceListAdapter.notifyDataSetChanged();
    }

    private void checkPresence() {
        Util.hideSoftKeyboard(mPresenceUid);
        Rtcc.instance().presence().request(mPresenceUid.getText().toString());
    }

    private void checkRoster() {
        Util.hideSoftKeyboard(mPresenceUid);
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
            Toast.makeText(this, R.string.toast_roster_updated, Toast.LENGTH_SHORT).show();
        } else {
            Roster.update(event.get());
            StringBuilder sb = new StringBuilder(getString(R.string.toast_presence_requested));
            for (Map.Entry<String, Integer> entry : event.get().entrySet()) {
                sb.append("\n").append(entry.getKey()).append(" ~ ").append(entry.getValue());
            }
            Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
        }
        mPresenceListAdapter.notifyDataSetChanged();
    }

    @RtccEventListener
    public void onPresenceUpdateEvent(PresenceUpdateEvent event) {
        Roster.update(event.getUid(), event.getValue());
        mPresenceListAdapter.notifyDataSetChanged();
    }

}
