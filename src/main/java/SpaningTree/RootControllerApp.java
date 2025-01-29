package SpaningTree;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

// RootController Actor
class RootController extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef pController1;
    private ActorRef pController2;
    private ActorRef pController3;

    private byte rootID;
    private byte rootDistance;
    private byte myID;
    private boolean IamRoot;

    public RootController(ActorRef pController1, ActorRef pController2, ActorRef pController3) {
        this.pController1 = pController1;
        this.pController2 = pController2;
        this.pController3 = pController3;
    }

    static class Initial {
        public final byte id;

        public Initial(byte id) {
            this.id = id;
        }
    }

    static class RecvInf {
        public final byte senderID;
        public final byte distance;
        public final byte believedRootID;

        public RecvInf(byte senderID, byte distance, byte believedRootID) {
            this.senderID = senderID;
            this.distance = distance;
            this.believedRootID = believedRootID;
        }
    }

    static class Config {}

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Initial.class, msg -> {
                    myID = msg.id;
                    rootID = myID;
                    rootDistance = 0;
                    IamRoot = true;
                    self().tell(new Config(), self());
                })
                .match(RecvInf.class, msg -> {
                    if (msg.believedRootID < rootID) {
                        updateRootState(msg.believedRootID, (byte) (msg.distance + 1));
                        propagateInformation();
                    } else if (msg.believedRootID == rootID && msg.distance + 1 < rootDistance) {
                        updateRootState(msg.believedRootID, (byte) (msg.distance + 1));
                        propagateInformation();
                    } else if (!(msg.believedRootID == rootID && msg.distance + 1 == rootDistance)) {
                        self().tell(new Config(), self());
                    }
                })
                .match(Config.class, msg -> {
                    if (IamRoot) {
                        log.info("I am the root. Sending LAN messages.");
                        sendLanMessages((byte) 0, myID);
                    }
                })
                .build();
    }

    private void updateRootState(byte newRootID, byte newDistance) {
        rootID = newRootID;
        rootDistance = newDistance;
        IamRoot = false;
    }

    private void propagateInformation() {
        log.info("Propagating information with RootID: {}, RootDistance: {}", rootID, rootDistance);
        pController1.tell(new PortController.SetBadPort(), self());
        pController2.tell(new PortController.SetBadPort(), self());
        pController3.tell(new PortController.SetBadPort(), self());

        sendLanMessages(rootDistance, rootID);
    }

    private void sendLanMessages(byte distance, byte believedRootID) {
        pController1.tell(new PortController.SendLan(myID, distance, believedRootID), self());
        pController2.tell(new PortController.SendLan(myID, distance, believedRootID), self());
        pController3.tell(new PortController.SendLan(myID, distance, believedRootID), self());
    }
}

// PortController Actor
class PortController extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef rootController;
    private ActorRef bridgeToLan;

    private byte rootID = (byte) 255;
    private byte rootDistance = (byte) 255;
    private boolean isTheBestPort;

    public PortController(ActorRef rootController, ActorRef bridgeToLan) {
        this.rootController = rootController;
        this.bridgeToLan = bridgeToLan;
    }

    static class SetBestPort {}
    static class SetBadPort {}
    static class SendLan {
        public final byte senderID;
        public final byte distance;
        public final byte believedRootID;

        public SendLan(byte senderID, byte distance, byte believedRootID) {
            this.senderID = senderID;
            this.distance = distance;
            this.believedRootID = believedRootID;
        }
    }
    static class SendBridge {
        public final byte senderID;
        public final byte distance;
        public final byte believedRootID;

        public SendBridge(byte senderID, byte distance, byte believedRootID) {
            this.senderID = senderID;
            this.distance = distance;
            this.believedRootID = believedRootID;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SetBestPort.class, msg -> {
                    isTheBestPort = true;
                    log.info("Port set as best port.");
                })
                .match(SetBadPort.class, msg -> {
                    isTheBestPort = false;
                    log.info("Port set as bad port.");
                })
                .match(SendLan.class, msg -> {
                    if (msg.believedRootID < rootID || (msg.believedRootID == rootID && msg.distance < rootDistance)) {
                        bridgeToLan.tell(new BridgeToLanPort.TurnOn(), self());
                        bridgeToLan.tell(new BridgeToLanPort.Send(msg.senderID, msg.distance, msg.believedRootID), self());
                    } else if (!isTheBestPort) {
                        bridgeToLan.tell(new BridgeToLanPort.TurnOff(), self());
                    }
                })
                .match(SendBridge.class, msg -> {
                    if (msg.believedRootID < rootID || (msg.believedRootID == rootID && msg.distance < rootDistance)) {
                        rootID = msg.believedRootID;
                        rootDistance = (byte) (msg.distance + 1);
                    }
                    rootController.tell(new RootController.RecvInf(msg.senderID, msg.distance, msg.believedRootID), self());
                })
                .build();
    }
}

// BridgeToLanPort Actor
class BridgeToLanPort extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private boolean alive = true;

    static class TurnOn {}
    static class TurnOff {}
    static class Send {
        public final byte senderID;
        public final byte distance;
        public final byte believedRootID;

        public Send(byte senderID, byte distance, byte believedRootID) {
            this.senderID = senderID;
            this.distance = distance;
            this.believedRootID = believedRootID;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TurnOn.class, msg -> {
                    alive = true;
                    log.info("BridgeToLanPort turned ON.");
                })
                .match(TurnOff.class, msg -> {
                    alive = false;
                    log.info("BridgeToLanPort turned OFF.");
                })
                .match(Send.class, msg -> {
                    if (alive) {
                        log.info("Message sent to LAN: senderID={}, distance={}, believedRootID={}",
                                msg.senderID, msg.distance, msg.believedRootID);
                    }
                })
                .build();
    }
}

// Main Application
public class RootControllerApp {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("NetworkSystem");

        ActorRef bridgeToLan1 = system.actorOf(Props.create(BridgeToLanPort.class), "bridgeToLan1");
        ActorRef bridgeToLan2 = system.actorOf(Props.create(BridgeToLanPort.class), "bridgeToLan2");
        ActorRef bridgeToLan3 = system.actorOf(Props.create(BridgeToLanPort.class), "bridgeToLan3");

        ActorRef pController1 = system.actorOf(Props.create(PortController.class, null, bridgeToLan1), "pController1");
        ActorRef pController2 = system.actorOf(Props.create(PortController.class, null, bridgeToLan2), "pController2");
        ActorRef pController3 = system.actorOf(Props.create(PortController.class, null, bridgeToLan3), "pController3");

        ActorRef rootController = system.actorOf(Props.create(RootController.class, pController1, pController2, pController3), "rootController");

        rootController.tell(new RootController.Initial((byte) 1), ActorRef.noSender());
    }
}
