package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MeetingHandler implements MessageHandler {


    private Message clientMessage;
    private Map<Integer, LYSnapshotResult> collectedLYValues;
    private SnapshotCollector snapshotCollector;


    private Map<Integer, Boolean> isteVrednosti = new ConcurrentHashMap<>();

    public MeetingHandler(Message clientMessage, Map<Integer, LYSnapshotResult> collectedLYValues, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.collectedLYValues = collectedLYValues;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        AppConfig.timestampedErrorPrint("MOJA MAPA JE " + AppConfig.parentMap);
        if (clientMessage.getMessageType() == MessageType.MEETING) {
            AppConfig.timestampedErrorPrint("MeetingHandler poslato od " + clientMessage.getInitiatorId() +
                    " klijent mapa " + clientMessage.getParentMap() + " APP MAPA JE" + AppConfig.parentMap);

            for (ConcurrentHashMap.Entry<Integer, CopyOnWriteArrayList<Integer>> entry : clientMessage.getParentMap().entrySet()) {
                AppConfig.parentMap.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArrayList<>()).add(entry.getKey());
                for (Integer key : entry.getValue()) {
                    AppConfig.parentMap.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArrayList<>()).add(key);

                }
                CopyOnWriteArrayList<Integer> list = null;
                for (Integer key : AppConfig.parentMap.keySet()) {
                    list = AppConfig.parentMap.get(key);
                    for (int i = 0; i < list.size(); i++) {
                        for (int j = i + 1; j < list.size(); j++) {
                            if (list.get(i).equals(list.get(j))) {
                                list.remove(j);
                                j--; // Adjust index after removal
                            }
                        }
                    }
                }

            }
            AppConfig.timestampedStandardPrint(clientMessage.getLySnapshotResultMap().toString());
            for (Map.Entry<Integer, LYSnapshotResult> entry : clientMessage.getLySnapshotResultMap().entrySet()) {
                snapshotCollector.addLYSnapshotInfo(
                        entry.getKey(),
                        entry.getValue());
            }

            AppConfig.timestampedErrorPrint("MAPA U MEETING JE " + AppConfig.parentMap);
            AppConfig.timestampedErrorPrint("SNAPSHOOT U MEETING JE " + ((SnapshotCollectorWorker) snapshotCollector).getCollectedLYValues());

            if (clientMessage.isSendBack()) {
                AppConfig.incrementForMeeting.incrementAndGet();
                AppConfig.timestampedErrorPrint("DOBAR SAM " + clientMessage.getOriginalSenderInfo().getId() + " vel " + AppConfig.parentMap.size() + " counter " + AppConfig.incrementForMeeting.get());


            }

            if (AppConfig.incrementForMeeting.get() == AppConfig.parentMap.size() - 1) {
                AppConfig.timestampedErrorPrint("KRENI DA RACUNAS");
                AppConfig.startCountingNowOvo.set(true);
            }
        }


    }
}


