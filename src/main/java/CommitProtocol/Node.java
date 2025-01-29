package CommitProtocol;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import java.util.Arrays;
import java.util.Random;

public class Node extends AbstractActor {
    private ActorRef node1;
    private ActorRef node2;
    private byte srvNo;
    private boolean recievedResults;      // Matches Rebeca's typo
    private boolean creationAbility;
    private int recievedResultsCounter;   // Matches Rebeca's typo
    private int expectedResultsCounter;
    private boolean[] cooperatorKnownRebecs = new boolean[2];

    // Messages (unchanged from original Rebeca logic)
    public static class InitialMessage {
        private final boolean creationAbility;
        public InitialMessage(boolean creationAbility) {
            this.creationAbility = creationAbility;
        }
        public boolean isCreationAbility() {
            return creationAbility;
        }
    }

    public static class Setup {
        private final ActorRef node1;
        private final ActorRef node2;
        public Setup(ActorRef node1, ActorRef node2) {
            this.node1 = node1;
            this.node2 = node2;
        }
        public ActorRef getNode1() {
            return node1;
        }
        public ActorRef getNode2() {
            return node2;
        }
    }

    public static class CreateTransaction {}
    public static class StartGlobalTransaction {}

    public static class CooperatorResponse {
        private final boolean result;
        public CooperatorResponse(boolean result) {
            this.result = result;
        }
        public boolean isResult() {
            return result;
        }
    }

    public static class ApplyResult {
        private final boolean result;
        public ApplyResult(boolean result) {
            this.result = result;
        }
        public boolean isResult() {
            return result;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(InitialMessage.class, this::handleInitial)
                .match(CreateTransaction.class, this::handleCreateTransaction)
                .match(StartGlobalTransaction.class, this::handleStartGlobalTransaction)
                .match(CooperatorResponse.class, this::handleCooperatorResponse)
                .match(ApplyResult.class, this::handleApplyResult)
                .build();
    }

    private void handleSetup(Setup msg) {
        this.node1 = msg.getNode1();
        this.node2 = msg.getNode2();
    }

    private void handleInitial(InitialMessage msg) {
        this.creationAbility = msg.isCreationAbility();
        getSelf().tell(new CreateTransaction(), getSelf());
    }

    private void handleCreateTransaction(CreateTransaction msg) {
        Random random = new Random();
        boolean startTrans = random.nextBoolean();

        if (startTrans && creationAbility) {
            Arrays.fill(cooperatorKnownRebecs, false);
            recievedResults = true;
            recievedResultsCounter = 0;
            expectedResultsCounter = 0;

            this.srvNo = 1;
            // Mirror Rebeca's non-deterministic cooperator selection
            if (random.nextBoolean()) {
                cooperatorKnownRebecs[0] = true;
                expectedResultsCounter++;
                node1.tell(new StartGlobalTransaction(), getSelf());
            }
            if (random.nextBoolean()) {
                cooperatorKnownRebecs[1] = true;
                expectedResultsCounter++;
                node2.tell(new StartGlobalTransaction(), getSelf());
            }

            // Local result (matches Rebeca's self.cooperatorResponse)
            expectedResultsCounter++;
            boolean localResult = random.nextBoolean();
            getSelf().tell(new CooperatorResponse(localResult), getSelf());
        } else {
            getSelf().tell(new CreateTransaction(), getSelf());
        }
    }

    // Matches Rebeca's sender-checking logic
    private void handleStartGlobalTransaction(StartGlobalTransaction msg) {
        boolean result = new Random().nextBoolean();
        ActorRef sender = getSender();
        if (sender.equals(node1)) {
            node1.tell(new CooperatorResponse(result), getSelf());
        } else if (sender.equals(node2)) {
            node2.tell(new CooperatorResponse(result), getSelf());
        }
    }

    private void handleCooperatorResponse(CooperatorResponse msg) {
        recievedResultsCounter++;
        if (!msg.isResult()) {
            recievedResults = false;
        }

        if (recievedResultsCounter == expectedResultsCounter) {
            if (cooperatorKnownRebecs[0]) {
                node1.tell(new ApplyResult(recievedResults), getSelf());
            }
            if (cooperatorKnownRebecs[1]) {
                node2.tell(new ApplyResult(recievedResults), getSelf());
            }
            getSelf().tell(new CreateTransaction(), getSelf());
        }
    }

    private void handleApplyResult(ApplyResult msg) {
        // No action (matches Rebeca)
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("TwoPhaseCommit");

        ActorRef node1 = system.actorOf(Props.create(Node.class), "node1");
        ActorRef node2 = system.actorOf(Props.create(Node.class), "node2");
        ActorRef node3 = system.actorOf(Props.create(Node.class), "node3");

        // Setup relationships (matches Rebeca's main block)
        node1.tell(new Setup(node2, node3), ActorRef.noSender());
        node2.tell(new Setup(node3, node1), ActorRef.noSender());
        node3.tell(new Setup(node1, node2), ActorRef.noSender());

        // After Setup is confirmed, send InitialMessage
        // (In Akka, messages to the same actor are processed in order)
        node1.tell(new InitialMessage(true), ActorRef.noSender());
        node2.tell(new InitialMessage(false), ActorRef.noSender());
        node3.tell(new InitialMessage(true), ActorRef.noSender());
    }
}
