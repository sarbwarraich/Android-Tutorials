package net.rtccloud.tutorial;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Util {

    public static void hideSoftKeyboard(final View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
