package ProducerConsumer;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ProducerConsumerSystem");

        // Create actors
        ActorRef buffer = system.actorOf(BufferManagerActor.props(), "buffer");
        ActorRef producer = system.actorOf(ProducerActor.props(), "producer");
        ActorRef consumer = system.actorOf(ConsumerActor.props(), "consumer");

        // Set up relationships
        buffer.tell(new BufferManagerActor.Setup(producer, consumer), ActorRef.noSender());
        producer.tell(new ProducerActor.Setup(buffer), ActorRef.noSender());
        consumer.tell(new ConsumerActor.Setup(buffer), ActorRef.noSender());

        // Initialize system
        buffer.tell(new BufferManagerActor.Initial(), ActorRef.noSender());
        producer.tell(new ProducerActor.Initial(), ActorRef.noSender());
        consumer.tell(new ConsumerActor.Initial(), ActorRef.noSender());
    }
}
