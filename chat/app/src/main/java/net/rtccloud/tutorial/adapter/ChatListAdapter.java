package net.rtccloud.tutorial.adapter;

import net.rtccloud.tutorial.R;
import net.rtccloud.tutorial.model.Chat;

import android.content.Context;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatListAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private final Date mDate = new Date();

    public ChatListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return Chat.size();
    }

    @Override
    public Chat.ChatEntry getItem(int position) {
        return Chat.get().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.item_chat, parent, false);
            holder.user = (TextView) view.findViewById(R.id.item_chat_user);
            holder.message = (TextView) view.findViewById(R.id.item_chat_message);
            holder.timestamp = (TextView) view.findViewById(R.id.item_chat_timestamp);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            view = convertView;
        }

        Chat.ChatEntry entry = getItem(position);
        mDate.setTime(entry.timestamp);
        holder.user.setText(Html.fromHtml("<b>" + (entry.byMe ? "To" : "From") + ":</b> " + entry.uid));
        holder.message.setText(entry.message);
        holder.timestamp.setText(mDateFormat.format(mDate));
        holder.timestamp.setCompoundDrawablesWithIntrinsicBounds(0, 0, entry.byMe ? (entry.acknowledged ? R.drawable.ic_sms_mms_delivered : R.drawable.ic_sms_mms_pending) : 0, 0);
        holder.user.setGravity(entry.byMe ? Gravity.RIGHT : Gravity.LEFT);
        holder.message.setGravity(entry.byMe ? Gravity.RIGHT : Gravity.LEFT);
        holder.timestamp.setGravity(entry.byMe ? Gravity.RIGHT : Gravity.LEFT);
        return view;
    }

    public static class ViewHolder {

        TextView user;

        TextView message;

        TextView timestamp;
    }
}
