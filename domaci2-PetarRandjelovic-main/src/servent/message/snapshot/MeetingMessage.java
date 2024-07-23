package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MeetingMessage extends BasicMessage {

   // private LYSnapshotResult lySnapshotResult;
    private Map<Integer,LYSnapshotResult> lySnapshotResultMap;
    public MeetingMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                          int snapshotVersion, int initiatorId, Map<Integer,
            LYSnapshotResult> lySnapshotResultMap, ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap,boolean sendback) {
        super(MessageType.MEETING, originalSenderInfo, receiverInfo, snapshotVersion, initiatorId,lySnapshotResultMap,parentMap,sendback);


        this.lySnapshotResultMap=lySnapshotResultMap;
    }

    @Override
    public Map<Integer, LYSnapshotResult> getLySnapshotResultMap() {
        return lySnapshotResultMap;
    }

    @Override
    public String toString() {
        return "LYTellMessage{" +
                "lySnapshotResultMap=" + lySnapshotResultMap +
                '}' +super.toString();
    }
}
