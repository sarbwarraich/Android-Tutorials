package net.rtccloud.tutorial;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import net.rtccloud.sdk.Call;
import net.rtccloud.sdk.Contact;
import net.rtccloud.sdk.Rtcc;
import net.rtccloud.sdk.RtccEngine;
import net.rtccloud.sdk.event.RtccEventListener;
import net.rtccloud.sdk.event.call.StatusEvent;
import net.rtccloud.sdk.event.global.AuthenticatedEvent;
import net.rtccloud.sdk.event.global.ConnectedEvent;
import net.rtccloud.sdk.event.global.EngineStatusEvent;
import net.rtccloud.sdk.view.VideoInFrame;
import net.rtccloud.sdk.view.VideoOutPreviewFrame;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Configuration;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private View mConnectionContainer;

    private View mCallContainer;

    private View mCallCreateContainer;

    private View mCallOutboundContainer;

    private View mCallInboundContainer;

    private View mCallActiveContainer;

    private LinearLayout mCallVideoContainer;

    private VideoInFrame mVideoIn;

    private VideoOutPreviewFrame mVideoOut;

    private static Call sPendingCall;

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
        mCallContainer = findViewById(R.id.container_call);
        mCallCreateContainer = findViewById(R.id.container_call_create);
        mCallOutboundContainer = findViewById(R.id.container_call_outbound);
        mCallInboundContainer = findViewById(R.id.container_call_inbound);
        mCallActiveContainer = findViewById(R.id.container_call_active);
        mCallVideoContainer = (LinearLayout) findViewById(R.id.container_call_videos);
        mVideoIn = (VideoInFrame) findViewById(R.id.call_video_in);
        mVideoOut = (VideoOutPreviewFrame) findViewById(R.id.call_video_out);
        findViewById(R.id.btn_call_hangup).setOnClickListener(this);
        findViewById(R.id.btn_call_hangup_active).setOnClickListener(this);
        findViewById(R.id.btn_call_create).setOnClickListener(this);
        findViewById(R.id.btn_call_accept).setOnClickListener(this);
        findViewById(R.id.call_deny_btn).setOnClickListener(this);
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
        mCallContainer.setVisibility(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED ? View.VISIBLE : View.GONE);
        boolean vertical = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        mCallVideoContainer.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams videoInParams = (LinearLayout.LayoutParams) mVideoIn.getLayoutParams();
        LinearLayout.LayoutParams videoOutParams = (LinearLayout.LayoutParams) mVideoOut.getLayoutParams();
        videoOutParams.width = videoInParams.width = vertical ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        videoOutParams.height = videoInParams.height = vertical ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;

        mCallCreateContainer
                .setVisibility(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED && !isInCall() && !isCallRinging() && !isCallProceeding() && !isCallPending() ? View.VISIBLE : View.GONE);
        mCallInboundContainer.setVisibility(isCallRinging() ? View.VISIBLE : View.GONE);
        mCallOutboundContainer.setVisibility(isCallProceeding() || isCallPending() && !isCallRinging() ? View.VISIBLE : View.GONE);
        mCallActiveContainer.setVisibility(isInCall() ? View.VISIBLE : View.GONE);

        if (isCallRinging()) {
            Contact contact = sPendingCall.getContact(Contact.DEFAULT_CONTACT_ID);
            ((TextView) findViewById(R.id.label_call_calling)).setText(Html.fromHtml(getString(R.string.label_call_calling, contact == null ? null : contact.getDisplayName())));
        }

        if (isCallActive()) {
            Call call = Rtcc.instance().getCurrentCall();
            if (call.getVideoIn(Contact.DEFAULT_CONTACT_ID) != mVideoIn) {
                call.setVideoIn(mVideoIn, Contact.DEFAULT_CONTACT_ID);
            }
            if (call.getVideoOut() != mVideoOut) {
                call.setVideoOut(mVideoOut);
            }
        }
    }

    private static boolean isLoading() {
        RtccEngine.Status status = Rtcc.getEngineStatus();
        return status == RtccEngine.Status.INITIALIZING || status == RtccEngine.Status.AUTHENTICATING || status == RtccEngine.Status.NETWORK_LOST || status == RtccEngine.Status.DISCONNECTING;
    }

    private static boolean isCallPending() {
        return sPendingCall != null;
    }

    private static boolean isCallProceeding() {
        return sPendingCall != null && sPendingCall.getStatus() == Call.CallStatus.PROCEEDING;
    }

    private static boolean isCallRinging() {
        return sPendingCall != null && sPendingCall.getStatus() == Call.CallStatus.RINGING;
    }

    private static boolean isInCall() {
        return Rtcc.instance() != null && Rtcc.instance().getCurrentCall() != null && Rtcc.instance().getCurrentCall().getStatus() != Call.CallStatus.ENDED;
    }

    private static boolean isCallActive() {
        return isInCall() && Rtcc.instance().getCurrentCall().getStatus() == Call.CallStatus.ACTIVE;
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
            case R.id.btn_call_create:
                call();
                break;
            case R.id.btn_call_hangup:
            case R.id.btn_call_hangup_active:
                hangup();
                break;
            case R.id.btn_call_accept:
                accept();
                break;
            case R.id.call_deny_btn:
                deny();
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

    private void call() {
        String uid = ((EditText) findViewById(R.id.txt_call_uid)).getText().toString();
        Rtcc.instance().createCall(uid);
        Util.hideSoftKeyboard(mCallContainer);
    }

    private void hangup() {
        mCallOutboundContainer.setVisibility(View.GONE);
        mCallActiveContainer.setVisibility(View.GONE);
        if (sPendingCall != null && sPendingCall.getStatus() != Call.CallStatus.ENDED) {
            sPendingCall.hangup();
        } else if (Rtcc.instance().getCurrentCall() != null && Rtcc.instance().getCurrentCall().getStatus() == Call.CallStatus.ACTIVE) {
            Rtcc.instance().getCurrentCall().hangup();
        }
    }

    private void deny() {
        if (sPendingCall != null && sPendingCall.getStatus() == Call.CallStatus.RINGING) {
            sPendingCall.hangup();
            mCallInboundContainer.setVisibility(View.GONE);
        }
    }

    private void accept() {
        if (sPendingCall != null && sPendingCall.getStatus() == Call.CallStatus.RINGING) {
            sPendingCall.resume();
            mCallInboundContainer.setVisibility(View.GONE);
        }
    }

    @RtccEventListener
    public void onCallStatusEvent(StatusEvent event) {
        Util.hideSoftKeyboard(findViewById(R.id.txt_call_uid));
        switch (event.getStatus()) {
            case CREATED:
                sPendingCall = event.getCall();
                break;
            case RINGING:
                break;
            case ACTIVE:
                sPendingCall = null;
                break;
            case ENDED:
                sPendingCall = null;
                Toast.makeText(this, "Call ended ~ " + event.getCall().getTerminationCause() + " (" + (event.getCall().getCallDuration() / 1000) + "s)", Toast.LENGTH_LONG).show();
                break;
            case PROCEEDING:
                break;
            case PAUSED:
                break;
        }
        invalidate();
    }

}
