package net.rtccloud.tutorial.model;

import net.rtccloud.sdk.Rtcc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Roster {

    private final static Map<String, RosterEntry> sMap = new HashMap<>();

    private final static List<RosterEntry> sList = new ArrayList<>();

    public static List<RosterEntry> get() {
        return sList;
    }

    public static int size() {
        return sList.size();
    }

    public static void add(String uid) {
        if (sMap.containsKey(uid)) {
            return;
        }
        add(new RosterEntry(uid));
    }

    private static void add(RosterEntry entry) {
        sMap.put(entry.uid, entry);
        sList.add(entry);
        Collections.sort(sList);
    }

    public static void updateRoster(Map<String, Integer> map) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String uid = entry.getKey();
            if (sMap.containsKey(uid)) {
                sMap.get(uid).presence = entry.getValue();
            } else {
                RosterEntry rosterEntry = new RosterEntry(uid, entry.getValue());
                add(rosterEntry);
            }
        }
    }

    public static void update(String uid, int presence) {
        if (sMap.containsKey(uid)) {
            sMap.get(uid).presence = presence;
        } else {
            add(new RosterEntry(uid, presence));
        }
    }

    public static void update(Map<String, Integer> presences) {
        Set<Map.Entry<String, Integer>> entries = presences.entrySet();
        for (Map.Entry<String, Integer> entry : entries) {
            update(entry.getKey(), entry.getValue());
        }
    }

    public static void clear() {
        sMap.clear();
        sList.clear();
        if (Rtcc.instance() != null) {
            Rtcc.instance().roster().clear();
        }
    }

    public static void remove(String uid) {
        RosterEntry entry = sMap.remove(uid);
        sList.remove(entry);
        Rtcc.instance().roster().remove(uid);
    }

    public static class RosterEntry implements Comparable<RosterEntry> {

        public final String uid;

        public int presence;

        public RosterEntry(String uid) {
            this.uid = uid;
        }

        public RosterEntry(String uid, int presence) {
            this.uid = uid;
            this.presence = presence;
        }

        @Override
        public int compareTo(@NonNull RosterEntry another) {
            return uid.compareTo(another.uid);
        }
    }

    public static SpannableString getPresenceSpan(Context ctx, int value, String uid) {
        int icon;
        switch (value) {
            case 0:
                icon = android.R.drawable.presence_offline;
                break;
            case 1:
                icon = android.R.drawable.presence_online;
                break;
            case 2:
                icon = android.R.drawable.presence_busy;
                break;
            case 3:
                icon = android.R.drawable.presence_away;
                break;
            case 4:
                icon = android.R.drawable.presence_video_online;
                break;
            case 5:
                icon = android.R.drawable.presence_video_busy;
                break;
            case 6:
                icon = android.R.drawable.presence_video_away;
                break;
            case 7:
                icon = android.R.drawable.presence_audio_online;
                break;
            case 8:
                icon = android.R.drawable.presence_audio_busy;
                break;
            case 9:
                icon = android.R.drawable.presence_audio_away;
                break;
            default:
                icon = android.R.drawable.presence_invisible;
                break;
        }

        String str = String.format("  %03d", value);
        if (uid != null) {
            str += " ~ " + uid;
        }
        SpannableString builder = new SpannableString(str);
        builder.setSpan(new ImageSpan(ctx, icon), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    public static SpannableString getPresenceSpan(Context ctx, int value) {
        return getPresenceSpan(ctx, value, null);
    }

}
