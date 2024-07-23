package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;

import java.util.concurrent.CopyOnWriteArrayList;

public class BitcakeInfoCommand implements CLICommand {

    private SnapshotCollector collector;

    public BitcakeInfoCommand(SnapshotCollector collector) {
        this.collector = collector;
    }

    @Override
    public String commandName() {
        return "bitcake_info";
    }

    @Override
    public void execute(String args) {

        if (AppConfig.inicijatorListApp.contains(AppConfig.myServentInfo.getId())) {


            AppConfig.parentId.set(AppConfig.myServentInfo.getId());
            AppConfig.supervisordId.set(AppConfig.myServentInfo.getId());
            CopyOnWriteArrayList<Integer> parentList = new CopyOnWriteArrayList<>();
            parentList.add(AppConfig.myServentInfo.getId());
            AppConfig.parentMap.put(AppConfig.myServentInfo.getId(), parentList);

            while (!AppConfig.collectedLYValues.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            AppConfig.parentMap.clear();
            AppConfig.startCountingNowOvo.set(false);
            AppConfig.parentMap.computeIfAbsent(AppConfig.myServentInfo.getId(), k -> new CopyOnWriteArrayList<>()).add(AppConfig.myServentInfo.getId());
            collector.startCollecting();
        } else {
            AppConfig.timestampedErrorPrint("Not initiator, can't start snapshot collection.");
        }
    }

}
