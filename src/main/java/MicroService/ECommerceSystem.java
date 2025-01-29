package MicroService;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import java.util.Random;

public class ECommerceSystem {

    // Message classes
    public static class Browse {}
    public static class Delivered {
        public final boolean success;
        public Delivered(boolean success) { this.success = success; }
    }
    public static class PlaceOrder {
        public final ActorRef customer;
        public final byte product;
        public final byte qty;
        public PlaceOrder(ActorRef customer, byte product, byte qty) {
            this.customer = customer;
            this.product = product;
            this.qty = qty;
        }
    }
    public static class CheckAvailability {
        public final ActorRef customer;
        public final byte product;
        public final byte qty;
        public CheckAvailability(ActorRef customer, byte product, byte qty) {
            this.customer = customer;
            this.product = product;
            this.qty = qty;
        }
    }
    public static class AvailabilityStatus {
        public final ActorRef customer;
        public final boolean available;
        public final byte product;
        public final byte qty;
        public AvailabilityStatus(ActorRef customer, boolean available, byte product, byte qty) {
            this.customer = customer;
            this.available = available;
            this.product = product;
            this.qty = qty;
        }
    }
    public static class ConfirmBoughtItem {
        public final ActorRef customer;
        public final byte product;
        public final byte qty;
        public ConfirmBoughtItem(ActorRef customer, byte product, byte qty) {
            this.customer = customer;
            this.product = product;
            this.qty = qty;
        }
    }
    public static class Confirm {
        public final ActorRef customer;
        public final byte product;
        public final byte qty;
        public Confirm(ActorRef customer, byte product, byte qty) {
            this.customer = customer;
            this.product = product;
            this.qty = qty;
        }
    }
    public static class ArrangeDelivery {
        public final ActorRef customer;
        public final byte product;
        public final byte qty;
        public ArrangeDelivery(ActorRef customer, byte product, byte qty) {
            this.customer = customer;
            this.product = product;
            this.qty = qty;
        }
    }
    public static class ShippingIsScheduled {
        public final ActorRef customer;
        public ShippingIsScheduled(ActorRef customer) { this.customer = customer; }
    }

    // Customer Actor
    public static class Customer extends AbstractActor {
        private final int id;
        private boolean buying;
        private final ActorRef orderService;
        private final Random random = new Random();

        public Customer(int id, ActorRef orderService) {
            this.id = id;
            this.orderService = orderService;
        }

        @Override
        public void preStart() {
            getSelf().tell(new Browse(), getSelf());
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Browse.class, this::handleBrowse)
                    .match(Delivered.class, this::handleDelivered)
                    .build();
        }

        private void handleBrowse(Browse msg) {
            buying = random.nextBoolean();
            if (buying) {
                byte product = (byte) random.nextInt(2);
                byte qty = (byte) (1 + random.nextInt(2));
                orderService.tell(new PlaceOrder(getSelf(), product, qty), getSelf());
            } else {
                getSelf().tell(new Browse(), getSelf());
            }
        }

        private void handleDelivered(Delivered msg) {
            buying = false;
            getSelf().tell(new Browse(), getSelf());
        }
    }

    // OrderService Actor
    public static class OrderService extends AbstractActor {
        private final ActorRef inventoryService;
        private final ActorRef shippingService;

        public OrderService(ActorRef inventoryService, ActorRef shippingService) {
            this.inventoryService = inventoryService;
            this.shippingService = shippingService;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(PlaceOrder.class, this::handlePlaceOrder)
                    .match(AvailabilityStatus.class, this::handleAvailabilityStatus)
                    .match(Confirm.class, this::handleConfirm)
                    .match(ShippingIsScheduled.class, this::handleShippingIsScheduled)
                    .build();
        }

        private void handlePlaceOrder(PlaceOrder msg) {
            inventoryService.tell(new CheckAvailability(msg.customer, msg.product, msg.qty), getSelf());
        }

        private void handleAvailabilityStatus(AvailabilityStatus msg) {
            if (msg.available) {
                inventoryService.tell(new ConfirmBoughtItem(msg.customer, msg.product, msg.qty), getSelf());
            } else {
                msg.customer.tell(new Delivered(false), getSelf());
            }
        }

        private void handleConfirm(Confirm msg) {
            shippingService.tell(new ArrangeDelivery(msg.customer, msg.product, msg.qty), getSelf());
        }

        private void handleShippingIsScheduled(ShippingIsScheduled msg) {
            msg.customer.tell(new Delivered(true), getSelf());
        }
    }

    // InventoryService Actor
    public static class InventoryService extends AbstractActor {
        private final int[] stockLevel = new int[2];

        @Override
        public void preStart() {
            stockLevel[0] = 5;
            stockLevel[1] = 5;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(CheckAvailability.class, this::handleCheckAvailability)
                    .match(ConfirmBoughtItem.class, this::handleConfirmBoughtItem)
                    .build();
        }

        private void handleCheckAvailability(CheckAvailability msg) {
            boolean available = stockLevel[msg.product] >= msg.qty;
            getSender().tell(new AvailabilityStatus(msg.customer, available, msg.product, msg.qty), getSelf());
        }

        private void handleConfirmBoughtItem(ConfirmBoughtItem msg) {
            stockLevel[msg.product] -= msg.qty;
            getSender().tell(new Confirm(msg.customer, msg.product, msg.qty), getSelf());
        }
    }

    // ShippingService Actor
    public static class ShippingService extends AbstractActor {
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ArrangeDelivery.class, this::handleArrangeDelivery)
                    .build();
        }

        private void handleArrangeDelivery(ArrangeDelivery msg) {
            getSender().tell(new ShippingIsScheduled(msg.customer), getSelf());
        }
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ECommerceSystem");

        // Create services
        ActorRef inventoryService = system.actorOf(Props.create(InventoryService.class), "inventory");
        ActorRef shippingService = system.actorOf(Props.create(ShippingService.class), "shipping");
        ActorRef orderService = system.actorOf(Props.create(OrderService.class, inventoryService, shippingService), "order");

        // Create customers
        system.actorOf(Props.create(Customer.class, 1, orderService), "customer1");
        system.actorOf(Props.create(Customer.class, 2, orderService), "customer2");
        system.actorOf(Props.create(Customer.class, 3, orderService), "customer3");
    }
}