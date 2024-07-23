package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYTellMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LYTellHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.LY_TELL) {
            LYTellMessage lyTellMessage = (LYTellMessage) clientMessage;
            //	AppConfig.timestampedStandardPrint("ULAZII");
            AppConfig.timestampedStandardPrint("Dodajem snapshot unutar tell handlera " + lyTellMessage.getOriginalSenderInfo().getId() + " " + lyTellMessage.getLYSnapshotResult());

            for (Integer key : lyTellMessage.getLySnapshotResultMap().keySet()) {

                snapshotCollector.addLYSnapshotInfo(
                        key,
                        lyTellMessage.getLySnapshotResultMap().get(key)
                );
            }

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


            AppConfig.neighboursAnswered.incrementAndGet();
            if ((AppConfig.neighboursAnswered.get() == AppConfig.myServentInfo.getNeighbors().size() - 1) && AppConfig.parentId.get() != AppConfig.myServentInfo.getId()) {
                AppConfig.timestampedErrorPrint("Poslacu nazad parent id " + " " + AppConfig.parentId.get() + " serveru " + AppConfig.supervisordId.get() + " sa znanjem" +
                        AppConfig.parentMap);
                LaiYangBitcakeManager lyFinancialManager =
                        (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
                lyFinancialManager.tellEvent(
                        snapshotCollector, clientMessage.getInitiatorId(), AppConfig.supervisordId.get(), AppConfig.parentMap, clientMessage.getMessageText());
            }
        } else {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

    }

}
