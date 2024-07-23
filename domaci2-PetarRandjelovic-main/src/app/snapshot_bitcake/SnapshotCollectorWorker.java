package app.snapshot_bitcake;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.MeetingMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 *
 * @author bmilojkovic
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

    private volatile boolean working = true;

    private AtomicBoolean collecting = new AtomicBoolean(false);

    private AtomicInteger parsed = new AtomicInteger(0);

    private Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();


    private BitcakeManager bitcakeManager;

    public SnapshotCollectorWorker() {
        bitcakeManager = new LaiYangBitcakeManager();
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return bitcakeManager;
    }

    @Override
    public void run() {
        while (working) {
            AppConfig.timestampedErrorPrint("?");
            /*
             * Not collecting yet - just sleep until we start actual work, or finish
             */
            while (collecting.get() == false) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (working == false) {
                    return;
                }
            }

            ((LaiYangBitcakeManager) bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this, AppConfig.parentId.get(), AppConfig.supervisordId.get(), AppConfig.snapshotVersion.get());
            boolean waiting = true;


            while (waiting) {
                if (AppConfig.neighboursAnswered.get() >= AppConfig.myServentInfo.getNeighbors().size() - 1) {
                    AppConfig.timestampedStandardPrint(AppConfig.neighboursAnswered.get() + " UDJEM");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    waiting = false;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (working == false) {
                    return;
                }
            }
            int sum;
            sum = 0;
            boolean anotherRound = true;
            while (anotherRound) {
                    for (Integer i : AppConfig.parentMap.keySet()) {
                    if (i == AppConfig.myServentInfo.getId()) {
                        //
                        continue;
                    }

                    Message meetingMessage = new MeetingMessage(AppConfig.getInfoById(AppConfig.myServentInfo.getId()), AppConfig.getInfoById(i),
                            AppConfig.snapshotVersion.get(), AppConfig.parentId.get(), collectedLYValues, AppConfig.parentMap, false);
                    AppConfig.timestampedErrorPrint("SALJEM PORUKU u snapshotu " + AppConfig.getInfoById(i).getId() + " " + meetingMessage.getParentMap() + " serventu " + i);
                    AppConfig.timestampedErrorPrint("SALJEM PORUKU u snapshotu za valuess " + AppConfig.getInfoById(i).getId() + " " + collectedLYValues + " serventu " + i);
                    MessageUtil.sendMessage(meetingMessage);
                }
                if (collectedLYValues.size() == AppConfig.getServentCount()) {
                    for (Integer i : AppConfig.parentMap.keySet()) {
                        if (i == AppConfig.myServentInfo.getId()) {
                            continue;
                        }
                        Message meetingMessage = new MeetingMessage(AppConfig.getInfoById(AppConfig.myServentInfo.getId()), AppConfig.getInfoById(i),
                                AppConfig.snapshotVersion.get(), AppConfig.parentId.get(), collectedLYValues, AppConfig.parentMap, true);
                        AppConfig.timestampedErrorPrint("SALJEM POSLEDNJU PORUKU u snapshotu " + AppConfig.getInfoById(i).getId() + " " + meetingMessage.getParentMap() + " serventu " + i);
                        AppConfig.timestampedErrorPrint("SALJEM POSLEDNJU PORUKU u snapshotu za valuess " + AppConfig.getInfoById(i).getId() + " " + collectedLYValues + " serventu " + i);

                        MessageUtil.sendMessage(meetingMessage);
                    }
                    anotherRound = false;
                } else {
                    AppConfig.timestampedStandardPrint("JOS JEDNA RUNDA " + collectedLYValues.size());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AppConfig.collectedLYValues = collectedLYValues;
            }
            AppConfig.timestampedStandardPrint("IZASAO IZ WHILE U SNAPSHOTU COLLECTIONU  " + collectedLYValues);

            boolean waiting2 = true;
            while (waiting2) {
                   if (AppConfig.startCountingNowOvo.get()) {
                    AppConfig.timestampedStandardPrint(AppConfig.neighboursAnswered.get() + " UDJEM");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    waiting2 = false;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (working == false) {
                    return;
                }
            }

            AppConfig.timestampedStandardPrint("Collecting results from " + collectedLYValues);
            for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
                sum += nodeResult.getValue().getRecordedAmount();
                AppConfig.timestampedStandardPrint(

                        "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount() + " versionid " + nodeResult.getValue().getSnapshotVersion());

            }
                AppConfig.parentMap.get(AppConfig.myServentInfo.getId());
            for (int c = AppConfig.snapshotVersion.get() - 1; c < AppConfig.snapshotVersion.get(); c++) {
                for (int i = 0; i < AppConfig.getServentCount(); i++) {
                    for (int j = 0; j < AppConfig.getServentCount(); j++) {
                        if (i != j) {
                            if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
                                    AppConfig.getInfoById(j).getNeighbors().contains(i)) {
                                ConcurrentHashMap<Integer, Integer> ijAmount = collectedLYValues.get(i).getGiveHistory().get(c);

                                if (ijAmount == null) {
                                    ijAmount = new ConcurrentHashMap<>();
                                    collectedLYValues.get(i).getGiveHistory().put(AppConfig.snapshotVersion.get() - 1, ijAmount);
                                }
                                ConcurrentHashMap<Integer, Integer> jiAmount = collectedLYValues.get(j).getGetHistory().get(c);
                                if (jiAmount == null) {
                                    jiAmount = new ConcurrentHashMap<>();
                                    collectedLYValues.get(j).getGetHistory().put(AppConfig.snapshotVersion.get() - 1, jiAmount);
                                }
                                if (ijAmount.get(j) == null) {
                                    ijAmount.put(j, 0);
                                }
                                if (jiAmount.get(i) == null) {
                                    jiAmount.put(i, 0);
                                }
                                int aAmount = ijAmount.get(j);
                                int bAmount = jiAmount.get(i);

                                AppConfig.timestampedStandardPrint("aAmount: " + aAmount + " bAmount: " + bAmount + " za verziju " + AppConfig.snapshotVersion);

                                if (aAmount != bAmount) {
                                    String outputString = String.format(
                                            "Unreceived bitcake amount: %d from servent %d to servent %d a from snapshot version %d",
                                            aAmount - bAmount, i, j, c);
                                    AppConfig.timestampedStandardPrint(outputString);
                                    sum += aAmount - bAmount;
                                }
                            }
                        }

                    }
                }

            }
            AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
            AppConfig.timestampedStandardPrint("Pre clearovanja: " + collectedLYValues);
            collectedLYValues.clear();
            AppConfig.timestampedStandardPrint("Posle clearovanja: " + collectedLYValues);
            AppConfig.startCountingNowOvo.set(false);
            AppConfig.parentMap.clear();
            AppConfig.parentId.set(-1);
            AppConfig.supervisordId.set(-1);
            AppConfig.collectedLYValues = new ConcurrentHashMap<>();
            AppConfig.rejectedList.clear();
            AppConfig.neighboursAnswered.set(0);
            AppConfig.incrementForMeeting.set(0);
            AppConfig.rejectedList.add(-2);

            collecting.set(false);
            parsed.getAndIncrement();
        }

    }

    @Override
    public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
        if (lySnapshotResult.getSnapshotVersion() != AppConfig.snapshotVersion.get()) {
            AppConfig.timestampedStandardPrint("Snapshot version is not the same as mine");
        } else {
            collectedLYValues.put(id, lySnapshotResult);
        }

    }

    @Override
    public void startCollecting() {
        boolean oldValue = this.collecting.getAndSet(true);

        if (oldValue == true) {
            AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
        }
    }

    @Override
    public void stop() {
        working = false;
    }

    public Map<Integer, LYSnapshotResult> getCollectedLYValues() {
        return collectedLYValues;
    }
}
