package ProducerConsumer;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class BufferManagerActor extends AbstractActor {
    // Messages
    public static class Setup {
        public final ActorRef producer;
        public final ActorRef consumer;

        public Setup(ActorRef producer, ActorRef consumer) {
            this.producer = producer;
            this.consumer = consumer;
        }
    }

    public static class Initial {}
    public static class GiveMeNextProduce {}
    public static class GiveMeNextConsume {}
    public static class AckProduce {}
    public static class AckConsume {}

    // State (1:1 with Rebeca)
    private boolean empty;
    private boolean full;
    private boolean producerWaiting;
    private boolean consumerWaiting;
    private int bufferLength;
    private int nextProduce;
    private int nextConsume;
    private ActorRef producer;
    private ActorRef consumer;

    public static Props props() {
        return Props.create(BufferManagerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Initial.class, this::handleInitial)
                .match(GiveMeNextProduce.class, this::handleGiveMeNextProduce)
                .match(GiveMeNextConsume.class, this::handleGiveMeNextConsume)
                .match(AckProduce.class, this::handleAckProduce)
                .match(AckConsume.class, this::handleAckConsume)
                .build();
    }

    private void handleSetup(Setup setup) {
        this.producer = setup.producer;
        this.consumer = setup.consumer;
    }

    private void handleInitial(Initial msg) {
        bufferLength = 2;
        empty = true;
        full = false;
        producerWaiting = false;
        consumerWaiting = false;
        nextProduce = 0;
        nextConsume = 0;
    }

    private void handleGiveMeNextProduce(GiveMeNextProduce msg) {
        if (!full) {
            producer.tell(new ProducerActor.Produce(nextProduce), getSelf());
        }
    }

    private void handleGiveMeNextConsume(GiveMeNextConsume msg) {
        if (!empty) {
            consumer.tell(new ConsumerActor.Consume(nextConsume), getSelf());
        } else {
            consumerWaiting = true;
        }
    }

    private void handleAckProduce(AckProduce msg) {
        nextProduce = (nextProduce + 1) % bufferLength;
        if (nextProduce == nextConsume) {
            full = true;
        }
        empty = false;
        if (consumerWaiting) {
            consumer.tell(new ConsumerActor.Consume(nextConsume), getSelf());
            consumerWaiting = false;
        }
    }

    private void handleAckConsume(AckConsume msg) {
        nextConsume = (nextConsume + 1) % bufferLength;
        if (nextConsume == nextProduce) {
            empty = true;
        }
        full = false;
        if (producerWaiting) {
            producer.tell(new ProducerActor.Produce(nextProduce), getSelf());
            producerWaiting = false;
        }
    }
}

class ProducerActor extends AbstractActor {
    // Messages
    public static class Setup {
        public final ActorRef buffer;

        public Setup(ActorRef buffer) {
            this.buffer = buffer;
        }
    }

    public static class Initial {}
    public static class Produce {
        public final int bufNum;
        public Produce(int bufNum) {
            this.bufNum = bufNum;
        }
    }
    public static class BeginProduce {}

    // State (matches empty Rebeca statevars)
    private ActorRef buffer;

    public static Props props() {
        return Props.create(ProducerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Initial.class, this::handleInitial)
                .match(Produce.class, this::handleProduce)
                .match(BeginProduce.class, this::handleBeginProduce)
                .build();
    }

    private void handleSetup(Setup setup) {
        this.buffer = setup.buffer;
    }

    private void handleInitial(Initial msg) {
        getSelf().tell(new BeginProduce(), getSelf());
    }

    private void handleProduce(Produce msg) {
        buffer.tell(new BufferManagerActor.AckProduce(), getSelf());
        getSelf().tell(new BeginProduce(), getSelf());
    }

    private void handleBeginProduce(BeginProduce msg) {
        buffer.tell(new BufferManagerActor.GiveMeNextProduce(), getSelf());
    }
}

class ConsumerActor extends AbstractActor {
    // Messages
    public static class Setup {
        public final ActorRef buffer;

        public Setup(ActorRef buffer) {
            this.buffer = buffer;
        }
    }

    public static class Initial {}
    public static class Consume {
        public final int bufNum;
        public Consume(int bufNum) {
            this.bufNum = bufNum;
        }
    }
    public static class BeginConsume {}

    // State (matches empty Rebeca statevars)
    private ActorRef buffer;

    public static Props props() {
        return Props.create(ConsumerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Setup.class, this::handleSetup)
                .match(Initial.class, this::handleInitial)
                .match(Consume.class, this::handleConsume)
                .match(BeginConsume.class, this::handleBeginConsume)
                .build();
    }

    private void handleSetup(Setup setup) {
        this.buffer = setup.buffer;
    }

    private void handleInitial(Initial msg) {
        getSelf().tell(new BeginConsume(), getSelf());
    }

    private void handleConsume(Consume msg) {
        buffer.tell(new BufferManagerActor.AckConsume(), getSelf());
        getSelf().tell(new BeginConsume(), getSelf());
    }

    private void handleBeginConsume(BeginConsume msg) {
        buffer.tell(new BufferManagerActor.GiveMeNextConsume(), getSelf());
    }
}

