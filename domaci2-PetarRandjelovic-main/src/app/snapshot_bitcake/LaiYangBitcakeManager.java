package app.snapshot_bitcake;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.snapshot.RejectMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class LaiYangBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);


    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

//	private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
//	private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    //colectorid onaj koji je poslao marker
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> giveHistory = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> getHistory = new ConcurrentHashMap<>();

    public LaiYangBitcakeManager() {
//        for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
//
//            int snapshotVersion = AppConfig.snapshotVersion.get();
//
//// For giveHistory
//            ConcurrentHashMap<Integer, Integer> give = giveHistory.get(snapshotVersion);
//            if (give == null) {
//                give = new ConcurrentHashMap<>();
//                giveHistory.putIfAbsent(snapshotVersion, give);
//            }
//            give.putIfAbsent(neighbor, 0);
//
//// For getHistory
//            ConcurrentHashMap<Integer, Integer> get = getHistory.get(snapshotVersion);
//            if (get == null) {
//                get = new ConcurrentHashMap<>();
//                getHistory.putIfAbsent(snapshotVersion, get);
//            }
//            get.putIfAbsent(neighbor, 0);
//        }

        for (int i = 0; i < AppConfig.getServentCount(); i++) {

            int snapshotVersion = AppConfig.snapshotVersion.get();

// For giveHistory
            ConcurrentHashMap<Integer, Integer> give = giveHistory.get(snapshotVersion);
            if (give == null) {
                give = new ConcurrentHashMap<>();
                giveHistory.putIfAbsent(snapshotVersion, give);
            }
            give.putIfAbsent(i, 0);

// For getHistory
            ConcurrentHashMap<Integer, Integer> get = getHistory.get(snapshotVersion);
            if (get == null) {
                get = new ConcurrentHashMap<>();
                getHistory.putIfAbsent(snapshotVersion, get);
            }
            get.putIfAbsent(i, 0);
        }
//        AppConfig.timestampedErrorPrint("Give history CONTST: prvo " + giveHistory);
//        AppConfig.timestampedErrorPrint("Get history CONTST: prvo" + getHistory);
    }


    /*
     * This value is protected by AppConfig.colorLock.
     * Access it only if you have the blessing.
     */
    public int recordedAmount = 0;

    public void markerEvent(int collectorId, SnapshotCollector snapshotCollector, int iniatorId, int originalSender, int snapshotVersion) {
        synchronized (AppConfig.colorLock) {
            AppConfig.timestampedErrorPrint(AppConfig.snapshotVersion.get() + " chacha "+snapshotVersion);
            if ((AppConfig.parentId.get() == -1 || AppConfig.parentId.get() == iniatorId) || AppConfig.snapshotVersion.get() == snapshotVersion) {

              //  AppConfig.sveKomsijeSuOdgovorili.incrementAndGet();

                AppConfig.timestampedErrorPrint("init"+iniatorId +"\noriginalsender"+originalSender+"\ncollectorid"+collectorId);
                AppConfig.parentId.set(iniatorId);
                      AppConfig.supervisordId.set(-1);
                AppConfig.timestampedErrorPrint("SADA JE PARENT Parent je " + AppConfig.parentId.get() + " inicijator id " + iniatorId);

                AppConfig.rejectedList.clear();
                AppConfig.rejectedList.add(-2);
                AppConfig.parentMap.clear();
                // AppConfig.parentId.set(-1);
                //       AppConfig.supervisordId.set(-1);
                AppConfig.collectedLYValues.clear();
                AppConfig.sveKomsijeSuOdgovorili.set(0);
                AppConfig.test1.set(0);
            }
            if (AppConfig.supervisordId.get() == -1) {
                AppConfig.supervisordId.set(originalSender);
                AppConfig.timestampedErrorPrint("SADA JE PARENT Parent je " + AppConfig.parentId.get() + " inicijator id " + iniatorId);
            }
            recordedAmount = getCurrentBitcakeAmount();
            AppConfig.snapshotVersion.getAndIncrement();

            AppConfig.myServentInfo.setVersionId(AppConfig.snapshotVersion.get());


            LYSnapshotResult snapshotResult = new LYSnapshotResult(
                    AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory, AppConfig.snapshotVersion.get());

            AppConfig.timestampedStandardPrint("Slikao sam snapshot " + snapshotResult);
            //	AppConfig.timestampedStandardPrint("Recording my state: " + AppConfig.snapshotVersion.get() +" POSLE");


            snapshotCollector.addLYSnapshotInfo(
                    AppConfig.myServentInfo.getId(),
                    snapshotResult);


            AppConfig.timestampedErrorPrint("Parent je " + AppConfig.parentId.get() + " inicijator id " + iniatorId);

            if (AppConfig.supervisordId.get() == originalSender) {

                AppConfig.timestampedErrorPrint("SALJEM MARKERE");
                for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {


                    //        AppConfig.timestampedErrorPrint(AppConfig.rejectedList + " rejected list");


                    //	AppConfig.timestampedErrorPrint("Sending to neighbour: " + AppConfig.getInfoById(neighbor)+"-"+ " from "+ AppConfig.myServentInfo.getId() /*+" and version is"+ AppConfig.getInfoById(neighbor)*/);
                    if (AppConfig.supervisordId.get()!=neighbor ) {
                        AppConfig.timestampedErrorPrint("AAAAAAA USAO za for Parent je " + AppConfig.supervisordId.get() + "za servent negh id " + neighbor);


                        Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, AppConfig.snapshotVersion.get(), AppConfig.parentId.get());
                        MessageUtil.sendMessage(clMarker);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                }

            } else {
                AppConfig.timestampedErrorPrint("ODBIO za for Parent je " + AppConfig.parentId.get() + "za servent id " + AppConfig.myServentInfo.getId() + " za collector " + collectorId);

                LaiYangBitcakeManager lyFinancialManager =
                        (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
                lyFinancialManager.rejectEvent(iniatorId, snapshotVersion, snapshotCollector, originalSender);


            }
        }
    }

    public void rejectEvent(int iniatorId, int collectorId, SnapshotCollector snapshotCollector, int originalSender) {
        Message rejectMessage = new RejectMessage(
                AppConfig.myServentInfo, AppConfig.getInfoById(originalSender), AppConfig.snapshotVersion.get(), AppConfig.parentId.get());
        MessageUtil.sendMessage(rejectMessage);

    }


    AtomicInteger provera = new AtomicInteger(1);

    private void metoda() {

        if (provera.get() <= AppConfig.snapshotVersion.get()) {
//            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
//
//                int snapshotVersion = provera.get();
//
//                ConcurrentHashMap<Integer, Integer> give = giveHistory.get(snapshotVersion);
//                if (give == null) {
//                    give = new ConcurrentHashMap<>();
//                    giveHistory.putIfAbsent(snapshotVersion, give);
//                }
//                give.putIfAbsent(neighbor, 0);
//                ConcurrentHashMap<Integer, Integer> get = getHistory.get(snapshotVersion);
//                if (get == null) {
//                    get = new ConcurrentHashMap<>();
//                    getHistory.putIfAbsent(snapshotVersion, get);
//                }
//                get.putIfAbsent(neighbor, 0);
//            }

//            AppConfig.timestampedErrorPrint("Give history CONTST: " + giveHistory);
//            AppConfig.timestampedErrorPrint("Get history CONTST: " + getHistory);

            for (int i = 0; i < AppConfig.getServentCount(); i++) {

                int snapshotVersion = provera.get();

// For giveHistory
                ConcurrentHashMap<Integer, Integer> give = giveHistory.get(snapshotVersion);
                if (give == null) {
                    give = new ConcurrentHashMap<>();
                    giveHistory.putIfAbsent(snapshotVersion, give);
                }
                give.putIfAbsent(i, 0);

// For getHistory
                ConcurrentHashMap<Integer, Integer> get = getHistory.get(snapshotVersion);
                if (get == null) {
                    get = new ConcurrentHashMap<>();
                    getHistory.putIfAbsent(snapshotVersion, get);
                }
                get.putIfAbsent(i, 0);
            }


            provera.getAndIncrement();
        }
    }

    public void tellEvent(SnapshotCollector snapshotCollector, int initiatorId, int supervisordId,
                          ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap, String messageText) {

        if(AppConfig.myServentInfo.getId() == initiatorId) {
            AppConfig.timestampedErrorPrint("TELL EVENT: I am initiator, returning.");
            return;
        }

        LYSnapshotResult snapshotResult = new LYSnapshotResult(
                AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory, AppConfig.snapshotVersion.get());
        AppConfig.timestampedStandardPrint("Slikao sam snapshot " + snapshotResult);


        AppConfig.uzeteVrednosti.put(AppConfig.myServentInfo.getId(), snapshotResult);
AppConfig.timestampedStandardPrint("TELL SALJEM "+((SnapshotCollectorWorker)snapshotCollector).getCollectedLYValues()+" saljem "+supervisordId);
        Message tellMessage = new LYTellMessage(
                AppConfig.myServentInfo, AppConfig.getInfoById(supervisordId), messageText, snapshotResult, AppConfig.snapshotVersion.get(), AppConfig.parentId.get(),
                parentMap, Map.copyOf(((SnapshotCollectorWorker)snapshotCollector).getCollectedLYValues()));
        MessageUtil.sendMessage(tellMessage);
    }

    private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {

        private int valueToAdd;

        public MapValueUpdater(int valueToAdd) {
            this.valueToAdd = valueToAdd;
        }

        @Override
        public Integer apply(Integer key, Integer oldValue) {
            return oldValue + valueToAdd;
        }
    }

    AtomicBoolean foundGive = new AtomicBoolean(false);

    public void recordGiveTransaction(int neighbor, int amount, int snapshotVersion) {
        foundGive.set(false);
        metoda();
        for (int i = 0; i <= AppConfig.snapshotVersion.get(); i++) {
            //   giveHistory.putIfAbsent(i, new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Integer> give = giveHistory.get(i);
            if (give == null) {
                give = new ConcurrentHashMap<>();
                giveHistory.putIfAbsent(snapshotVersion, give);
            }
            give.putIfAbsent(neighbor, 0);
            if (snapshotVersion == AppConfig.snapshotVersion.get()) {

                giveHistory.get(i).compute(neighbor, new MapValueUpdater(amount));
                foundGive.set(true);
                //		AppConfig.timestampedErrorPrint("UPDATED Give history: "+ giveHistory.get(snapshotVersion)+" for "+ snapshotVersion);
            }
        }
    }

    AtomicBoolean foundGet = new AtomicBoolean(false);

    public void recordGetTransaction(int neighbor, int amount, int snapshotVersion) {

        metoda();

        // AppConfig.timestampedErrorPrint(AppConfig.snapshotVersion.get() + " OVDE JE");
        foundGet.set(false);
        //    AppConfig.timestampedErrorPrint("GET TRANSACTION: " + neighbor + " " + amount + " " + snapshotVersion + "\n" + getHistory);
        for (int i = 0; i <= AppConfig.snapshotVersion.get(); i++) {
            //   getHistory.get(i).putIfAbsent(neighbor, 0);
            //
            //    getHistory.putIfAbsent(i, new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Integer> get = getHistory.get(i);
            if (get == null) {
                get = new ConcurrentHashMap<>();
                getHistory.putIfAbsent(snapshotVersion, get);
            }
            getHistory.get(i).putIfAbsent(neighbor, 0);
            if (snapshotVersion == AppConfig.snapshotVersion.get()) {
                getHistory.get(i).compute(neighbor, new MapValueUpdater(amount));
                foundGet.set(true);

            }
            //      AppConfig.timestampedErrorPrint("UPDATED Get history: " + getHistory.get(snapshotVersion) + " for " + snapshotVersion);
        }

    }
}
