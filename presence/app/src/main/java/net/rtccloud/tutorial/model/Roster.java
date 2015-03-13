package net.rtccloud.tutorial.model;

import android.support.annotation.NonNull;

import net.rtccloud.sdk.Rtcc;

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
        Rtcc.instance().roster().clear();
    }

    public static void remove(String uid) {
        RosterEntry entry = sMap.remove(uid);
        sList.remove(entry);
        Rtcc.instance().roster().remove(uid);
    }


    public static class RosterEntry implements Comparable<RosterEntry> {

        public final String uid;
        public int presence = -1;

        public RosterEntry(String uid) {
            this.uid = uid;
        }

        public RosterEntry(String uid, int presence) {
            this.uid = uid;
            this.presence = presence;
        }

        @Override
        public String toString() {
            return uid + " ~ " + (presence == -1 ? "•" : presence);
        }

        @Override
        public int compareTo(@NonNull RosterEntry another) {
            return uid.compareTo(another.uid);
        }
    }
}
