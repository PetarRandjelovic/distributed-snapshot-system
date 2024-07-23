package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modificator methods.
 * If you don't override the modificators, you might drop stuff.
 *
 * @author bmilojkovic
 */
public class BasicMessage implements Message {

    private static final long serialVersionUID = -9075856313609777945L;
    private final MessageType type;
    private final ServentInfo originalSenderInfo;
    private final ServentInfo receiverInfo;
    private final List<ServentInfo> routeList;
    private final String messageText;
    private final boolean white;
    private final int snapshotVersion;
    private  int initiatorId;
    private ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap;
    private Map<Integer, LYSnapshotResult> lySnapshotResultMap;
    //This gives us a unique id - incremented in every natural constructor.
    private static AtomicInteger messageCounter = new AtomicInteger(0);
    private final int messageId;
    private   boolean sendback;


    public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, int snapshotVersion, int iniatorId) {
        this.type = type;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.white = AppConfig.isWhite.get();
        this.routeList = new ArrayList<>();
        this.messageText = "";
        this.messageId = messageCounter.getAndIncrement();
        this.snapshotVersion = snapshotVersion;
        this.initiatorId = iniatorId;
    }

    public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                        String messageText, int snapshotVersion, int iniatorId) {
        this.type = type;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.white = AppConfig.isWhite.get();
        this.routeList = new ArrayList<>();
        this.messageText = messageText;
        this.messageId = messageCounter.getAndIncrement();
        this.snapshotVersion = snapshotVersion;
        this.initiatorId = iniatorId;
    }

    public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                        String messageText, int snapshotVersion, int iniatorId, ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap) {
        this.type = type;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.white = AppConfig.isWhite.get();
        this.routeList = new ArrayList<>();
        this.messageText = messageText;
        this.messageId = messageCounter.getAndIncrement();
        this.snapshotVersion = snapshotVersion;
        this.initiatorId = iniatorId;
        this.parentMap = parentMap;
    }

//jej
    public BasicMessage(MessageType messageType, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                        int snapshotVersion, int initiatorId, Map<Integer,
            LYSnapshotResult> lySnapshotResultMap, ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap,boolean sendback) {
        this.type = messageType;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.white = AppConfig.isWhite.get();
        this.routeList = new ArrayList<>();
        this.messageText = "nesto";
        this.messageId = messageCounter.getAndIncrement();
        this.snapshotVersion = snapshotVersion;
        this.initiatorId = initiatorId;
        this.lySnapshotResultMap = lySnapshotResultMap;
        this.parentMap = parentMap;
        this.sendback=sendback;
    }

    @Override
    public MessageType getMessageType() {
        return type;
    }

    @Override
    public ServentInfo getOriginalSenderInfo() {
        return originalSenderInfo;
    }

    @Override
    public ServentInfo getReceiverInfo() {
        return receiverInfo;
    }

    @Override
    public boolean isWhite() {
        return white;
    }


    @Override
    public List<ServentInfo> getRoute() {
        return routeList;
    }

    @Override
    public String getMessageText() {
        return messageText;
    }

    @Override
    public int getMessageId() {
        return messageId;
    }

    public Map<Integer, LYSnapshotResult> getLySnapshotResultMap() {
        return lySnapshotResultMap;
    }

    protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                           boolean white, List<ServentInfo> routeList, String messageText, int messageId, int snapshotVersion, int initiatorId, ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> parentMap) {
        this.type = type;
        this.originalSenderInfo = originalSenderInfo;
        this.receiverInfo = receiverInfo;
        this.white = white;
        this.routeList = routeList;
        this.messageText = messageText;
        this.messageId = messageId;
        this.snapshotVersion = snapshotVersion;
        this.initiatorId = initiatorId;
        this.parentMap = parentMap;
    }

    public int getInitiatorId() {
        return initiatorId;
    }

    @Override
    public ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> getConcurentParentMap() {
        return parentMap;
    }

    public void setInitiatorId(int initiatorId) {
        this.initiatorId = initiatorId;
    }

    /**
     * Used when resending a message. It will not change the original owner
     * (so equality is not affected), but will add us to the route list, so
     * message path can be retraced later.
     */




//    @Override
//    public Message makeMeASender() {
//        ServentInfo newRouteItem = AppConfig.myServentInfo;
//
//        List<ServentInfo> newRouteList = new ArrayList<>(routeList);
//        newRouteList.add(newRouteItem);
//        Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
//                getReceiverInfo(), isWhite(), newRouteList, getMessageText(), getMessageId(),getSnapshotVersion(),getInitiatorId());
//
//        return toReturn;
//    }

    /**
     * Change the message received based on ID. The receiver has to be our neighbor.
     * Use this when you want to send a message to multiple neighbors, or when resending.
     */
//    @Override
//    public Message changeReceiver(Integer newReceiverId) {
//        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
//            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
//
//            Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
//                    newReceiverInfo, isWhite(), getRoute(), getMessageText(), getMessageId(),getSnapshotVersion(),getInitiatorId());
//
//            return toReturn;
//        } else {
//            AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
//
//            return null;
//        }
//
//    }

//    @Override
//    public Message setRedColor() {
//        Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
//                getReceiverInfo(), false, getRoute(), getMessageText(), getMessageId(),getSnapshotVersion(),getInitiatorId());
//
//        return toReturn;
//    }
//
//    @Override
//    public Message setWhiteColor() {
//        Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
//                getReceiverInfo(), true, getRoute(), getMessageText(), getMessageId(),getSnapshotVersion(),getInitiatorId());
//
//        return toReturn;
//    }

    /**
     * Comparing messages is based on their unique id and the original sender id.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicMessage) {
            BasicMessage other = (BasicMessage) obj;

            if (getMessageId() == other.getMessageId() &&
                    getOriginalSenderInfo().getId() == other.getOriginalSenderInfo().getId()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Hash needs to mirror equals, especially if we are gonna keep this object
     * in a set or a map. So, this is based on message id and original sender id also.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getMessageId(), getOriginalSenderInfo().getId());
    }

    /**
     * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
     */
    @Override
    public String toString() {
        return "[" + getOriginalSenderInfo().getId() + "|" + getMessageId() + "|" +
                getMessageText() + "|" + getMessageType() + "|" +
                getReceiverInfo().getId() + " Version id: "+ getReceiverInfo().getVersionId()+"]"+" "+getParentMap()+" app con "+AppConfig.collectedLYValues;
    }

    /**
     * Empty implementation, which will be suitable for most messages.
     */
    @Override
    public void sendEffect() {

    }

    @Override
    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    @Override
    public ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> getParentMap() {
        return parentMap;
    }


    public boolean isSendBack() {
        return sendback;
     //   return false;
    }
}
