package servent.message.snapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	private LYSnapshotResult lySnapshotResult;
	private Map<Integer,LYSnapshotResult> lySnapshotResultMap;
	public LYTellMessage(ServentInfo sender, ServentInfo receiver,String messageText ,LYSnapshotResult lySnapshotResult, int snapshotVersion, int initiatorId, ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap
			,Map<Integer,LYSnapshotResult> lysnapshotResultMap) {
		super(MessageType.LY_TELL, sender, receiver,messageText, snapshotVersion, initiatorId,parentMap);
		this.lySnapshotResultMap=lysnapshotResultMap;
		this.lySnapshotResult = lySnapshotResult;
	}

	public LYSnapshotResult getLYSnapshotResult() {
		return lySnapshotResult;
	}

	@Override
	public Map<Integer, LYSnapshotResult> getLySnapshotResultMap() {
		return lySnapshotResultMap;
	}

	@Override
	public String toString() {
		return "{" +
				"lySnapshotResultMap=" + lySnapshotResultMap +
				'}' +super.toString();
	}
}
