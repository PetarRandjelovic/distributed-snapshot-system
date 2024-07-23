package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.MeetingMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MeetingHandler implements MessageHandler {


    private Message clientMessage;
    private Map<Integer, LYSnapshotResult> collectedLYValues;
    private SnapshotCollector snapshotCollector;



    private Map<Integer, Boolean> isteVrednosti= new ConcurrentHashMap<>();

    public MeetingHandler(Message clientMessage, Map<Integer, LYSnapshotResult> collectedLYValues, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.collectedLYValues = collectedLYValues;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {

        //    AppConfig.timestampedErrorPrint("MeetingHandler poslato od "+clientMessage.getOriginalSenderInfo().getId()+" serventu "+clientMessage.getOriginalSenderInfo().getId()+" "+ clientMessage.getLySnapshotResultMap());

        AppConfig.timestampedErrorPrint("MOJA MAPA JE " + AppConfig.parentMap);


//else {
        if (clientMessage.getMessageType() == MessageType.MEETING) {



        //    if(collectedLYValues)

                AppConfig.timestampedErrorPrint("MeetingHandler poslato od " + clientMessage.getInitiatorId() +
                        " klijent mapa " + clientMessage.getParentMap() + " APP MAPA JE" + AppConfig.parentMap);

                for (ConcurrentHashMap.Entry<Integer, CopyOnWriteArrayList<Integer>> entry : clientMessage.getParentMap().entrySet()) {
                    AppConfig.parentMap.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArrayList<>()).add(entry.getKey());
                    for (Integer key : entry.getValue()) {
                        AppConfig.parentMap.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArrayList<>()).add(key);

                    }
                    //   AppConfig.parentMap.computeIfAbsent(entry.getKey(), k -> entry.getValue());
                    //      if (AppConfig.parentId.get() == AppConfig.myServentInfo.getId()) {
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
//                AppConfig.timestampedErrorPrint("POSLEEE MeetingHandler poslato od " + clientMessage.getInitiatorId() +
//                        " klijent mapa " + clientMessage.getParentMap());

                AppConfig.timestampedStandardPrint(clientMessage.getLySnapshotResultMap().toString());
                for (Map.Entry<Integer, LYSnapshotResult> entry : clientMessage.getLySnapshotResultMap().entrySet()) {
//AppConfig.timestampedErrorPrint("PRAVIM key "+ entry.getKey()+" value "+entry.getValue());

                    snapshotCollector.addLYSnapshotInfo(
                            entry.getKey(),
                            entry.getValue());
                }

            AppConfig.timestampedErrorPrint("MAPA U MEETING JE "+AppConfig.parentMap);
            AppConfig.timestampedErrorPrint("SNAPSHOOT U MEETING JE "+((SnapshotCollectorWorker)snapshotCollector).getCollectedLYValues());

            if(clientMessage.isSendBack())
            {
                AppConfig.brojacZaMeeting.incrementAndGet();
                AppConfig.timestampedErrorPrint("DOBAR SAM "+ clientMessage.getOriginalSenderInfo().getId()+" vel "+AppConfig.parentMap.size()+" counter "+ AppConfig.brojacZaMeeting.get());


            }

            if( AppConfig.brojacZaMeeting.get()==AppConfig.parentMap.size()-1){
                AppConfig.timestampedErrorPrint("KRENI DA RACUNAS");
                AppConfig.startCountingNowOvo.set(true);
            }
//            for (Integer entry : clientMessage.getParentMap().keySet()) {
//                isteVrednosti.putIfAbsent(entry, clientMessage.isSendBack());
//                isteVrednosti.put(entry, clientMessage.isSendBack());
//            }
//            boolean flag=true;
//            for(Integer entry: isteVrednosti.keySet()){
//                AppConfig.timestampedErrorPrint("ENTRY za iste vrednosti "+entry+" boolean "+ isteVrednosti.get(entry));
//
//                if(isteVrednosti.get(entry)==false){
//                flag=false;
//                }
//            }

//            if(flag){
//                AppConfig.canStartCounting.set(true);
//            }

        }



    }

    public static boolean areMapsEqualLY(Map<Integer, LYSnapshotResult> map1,
                                         Map<Integer, LYSnapshotResult> map2) {
        if (map1 == map2)
            return true;
        if (map1 == null || map2 == null || map1.size() != map2.size())
            return false;

        for (Map.Entry<Integer, LYSnapshotResult> entry : map1.entrySet()) {
            Integer key = entry.getKey();
            LYSnapshotResult result1 = entry.getValue();
            LYSnapshotResult result2 = map2.get(key);

            if (result2 == null || !areLYSnapshotResultsEqual(result1, result2))
                return false;
        }
        return true;
    }

    private static boolean areLYSnapshotResultsEqual(LYSnapshotResult result1, LYSnapshotResult result2) {
        if (result1 == result2)
            return true;
        if (result1 == null || result2 == null)
            return false;

        return result1.getServentId() == result2.getServentId() &&
                result1.getRecordedAmount() == result2.getRecordedAmount() &&
                areMapsEqualGetGive(result1.getGiveHistory(), result2.getGiveHistory()) &&
                areMapsEqualGetGive(result1.getGetHistory(), result2.getGetHistory()) &&
                result1.getSnapshotVersion() == result2.getSnapshotVersion();
    }

    public static boolean areMapsEqual(ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> map1,
                                       ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> map2) {
        if (map1 == map2)
            return true;
        if (map1 == null || map2 == null || map1.size() != map2.size())
            return false;

        for (Integer key : map1.keySet()) {
            if (!map2.containsKey(key))
                return false;

            CopyOnWriteArrayList<Integer> list1 = map1.get(key);
            CopyOnWriteArrayList<Integer> list2 = map2.get(key);

            if (list1.size() != list2.size())
                return false;

            for (Integer value : list1) {
                if (!list2.contains(value))
                    return false;
            }
        }
        return true;
    }

    public static boolean areMapsEqualGetGive(ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> map1,
                                              ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> map2) {
        if (map1 == map2)
            return true;
        if (map1 == null || map2 == null || map1.size() != map2.size())
            return false;

        for (Map.Entry<Integer, ConcurrentHashMap<Integer, Integer>> entry : map1.entrySet()) {
            Integer key = entry.getKey();
            ConcurrentHashMap<Integer, Integer> innerMap1 = entry.getValue();
            ConcurrentHashMap<Integer, Integer> innerMap2 = map2.get(key);

            if (innerMap2 == null || !areInnerMapsEqualGetGive(innerMap1, innerMap2))
                return false;
        }
        return true;
    }

    private static boolean areInnerMapsEqualGetGive(ConcurrentHashMap<Integer, Integer> innerMap1,
                                                    ConcurrentHashMap<Integer, Integer> innerMap2) {
        if (innerMap1 == innerMap2)
            return true;
        if (innerMap1 == null || innerMap2 == null || innerMap1.size() != innerMap2.size())
            return false;

        for (Map.Entry<Integer, Integer> entry : innerMap1.entrySet()) {
            Integer key = entry.getKey();
            Integer value1 = entry.getValue();
            Integer value2 = innerMap2.get(key);

            if (!innerMap2.containsKey(key) || !value1.equals(value2))
                return false;
        }
        return true;
    }
}


