package net.rtccloud.tutorial.adapter;

import net.rtccloud.tutorial.R;
import net.rtccloud.tutorial.model.Roster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PresenceSpinnerAdapter extends BaseAdapter {

    private final static int MAX_PRESENCE = 256;

    private final Context mContext;

    private final LayoutInflater mInflater;

    public PresenceSpinnerAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public int getCount() {
        return MAX_PRESENCE;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return buildView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return buildView(position, convertView, parent);
    }

    private View buildView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
            view = (TextView) mInflater.inflate(R.layout.item_presence, parent, false);
        } else {
            view = (TextView) convertView;
        }
        view.setText(Roster.getPresenceSpan(mContext, position));
        return view;
    }
}
