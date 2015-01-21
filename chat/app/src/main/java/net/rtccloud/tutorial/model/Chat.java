package net.rtccloud.tutorial.model;


import android.util.SparseArray;

import net.rtccloud.sdk.event.datachannel.DataChannelAckEvent;
import net.rtccloud.sdk.event.datachannel.DataChannelEvent;
import net.rtccloud.sdk.event.datachannel.DataChannelOutOfBandEvent;

import java.util.ArrayList;
import java.util.List;

public class Chat {

    private static SparseArray<ChatEntry> sMap = new SparseArray<>();
    private static List<ChatEntry> sList = new ArrayList<>();

    public static List<ChatEntry> get() {
        return sList;
    }

    public static void add(int id, String uid, String message) {
        ChatEntry entry = new ChatEntry(id, uid, message, true);
        sMap.put(id, entry);
        sList.add(entry);
    }

    public static void add(DataChannelEvent event) {
        if (event instanceof DataChannelOutOfBandEvent) {
            ChatEntry entry = new ChatEntry(((DataChannelOutOfBandEvent) event).getId(), ((DataChannelOutOfBandEvent) event).getUid(), new String(((DataChannelOutOfBandEvent) event).getPayload()), false);
            sMap.put(entry.id, entry);
            sList.add(entry);
        } else if (event instanceof DataChannelAckEvent) {
            int id = ((DataChannelAckEvent) event).getId();
            ChatEntry entry = sMap.get(id);
            if (entry != null) {
                entry.acknowledged = true;
            }
        }
    }

    public static void clear() {
        sMap.clear();
        sList.clear();
    }

    public static class ChatEntry {

        private static final String PENDING = "☐ ";
        private static final String ACKNOWLEDGED = "☑ ";

        public final int id;
        public final String uid;
        public final String message;
        public boolean acknowledged;
        public final boolean byMe;

        public ChatEntry(int id, String uid, String message, boolean byMe) {
            this.id = id;
            this.uid = uid;
            this.message = message;
            this.byMe = byMe;
        }

        @Override
        public String toString() {
            return (byMe
                    ? ((acknowledged ? ACKNOWLEDGED : PENDING) + "To ")
                    : "From ")
                    + uid + ": " + message;
        }
    }

}
