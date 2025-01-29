package LeaderElectionLCR;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Node extends AbstractActor {

    // Message classes
    public static class Setup {
        private final ActorRef rightNode;

        public Setup(ActorRef rightNode) {
            this.rightNode = rightNode;
        }

        public ActorRef getRightNode() {
            return rightNode;
        }
    }

    public static class Initial {
        private final int n;

        public Initial(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }
    }

    public static class Send {}

    public static class Receive {
        private final int n;

        public Receive(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }
    }

    public static class ImLeader {}

    // Actor state
    private ActorRef rightNode;
    private boolean isLeader;
    private int myNumber;
    private int currentLeader;

    public static Props props() {
        return Props.create(Node.class);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Initial.class, this::handleInitial)
                .match(Send.class, this::handleSend)
                .match(Receive.class, this::handleReceive)
                .match(ImLeader.class, this::handleImLeader)
                .build();
    }

    private void handleSetup(Setup setup) {
        rightNode = setup.getRightNode();
    }

    private void handleInitial(Initial initial) {
        myNumber = initial.getN();
        currentLeader = myNumber;
        isLeader = false;
        getSelf().tell(new Send(), getSelf());
    }

    private void handleSend(Send send) {
        rightNode.tell(new Receive(currentLeader), getSelf());
    }

    private void handleReceive(Receive receive) {
        int n = receive.getN();
        if (n == myNumber) {
            isLeader = true;
            getSelf().tell(new ImLeader(), getSelf());
        } else if (n > currentLeader) {
            currentLeader = n;
            getSelf().tell(new Send(), getSelf());
        }
        // Else discard (do nothing)
    }

    private void handleImLeader(ImLeader imLeader) {
        // Keep sending ImLeader to avoid termination
        getSelf().tell(new ImLeader(), getSelf());
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("LeaderElectionSystem");

        // Create nodes
        ActorRef node0 = system.actorOf(Node.props(), "node0");
        ActorRef node1 = system.actorOf(Node.props(), "node1");
        ActorRef node2 = system.actorOf(Node.props(), "node2");

        // Setup node connections
        node0.tell(new Setup(node2), ActorRef.noSender());
        node1.tell(new Setup(node0), ActorRef.noSender());
        node2.tell(new Setup(node1), ActorRef.noSender());

        // Initialize nodes with their numbers
        node0.tell(new Initial(4), ActorRef.noSender());
        node1.tell(new Initial(20), ActorRef.noSender());
        node2.tell(new Initial(10), ActorRef.noSender());
    }
}