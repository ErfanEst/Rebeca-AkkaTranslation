package TrainController;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

// BridgeController Actor
class BridgeController extends AbstractActor {
    private boolean isOnBridge1;
    private boolean isOnBridge2;
    private boolean isWaiting1;
    private boolean isWaiting2;

    private ActorRef train1;
    private ActorRef train2;

    public BridgeController(ActorRef train1, ActorRef train2) {
        this.isOnBridge1 = false;
        this.isOnBridge2 = false;
        this.isWaiting1 = false;
        this.isWaiting2 = false;

        this.train1 = train1;
        this.train2 = train2;
    }

    static Props props(ActorRef train1, ActorRef train2) {
        return Props.create(BridgeController.class, () -> new BridgeController(train1, train2));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Messages.RegisterTrain.class, msg -> {
                    this.train1 = msg.train1;
                    this.train2 = msg.train2;
                })
                .match(Messages.Arrive.class, msg -> {
                    if (getSender().equals(train1)) {
                        if (!isOnBridge2) {
                            isOnBridge1 = true;
                            train1.tell(new Messages.YouMayPass(), getSelf());
                        } else {
                            isWaiting1 = true;
                        }
                    } else if (getSender().equals(train2)) {
                        if (!isOnBridge1) {
                            isOnBridge2 = true;
                            train2.tell(new Messages.YouMayPass(), getSelf());
                        } else {
                            isWaiting2 = true;
                        }
                    }
                })
                .match(Messages.Leave.class, msg -> {
                    if (getSender().equals(train1)) {
                        isOnBridge1 = false;
                        if (isWaiting2) {
                            isOnBridge2 = true;
                            train2.tell(new Messages.YouMayPass(), getSelf());
                            isWaiting2 = false;
                        }
                    } else if (getSender().equals(train2)) {
                        isOnBridge2 = false;
                        if (isWaiting1) {
                            isOnBridge1 = true;
                            train1.tell(new Messages.YouMayPass(), getSelf());
                            isWaiting1 = false;
                        }
                    }
                })
                .build();
    }
}

// Train Actor
class Train extends AbstractActor {

    boolean onTheBridge;
    private final ActorRef controller;

    public Train(ActorRef controller) {
        this.controller = controller;
        this.onTheBridge = false;
    }

    static Props props(ActorRef controller) {
        return Props.create(Train.class, () -> new Train(controller));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Messages.YouMayPass.class, msg -> {
                    onTheBridge = true;
                    System.out.println(getSelf().path().name() + " is passing the bridge.");
                    self().tell(new Messages.Passed(), getSelf());
                })
                .match(Messages.Passed.class, msg -> {
                    onTheBridge = false;
                    controller.tell(new Messages.Leave(), getSelf());
                    System.out.println(getSelf().path().name() + " has left the bridge.");
                    self().tell(new Messages.ReachBridge(), getSelf());
                })
                .match(Messages.ReachBridge.class, msg -> {
                    controller.tell(new Messages.Arrive(), getSelf());
                })
                .build();
    }
}

// Messages
class Messages {
    static class Arrive {}
    static class Leave {}
    static class YouMayPass {}
    static class Passed {}
    static class ReachBridge {}
    static class RegisterTrain {
        final ActorRef train1;
        final ActorRef train2;

        RegisterTrain(ActorRef train1, ActorRef train2) {
            this.train1 = train1;
            this.train2 = train2;
        }
    }
}

// Main Class
public class BridgeControlSystem {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("BridgeControlSystem");

//        // Create Train actors and pass the controller
//        ActorRef train1 = system.actorOf(Train.props(bridgeController), "train1");
//        ActorRef train2 = system.actorOf(Train.props(bridgeController), "train2");
//
//        // Create BridgeController actor
//        ActorRef bridgeController = system.actorOf(BridgeController.props(train1, train2), "bridgeController");
//
//        // Register trains with the BridgeController
//        bridgeController.tell(new Messages.RegisterTrain(train1, train2), ActorRef.noSender());
//
//        // Start the trains
//        train1.tell(new Messages.ReachBridge(), ActorRef.noSender());
//        train2.tell(new Messages.ReachBridge(), ActorRef.noSender());
    }
}
