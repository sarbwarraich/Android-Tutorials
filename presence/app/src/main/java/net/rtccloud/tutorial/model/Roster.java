package net.rtccloud.tutorial.model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.rtccloud.sdk.Rtcc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Roster {

    private static Map<String, RosterEntry> sMap = new HashMap<>();
    private static List<RosterEntry> sList = new ArrayList<>();

    public static List<RosterEntry> get() {
        return sList;
    }

    public static void add(String uid) {
        if (TextUtils.isEmpty(uid) || sMap.containsKey(uid)) {
            return;
        }
        RosterEntry entry = new RosterEntry(uid);
        sMap.put(uid, entry);
        sList.add(entry);
        Collections.sort(sList);
    }

    public static void updateRoster(Map<String, Integer> map) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String uid = entry.getKey();
            Presence presence = Presence.fromOrdinal(entry.getValue());
            if (sMap.containsKey(uid)) {
                sMap.get(uid).presence = presence;
            } else {
                RosterEntry rosterEntry = new RosterEntry(uid);
                rosterEntry.presence = presence;
                sMap.put(uid, rosterEntry);
                sList.add(rosterEntry);
            }
        }
        Collections.sort(sList);
    }

    public static void update(String uid, int presence) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        if (sMap.containsKey(uid)) {
            sMap.get(uid).presence = Presence.fromOrdinal(presence);
        } else {
            RosterEntry rosterEntry = new RosterEntry(uid);
            rosterEntry.presence = Presence.fromOrdinal(presence);
            sMap.put(uid, rosterEntry);
            sList.add(rosterEntry);
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
        Rtcc.instance().roster().clear();
    }

    public static void remove(RosterEntry entry) {
        sMap.remove(entry.uid);
        sList.remove(entry);
        Rtcc.instance().roster().remove(entry.uid);
    }


    public static class RosterEntry implements Comparable<RosterEntry> {

        public final String uid;
        public Presence presence;

        public RosterEntry(String uid) {
            this.uid = uid;
        }

        @Override
        public String toString() {
            return uid + " ~ " + presence;
        }

        @Override
        public int compareTo(@NonNull RosterEntry another) {
            return uid.compareTo(another.uid);
        }
    }
}
