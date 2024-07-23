package servent;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.LYMarkerHandler;
import servent.handler.snapshot.LYTellHandler;
import servent.handler.snapshot.MeetingHandler;
import servent.handler.snapshot.RejectHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SimpleServentListener implements Runnable, Cancellable {

    private volatile boolean working = true;

    private SnapshotCollector snapshotCollector;

    public SimpleServentListener(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
    }

    private static ScheduledExecutorService executorService;

    /*
     * Thread pool for executing the handlers. Each client will get it's own handler thread.
     */
    private final ExecutorService threadPool = Executors.newWorkStealingPool();
    private List<Message> redMessages = new ArrayList<>();

    @Override
    public void run() {
        ServerSocket listenerSocket = null;
        try {
            listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
            /*
             * If there is no connection after 1s, wake up and see if we should terminate.
             */
            listenerSocket.setSoTimeout(1000);
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
            System.exit(0);
        }


        while (working) {
            try {
                Message clientMessage;
                /*
                 * Lai-Yang stuff. Process any red messages we got before we got the marker.
                 * The marker contains the collector id, so we need to process that as our first
                 * red message.
                 */
                Socket clientSocket = listenerSocket.accept();
                clientMessage = MessageUtil.readMessage(clientSocket);

                synchronized (AppConfig.colorLock) {


                    if (AppConfig.snapshotVersion.get() != clientMessage.getSnapshotVersion()) {

                        if (clientMessage.getMessageType() == MessageType.LY_MARKER && AppConfig.snapshotVersion.get() != clientMessage.getSnapshotVersion()) {

                            LaiYangBitcakeManager lyFinancialManager =
                                    (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
                            lyFinancialManager.markerEvent(
                                    Integer.parseInt(clientMessage.getMessageText()), snapshotCollector, clientMessage.getInitiatorId(), clientMessage.getOriginalSenderInfo().getId(), AppConfig.snapshotVersion.get());
                        }
                    }


                }

                MessageHandler messageHandler = new NullHandler(clientMessage);
                switch (clientMessage.getMessageType()) {
                    case TRANSACTION:
                        messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
                        break;
                    case LY_MARKER:

                        messageHandler = new LYMarkerHandler();

                        if (AppConfig.parentId.get() == AppConfig.myServentInfo.getId()) {

                            AppConfig.parentMap.computeIfAbsent(clientMessage.getInitiatorId(), k -> new CopyOnWriteArrayList<>()).add(clientMessage.getOriginalSenderInfo().getId());
                            LaiYangBitcakeManager lyFinancialManager =
                                    (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
                            lyFinancialManager.rejectEvent(clientMessage.getInitiatorId(), clientMessage.getSnapshotVersion(), snapshotCollector, clientMessage.getOriginalSenderInfo().getId());
                            AppConfig.timestampedStandardPrint("Odbijam automatski marker od " + clientMessage.getInitiatorId() + " onaj koj salje " + clientMessage.getOriginalSenderInfo().getId() + " supervizor je " + AppConfig.supervisordId.get());
                        } else if (AppConfig.supervisordId.get() != clientMessage.getOriginalSenderInfo().getId()) {
                            LaiYangBitcakeManager lyFinancialManager =
                                    (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
                            lyFinancialManager.rejectEvent(clientMessage.getInitiatorId(), clientMessage.getSnapshotVersion(), snapshotCollector, clientMessage.getOriginalSenderInfo().getId());
                        }
                        if ((AppConfig.neighboursAnswered.get() == AppConfig.myServentInfo.getNeighbors().size() - 1)) {
                            giveTellAnswer(clientMessage);

                        }
                        break;
                    case LY_TELL:


                        AppConfig.timestampedErrorPrint("Dobio tell KOMSIJA " + AppConfig.neighboursAnswered + " od " + clientMessage.getOriginalSenderInfo().getId());
                        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap = AppConfig.parentMap;

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
                        AppConfig.timestampedErrorPrint("STA JE U OVOM TELU " + clientMessage.getLySnapshotResultMap());
                        messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
                        break;
                    case REJECT:
                        AppConfig.neighboursAnswered.incrementAndGet();

                        AppConfig.rejectedList.add(clientMessage.getOriginalSenderInfo().getId());
                        AppConfig.parentMap.computeIfAbsent(clientMessage.getInitiatorId(), k -> new CopyOnWriteArrayList<>()).add(clientMessage.getOriginalSenderInfo().getId());
                        for (Integer key : AppConfig.parentMap.keySet()) {
                            CopyOnWriteArrayList<Integer> list = AppConfig.parentMap.get(key);
                            for (int i = 0; i < list.size(); i++) {
                                for (int j = i + 1; j < list.size(); j++) {
                                    if (list.get(i).equals(list.get(j))) {
                                        list.remove(j);
                                        j--; // Adjust index after removal
                                    }
                                }
                            }
                        }
                        messageHandler = new RejectHandler();

                        AppConfig.timestampedStandardPrint("KOMSIJA REJECT " + AppConfig.neighboursAnswered.get() + " TREBA " + AppConfig.myServentInfo.getNeighbors().size());
                        if ((AppConfig.neighboursAnswered.get() == AppConfig.myServentInfo.getNeighbors().size() - 1)) {
                            giveTellAnswer(clientMessage);

                        }

                        break;
                    case MEETING:

                        messageHandler = new MeetingHandler(clientMessage, ((SnapshotCollectorWorker) snapshotCollector).getCollectedLYValues(), snapshotCollector);
                        if (executorService != null && executorService.isShutdown()) {
                            executorService.shutdownNow();
                        }
                        executorService = Executors.newSingleThreadScheduledExecutor();
                        int timeout = 2;
                        // Schedule a task to be executed if no case is entered within TIMEOUT_DURATION seconds
                        executorService.schedule(() -> {
                        }, timeout, TimeUnit.SECONDS);

                        AppConfig.canStartCounting.set(false);
                        executorService.shutdown();
                        break;
                }

                threadPool.submit(messageHandler);

            } catch (SocketTimeoutException timeoutEx) {
                //Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void giveTellAnswer(Message clientMessage) {
        AppConfig.timestampedErrorPrint("Poslacu nazad parent id " + " " + AppConfig.parentId.get() + " serveru " + AppConfig.supervisordId.get() + " sa znanjem" +
                AppConfig.parentMap + " " + clientMessage.getMessageText());
        LaiYangBitcakeManager lyFinancialManager =
                (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
        lyFinancialManager.tellEvent(
                snapshotCollector, clientMessage.getInitiatorId(), AppConfig.supervisordId.get(), AppConfig.parentMap, clientMessage.getMessageText());

    }

    @Override
    public void stop() {
        this.working = false;
    }

}