package net.rtccloud.tutorial;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import net.rtccloud.sdk.Rtcc;
import net.rtccloud.sdk.RtccEngine;
import net.rtccloud.sdk.event.RtccEventListener;
import net.rtccloud.sdk.event.datachannel.DataChannelEvent;
import net.rtccloud.sdk.event.datachannel.DataChannelOutOfBandEvent;
import net.rtccloud.sdk.event.global.AuthenticatedEvent;
import net.rtccloud.sdk.event.global.ConnectedEvent;
import net.rtccloud.sdk.event.global.EngineStatusEvent;
import net.rtccloud.tutorial.adapter.ChatListAdapter;
import net.rtccloud.tutorial.model.Chat;

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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private View mConnectionContainer;

    private View mChatContainer;

    private EditText mChatUid;

    private EditText mChatMessage;

    private BaseAdapter mChatListAdapter;

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
        mChatContainer = findViewById(R.id.container_chat);
        mChatUid = (EditText) findViewById(R.id.txt_chat_uid);
        mChatMessage = (EditText) findViewById(R.id.txt_chat_message);
        findViewById(R.id.send_message_btn).setOnClickListener(this);

        mChatListAdapter = new ChatListAdapter(this);
        ListView chatList = (ListView) findViewById(R.id.list_chat);
        chatList.setEmptyView(findViewById(R.id.list_chat_empty));
        chatList.setAdapter(mChatListAdapter);
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
        mChatContainer.setVisibility(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED ? View.VISIBLE : View.GONE);
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
            case R.id.send_message_btn:
                sendMessage();
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
                Chat.clear();
                mChatListAdapter.notifyDataSetChanged();
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

    private void sendMessage() {
        Util.hideSoftKeyboard(mChatMessage);
        int id = (int) (Math.random() * Integer.MAX_VALUE);
        String uid = mChatUid.getText().toString();
        String message = mChatMessage.getText().toString();
        Chat.add(id, uid, message);
        mChatListAdapter.notifyDataSetChanged();
        Rtcc.instance().dataChannel().send(message.getBytes(), id, uid);
    }

    @RtccEventListener
    public void onDataChannelEvent(DataChannelEvent event) {
        Chat.add(event);
        mChatListAdapter.notifyDataSetChanged();
        if (event instanceof DataChannelOutOfBandEvent) {
            ((DataChannelOutOfBandEvent) event).ack(0);
        }
    }

}
