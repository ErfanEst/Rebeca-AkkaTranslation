package SenderReceiver;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import java.util.Random;

public class SenderActor extends AbstractActor {
    // Messages
    public static class Setup {
        public final ActorRef medium;
        public final ActorRef receiver;

        public Setup(ActorRef medium, ActorRef receiver) {
            this.medium = medium;
            this.receiver = receiver;
        }
    }

    public static class Initial {}
    public static class SendMsg {}

    // State (1:1 with Rebeca)
    private ActorRef medium;
    private ActorRef receiver;
    private boolean receivedBit;
    private boolean sendBit;
    private boolean hasSucceeded;

    public static Props props() {
        return Props.create(SenderActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Initial.class, this::handleInitial)
                .match(SendMsg.class, this::handleSendMsg)
                .build();
    }

    private void handleSetup(Setup setup) {
        this.medium = setup.medium;
        this.receiver = setup.receiver;
    }

    private void handleInitial(Initial msg) {
        sendBit = false;
        medium.tell(new MediumActor.Pass(sendBit), getSelf());
        getSelf().tell(new SendMsg(), getSelf());
    }

    private void handleSendMsg(SendMsg msg) {
        if (hasSucceeded) {
            sendBit = !sendBit;
        }
        medium.tell(new MediumActor.Pass(sendBit), getSelf());
        getSelf().tell(new SendMsg(), getSelf());
    }
}

class MediumActor extends AbstractActor {
    // Messages
    public static class Setup {
        public final ActorRef receiver;
        public final ActorRef sender;

        public Setup(ActorRef receiver, ActorRef sender) {
            this.receiver = receiver;
            this.sender = sender;
        }
    }

    public static class Pass {
        public final boolean msgBit;

        public Pass(boolean msgBit) {
            this.msgBit = msgBit;
        }
    }

    public static class Initial {}

    // State (1:1 with Rebeca)
    private ActorRef receiver;
    private ActorRef sender;
    private boolean passMessage;
    private final Random random = new Random();

    public static Props props() {
        return Props.create(MediumActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Pass.class, this::handlePass)
                .match(Initial.class, this::handleInitial)
                .build();
    }

    private void handleSetup(Setup setup) {
        this.receiver = setup.receiver;
        this.sender = setup.sender;
    }

    private void handleInitial(Initial msg) {
        passMessage = true; // Exact initial state
    }

    private void handlePass(Pass msg) {
        // Implement probabilistic choice (?(true,false))
        passMessage = random.nextBoolean();

        if(passMessage) {
            receiver.tell(new ReceiverActor.ReceiveMsg(msg.msgBit), getSelf());
        }
        // Else clause exactly matches Rebeca code's empty else
    }
}

class ReceiverActor extends AbstractActor {
    // Messages
    public static class Setup {
        public final ActorRef medium;
        public final ActorRef sender;

        public Setup(ActorRef medium, ActorRef sender) {
            this.medium = medium;
            this.sender = sender;
        }
    }

    public static class ReceiveMsg {
        public final boolean msgBit;

        public ReceiveMsg(boolean msgBit) {
            this.msgBit = msgBit;
        }
    }

    public static class Initial {}

    // State (1:1 with Rebeca)
    boolean messageBit;
    private ActorRef medium;
    private ActorRef sender;

    public static Props props() {
        return Props.create(ReceiverActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(ReceiveMsg.class, this::handleReceiveMsg)
                .match(Initial.class, this::handleInitial)
                .build();
    }

    private void handleSetup(Setup setup) {
        this.medium = setup.medium;
        this.sender = setup.sender;
    }

    private void handleInitial(Initial msg) {
    }

    private void handleReceiveMsg(ReceiveMsg msg) {
        messageBit = msg.msgBit;
        // Matches commented out sender.receive(true) in Rebeca
    }
}

