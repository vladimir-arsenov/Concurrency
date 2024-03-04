import FoodEnums.Course;
import FoodEnums.Food;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;



public class RestaurantWithQueues {
    public static void main(String[] args) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        Restaurant restaurant = new Restaurant(3, 5, exec);
        exec.execute(restaurant);
        TimeUnit.SECONDS.sleep(3);
        exec.shutdownNow();
    }
}

class Order {
    private static int counter = 0;
    private final int id = counter++;
    private final Customer customer;
    private final Waiter waiter;
    private final Food food;

    Order(Customer customer, Waiter waiter, Food food) {
        this.customer = customer;
        this.waiter = waiter;
        this.food = food;
    }

    public Customer customer() {
        return customer;
    }

    public Waiter waiter() {
        return waiter;
    }

    public Food food() {
        return food;
    }

    @Override
    public String toString() {
        return "Order: " + id +
                " item: " + food +
                " for: " + customer +
                " served by: " + waiter;
    }
}

record Plate(Order order, Food food) {
    @Override
    public String toString() {
        return food.toString();
    }
}

class Customer implements Runnable {
    private static int counter = 0;
    private final int id = counter++;
    private final Waiter waiter;
    private SynchronousQueue<Plate> placeSetting = new SynchronousQueue<>();

    public Customer(Waiter waiter) {
        this.waiter = waiter;
    }

    @Override
    public void run() {
        for (Course course : Course.values()) {
            try {
                Food food = course.randomSelection();
                waiter.placeOrder(this, food);
                System.out.println(this + " eating " + placeSetting.take());
            } catch (InterruptedException e) {
                System.out.println(this + " waiting for " + course + " interrupted");
                break;
            }
        }
        System.out.println(this + " finished meal, leaving");
    }

    public void deliver(Plate plate) {
        try {
            placeSetting.put(plate);
        } catch (InterruptedException e) {
            System.out.println(waiter + " was killed while delivering");
        }
    }

    @Override
    public String toString() {
        return "Customer " + id;
    }
}

class Chef implements Runnable {
    private static int counter = 0;
    private final int id = counter++;
    private final Restaurant restaurant;
    private final Random rand = new Random(47);

    public Chef(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Order order = restaurant.orders.take();
                Food food = order.food();
                TimeUnit.MILLISECONDS.sleep(rand.nextInt(500));
                order.waiter().filledOrders.put(new Plate(order, food));
            }
        } catch (InterruptedException e) {
            System.out.println(this + " was interrupted");
        }
        System.out.println(this + " off duty");
    }

    @Override
    public String toString() {
        return "Chef " + id;
    }
}

class Waiter implements Runnable {
    private static int counter = 0;
    private final int id = counter++;
    private final Restaurant restaurant;
    BlockingDeque<Plate> filledOrders = new LinkedBlockingDeque<>();

    public Waiter(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public void placeOrder(Customer customer, Food food) {
        try {
            restaurant.orders.put(new Order(customer, this, food));
        } catch (InterruptedException e) {
            System.out.println(this + " placeOrder was interrupted");
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Plate plate = filledOrders.take();
                System.out.println(this + " received " + plate + " delivering to " + plate.order().customer());
                plate.order().customer().deliver(plate);
            }
        } catch (InterruptedException e) {
            System.out.println(this + " was interrupted");
        }
        System.out.println(this + " off duty");
    }

    @Override
    public String toString() {
        return "Waiter " + id;
    }
}

class Restaurant implements Runnable {
    BlockingDeque<Order> orders = new LinkedBlockingDeque<>();
    private List<Waiter> waiters = new ArrayList<>();
    private List<Chef> chefs = new ArrayList<>();
    private ExecutorService exec;
    private Random rand = new Random(47);

    public Restaurant(int nChefs, int nWaiters, ExecutorService exec) {
        this.exec = exec;

        for (int i = 0; i < nChefs; i++) {
            Chef chef = new Chef(this);
            exec.execute(chef);
            chefs.add(chef);
        }

        for (int i = 0; i < nWaiters; i++) {
            Waiter waiter = new Waiter(this);
            exec.execute(waiter);
            waiters.add(waiter);
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Waiter waiter = waiters.get(rand.nextInt(waiters.size()));
                Customer customer = new Customer(waiter);
                exec.execute(customer);
                TimeUnit.MILLISECONDS.sleep(100);
            }
        } catch (InterruptedException e) {
            System.out.println("Restaurant was interrupted");
        }
        System.out.println("Restaurant is closing");
    }
}

