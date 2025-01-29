package SenderReceiver;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class SenderReceiver {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ProtocolSystem");

        // Create actors with exact rebec relationships
        ActorRef sender = system.actorOf(SenderActor.props(), "sender");
        ActorRef medium = system.actorOf(MediumActor.props(), "medium");
        ActorRef receiver = system.actorOf(ReceiverActor.props(), "receiver");

        // Setup knownrebecs exactly as in main{} section
        sender.tell(new SenderActor.Setup(medium, receiver), ActorRef.noSender());
        medium.tell(new MediumActor.Setup(receiver, sender), ActorRef.noSender());
        receiver.tell(new ReceiverActor.Setup(medium, sender), ActorRef.noSender());

        // Initialize system
        sender.tell(new SenderActor.Initial(), ActorRef.noSender());
        medium.tell(new MediumActor.Initial(), ActorRef.noSender());
        receiver.tell(new ReceiverActor.Initial(), ActorRef.noSender());
    }
}
