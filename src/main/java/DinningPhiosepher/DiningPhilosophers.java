package DinningPhiosepher;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

// Messages for Philosopher
class PhilosopherMessages {
    static class Initial {}
    static class Arrive {}
    static class Permit {}
    static class Eat {}
    static class Leave {}
}

// Messages for Fork
class ForkMessages {
    static class Initial {}
    static class Request {}
    static class Release {}
}

// Philosopher Actor
class Philosopher extends AbstractActor {
    private final ActorRef forkL;
    private final ActorRef forkR;

    boolean eating;
    private boolean fL;
    private boolean fR;

    public Philosopher(ActorRef forkL, ActorRef forkR) {
        this.forkL = forkL;
        this.forkR = forkR;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PhilosopherMessages.Initial.class, msg -> {
                    fL = false;
                    fR = false;
                    this.eating = false;
                    System.out.println(getSelf().path().name() + " initialized.");
                    self().tell(new PhilosopherMessages.Arrive(), self());
                })
                .match(PhilosopherMessages.Arrive.class, msg -> {
                    System.out.println(getSelf().path().name() + " is arriving and requesting left fork.");
                    forkL.tell(new ForkMessages.Request(), self());
                })
                .match(PhilosopherMessages.Permit.class, msg -> {
                    if (sender().equals(forkL) && !fL) {
                        fL = true;
                        System.out.println(getSelf().path().name() + " acquired left fork.");
                        forkR.tell(new ForkMessages.Request(), self());
                    } else if (sender().equals(forkR) && fL && !fR) {
                        fR = true;
                        System.out.println(getSelf().path().name() + " acquired right fork. Ready to eat.");
                        self().tell(new PhilosopherMessages.Eat(), self());
                    }
                })
                .match(PhilosopherMessages.Eat.class, msg -> {
                    eating = true;
                    System.out.println(getSelf().path().name() + " is eating.");
                    self().tell(new PhilosopherMessages.Leave(), self());
                })
                .match(PhilosopherMessages.Leave.class, msg -> {
                    System.out.println(getSelf().path().name() + " finished eating and is releasing forks.");
                    fL = false;
                    fR = false;
                    eating = false;
                    forkL.tell(new ForkMessages.Release(), self());
                    forkR.tell(new ForkMessages.Release(), self());
                    self().tell(new PhilosopherMessages.Arrive(), self());
                })
                .build();
    }

    public static Props props(ActorRef forkL, ActorRef forkR) {
        return Props.create(Philosopher.class, () -> new Philosopher(forkL, forkR));
    }
}

// Fork Actor
class Fork extends AbstractActor {
    private final ActorRef philL;
    private final ActorRef philR;

    private boolean lAssign;
    private boolean rAssign;
    private boolean leftReq;
    private boolean rightReq;

    public Fork(ActorRef philL, ActorRef philR) {
        this.philL = philL;
        this.philR = philR;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ForkMessages.Initial.class, msg -> {
                    lAssign = false;
                    rAssign = false;
                    leftReq = false;
                    rightReq = false;
                    System.out.println(getSelf().path().name() + " initialized.");
                })
                .match(ForkMessages.Request.class, msg -> {
                    System.out.println(getSelf().path().name() + " received request from " + sender().path().name());
                    if (sender().equals(philL)) {
                        if (!leftReq) {
                            leftReq = true;
                            if (!rAssign) {
                                lAssign = true;
                                System.out.println(getSelf().path().name() + " assigned to " + philL.path().name());
                                philL.tell(new PhilosopherMessages.Permit(), self());
                            }
                        }
                    } else if (sender().equals(philR)) {
                        if (!rightReq) {
                            rightReq = true;
                            if (!lAssign) {
                                rAssign = true;
                                System.out.println(getSelf().path().name() + " assigned to " + philR.path().name());
                                philR.tell(new PhilosopherMessages.Permit(), self());
                            }
                        }
                    }
                })
                .match(ForkMessages.Release.class, msg -> {
                    System.out.println(getSelf().path().name() + " received release from " + sender().path().name());
                    if (sender().equals(philL) && lAssign) {
                        leftReq = false;
                        lAssign = false;
                        if (rightReq) {
                            rAssign = true;
                            System.out.println(getSelf().path().name() + " assigned to " + philR.path().name());
                            philR.tell(new PhilosopherMessages.Permit(), self());
                        }
                    } else if (sender().equals(philR) && rAssign) {
                        rightReq = false;
                        rAssign = false;
                        if (leftReq) {
                            lAssign = true;
                            System.out.println(getSelf().path().name() + " assigned to " + philL.path().name());
                            philL.tell(new PhilosopherMessages.Permit(), self());
                        }
                    }
                })
                .build();
    }

    public static Props props(ActorRef philL, ActorRef philR) {
        return Props.create(Fork.class, () -> new Fork(philL, philR));
    }
}

// Main Application
public class DiningPhilosophers {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("DiningPhilosophers");

        // Create forks
        ActorRef fork0 = system.actorOf(Fork.props(null, null), "Fork0");
        ActorRef fork1 = system.actorOf(Fork.props(null, null), "Fork1");
        ActorRef fork2 = system.actorOf(Fork.props(null, null), "Fork2");

        // Create philosophers
        ActorRef phil0 = system.actorOf(Philosopher.props(fork0, fork2), "Philosopher0");
        ActorRef phil1 = system.actorOf(Philosopher.props(fork0, fork1), "Philosopher1");
        ActorRef phil2 = system.actorOf(Philosopher.props(fork1, fork2), "Philosopher2");

        // Initialize actors
        fork0.tell(new ForkMessages.Initial(), ActorRef.noSender());
        fork1.tell(new ForkMessages.Initial(), ActorRef.noSender());
        fork2.tell(new ForkMessages.Initial(), ActorRef.noSender());

        phil0.tell(new PhilosopherMessages.Initial(), ActorRef.noSender());
        phil1.tell(new PhilosopherMessages.Initial(), ActorRef.noSender());
        phil2.tell(new PhilosopherMessages.Initial(), ActorRef.noSender());
    }
}
