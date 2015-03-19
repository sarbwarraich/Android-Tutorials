package net.rtccloud.tutorial.adapter;

import net.rtccloud.tutorial.model.Roster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PresenceListAdapter extends BaseAdapter {

    private final Context mContext;

    private final LayoutInflater mInflater;

    public PresenceListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public int getCount() {
        return Roster.size();
    }

    @Override
    public Roster.RosterEntry getItem(int position) {
        return Roster.get().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
            view = (TextView) mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        } else {
            view = (TextView) convertView;
        }
        Roster.RosterEntry entry = getItem(position);
        view.setText(Roster.getPresenceSpan(mContext, entry.presence, entry.uid));
        return view;
    }
}
