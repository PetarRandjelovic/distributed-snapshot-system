package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class RejectMessage extends BasicMessage {


    public RejectMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,  int snapshotVersion, int initiatorId) {
        super(MessageType.REJECT, originalSenderInfo, receiverInfo, snapshotVersion, initiatorId);
    }
}
