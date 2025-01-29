package LeaderElectionHS;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Node extends AbstractActor {

    // Message classes
    public static class Setup {
        private final ActorRef nodeL;
        private final ActorRef nodeR;

        public Setup(ActorRef nodeL, ActorRef nodeR) {
            this.nodeL = nodeL;
            this.nodeR = nodeR;
        }

        public ActorRef getNodeL() {
            return nodeL;
        }

        public ActorRef getNodeR() {
            return nodeR;
        }
    }

    public static class Initial {
        private final int id;

        public Initial(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public static class Arrive {}

    public static class Receive {
        private final int msgId;
        private final boolean inOut;
        private final int hopCount;

        public Receive(int msgId, boolean inOut, int hopCount) {
            this.msgId = msgId;
            this.inOut = inOut;
            this.hopCount = hopCount;
        }

        public int getMsgId() {
            return msgId;
        }

        public boolean isInOut() {
            return inOut;
        }

        public int getHopCount() {
            return hopCount;
        }
    }

    // Actor state
    private ActorRef nodeL;
    private ActorRef nodeR;
    private boolean monitor;
    private int myId;
    private int phase;
    private int monitorId;
    private boolean receivedLeft;
    private boolean receivedRight;

    public static Props props() {
        return Props.create(Node.class);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Initial.class, this::handleInitial)
                .match(Arrive.class, this::handleArrive)
                .match(Receive.class, this::handleReceive)
                .build();
    }

    private void handleSetup(Setup setup) {
        nodeL = setup.getNodeL();
        nodeR = setup.getNodeR();
    }

    private void handleInitial(Initial initial) {
        myId = initial.getId();
        monitor = false;
        monitorId = myId;
        phase = 1;
        receivedLeft = false;
        receivedRight = false;
        getSelf().tell(new Arrive(), getSelf());
    }

    private void handleArrive(Arrive arrive) {
        nodeL.tell(new Receive(myId, true, phase), getSelf());
        nodeR.tell(new Receive(myId, true, phase), getSelf());
    }

    private void handleReceive(Receive receive) {
        int msgId = receive.getMsgId();
        boolean inOut = receive.isInOut();
        int hopCount = receive.getHopCount();
        ActorRef sender = getSender();

        if (sender.equals(nodeL) && inOut) {
            if (msgId <= monitorId && hopCount > 1) {
                monitorId = msgId;
                nodeR.tell(new Receive(msgId, true, hopCount - 1), getSelf());
            } else if (msgId <= monitorId && hopCount == 1) {
                monitorId = msgId;
                nodeL.tell(new Receive(msgId, false, 1), getSelf());
            } else if (msgId == myId) {
                monitor = true;
                monitorId = myId;
            }
        }

        if (sender.equals(nodeR) && inOut) {
            if (msgId <= monitorId && hopCount > 1) {
                monitorId = msgId;
                nodeL.tell(new Receive(msgId, true, hopCount - 1), getSelf());
            } else if (msgId <= monitorId && hopCount == 1) {
                monitorId = msgId;
                nodeR.tell(new Receive(msgId, false, 1), getSelf());
            } else if (msgId == myId) {
                monitor = true;
                monitorId = myId;
            }
        }

        if (sender.equals(nodeL) && !inOut && msgId != myId) {
            nodeR.tell(new Receive(msgId, false, 1), getSelf());
        }

        if (sender.equals(nodeR) && !inOut && msgId != myId) {
            nodeL.tell(new Receive(msgId, false, 1), getSelf());
        }

        if (sender.equals(nodeL) && !inOut && msgId == myId && hopCount == 1) {
            receivedLeft = true;
        }

        if (sender.equals(nodeR) && !inOut && msgId == myId && hopCount == 1) {
            receivedRight = true;
        }

        if (receivedLeft && receivedRight && phase < 3) {
            if (phase == 2) {
                monitor = true;
            } else {
                phase *= 2;
                receivedLeft = false;
                receivedRight = false;
                nodeL.tell(new Receive(myId, true, phase), getSelf());
                nodeR.tell(new Receive(myId, true, phase), getSelf());
            }
        }
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("NodeSystem");

        // Create nodes
        ActorRef node1 = system.actorOf(Node.props(), "node1");
        ActorRef node2 = system.actorOf(Node.props(), "node2");
        ActorRef node3 = system.actorOf(Node.props(), "node3");
        ActorRef node4 = system.actorOf(Node.props(), "node4");

        // Setup node connections
        node1.tell(new Setup(node4, node2), ActorRef.noSender());
        node2.tell(new Setup(node1, node3), ActorRef.noSender());
        node3.tell(new Setup(node2, node4), ActorRef.noSender());
        node4.tell(new Setup(node3, node1), ActorRef.noSender());

        // Initialize nodes
        node1.tell(new Initial(1), ActorRef.noSender());
        node2.tell(new Initial(2), ActorRef.noSender());
        node3.tell(new Initial(3), ActorRef.noSender());
        node4.tell(new Initial(4), ActorRef.noSender());
    }
}