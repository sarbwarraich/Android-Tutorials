package net.rtccloud.tutorial;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.rtccloud.sdk.Call;
import net.rtccloud.sdk.Contact;
import net.rtccloud.sdk.Logger;
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

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private RequestQueue mRequestQueue;

    private View mConnectionContainer;
    private View mCallContainer;
    private View mCallOutboundContainer;
    private View mCallInboundContainer;

    private EditText mContactUid;
    private TextView mCalleeCalling;
    private Button mCallHangup;
    private ProgressBar mCallProgress;
    private LinearLayout mVideoContainer;
    private VideoInFrame mVideoIn;
    private VideoOutPreviewFrame mVideoOut;

    private static Call sPendingCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.setGlobalLevel(Logger.LoggerLevel.VERBOSE);
        setContentView(R.layout.activity_main);
        buildActionBar();

        mRequestQueue = Volley.newRequestQueue(this);
        mConnectionContainer = findViewById(R.id.connection_container);
        mCallContainer = findViewById(R.id.call_container);
        mCallOutboundContainer = findViewById(R.id.call_outbound_container);
        mCallInboundContainer = findViewById(R.id.call_inbound_container);
        mContactUid = (EditText) findViewById(R.id.call_uid);
        mCalleeCalling = (TextView) findViewById(R.id.callee_calling);
        mCallHangup = (Button) findViewById(R.id.call_hangup_btn);
        mCallProgress = (ProgressBar) findViewById(R.id.call_in_progress);
        mVideoContainer = (LinearLayout) findViewById(R.id.call_video_container);
        mVideoIn = (VideoInFrame) findViewById(R.id.call_video_in);
        mVideoOut = (VideoOutPreviewFrame) findViewById(R.id.call_video_out);

        mCallHangup.setOnClickListener(this);
        findViewById(R.id.call_create_btn).setOnClickListener(this);
        findViewById(R.id.connection_btn).setOnClickListener(this);
        findViewById(R.id.call_accept_btn).setOnClickListener(this);
        findViewById(R.id.call_deny_btn).setOnClickListener(this);

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
        mCallContainer.setVisibility(Rtcc.getEngineStatus() == RtccEngine.Status.AUTHENTICATED ? View.VISIBLE : View.GONE);
        boolean vertical = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        mVideoContainer.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams videoInParams = (LinearLayout.LayoutParams) mVideoIn.getLayoutParams();
        LinearLayout.LayoutParams videoOutParams = (LinearLayout.LayoutParams) mVideoOut.getLayoutParams();
        videoOutParams.width = videoInParams.width = vertical ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        videoOutParams.height = videoInParams.height = vertical ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;

        if (isCallRinging()) {
            Contact contact = sPendingCall.getContact(Contact.DEFAULT_CONTACT_ID);
            mCalleeCalling.setText(Html.fromHtml(getString(R.string.callee_calling, contact == null ? null : contact.getDisplayName())));
            mVideoContainer.setVisibility(View.GONE);
            mCallOutboundContainer.setVisibility(View.GONE);
            mCallHangup.setVisibility(View.GONE);
            mCallInboundContainer.setVisibility(View.VISIBLE);
            mCallProgress.setVisibility(View.VISIBLE);
        } else if (isCallPending() || isInCall()) {
            mVideoContainer.setVisibility(isInCall() ? View.VISIBLE : View.GONE);
            mCallOutboundContainer.setVisibility(View.GONE);
            mCallInboundContainer.setVisibility(View.GONE);
            mCallProgress.setVisibility(isInCall() ? View.GONE : View.VISIBLE);
            mCallHangup.setVisibility(isInCall() || isCallProceeding() || isCallRinging() ? View.VISIBLE : View.GONE);
        } else {
            mVideoContainer.setVisibility(View.GONE);
            mCallInboundContainer.setVisibility(View.GONE);
            mCallHangup.setVisibility(View.GONE);
            mCallProgress.setVisibility(View.GONE);
            mCallOutboundContainer.setVisibility(View.VISIBLE);
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
        menu.findItem(R.id.action_disconnect).setVisible((status == RtccEngine.Status.AUTHENTICATED && !isCallActive()) || status == RtccEngine.Status.CONNECTED);
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
            case R.id.call_create_btn:
                call();
                break;
            case R.id.call_hangup_btn:
                hangup();
                break;
            case R.id.call_accept_btn:
                accept();
                break;
            case R.id.call_deny_btn:
                deny();
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

    private void call() {
        String uid = mContactUid.getText().toString();
        Rtcc.instance().createCall(uid);
        Util.hideSoftKeyboard(mCallContainer);
        mCallOutboundContainer.setVisibility(View.GONE);
    }

    private void hangup() {
        if (sPendingCall != null && sPendingCall.getStatus() != Call.CallStatus.ENDED) {
            sPendingCall.hangup();
        } else if (Rtcc.instance().getCurrentCall() != null && Rtcc.instance().getCurrentCall().getStatus() != Call.CallStatus.ENDED) {
            Rtcc.instance().getCurrentCall().hangup();
        }
        mCallHangup.setVisibility(View.GONE);
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
