package net.rtccloud.tutorial;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Util {

    public static void detectConfigError(final Activity activity) {
        if (TextUtils.isEmpty(Config.APP_ID) || TextUtils.isEmpty(Config.AUTH_URL)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.error_title)
                    .setMessage(R.string.error_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    public static void hideSoftKeyboard(final View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
