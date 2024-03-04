import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnotherRestaurant {

    public static void main(String[] args) {
        new Restaurant1();
    }
}


class Meal {
    private final int orderNum;
    public Meal(int orderNum) { this.orderNum = orderNum; }
    public String toString() { return "Meal " + orderNum; }
}

class BusBoy implements Runnable {
    private Restaurant1 restaurant;
    public BusBoy(Restaurant1 r) { restaurant = r; }


    public void run() {
        try {
            while(!Thread.interrupted()) {
                synchronized (this) {
                    while(restaurant.clean)
                        wait();
                }

                System.out.println("Cleaned up!");
                synchronized(restaurant.chef) {
                    restaurant.clean = true;
                    restaurant.chef.notifyAll(); // Ready for another
                }
            }
        } catch (InterruptedException e)  {
            System.out.println("BusBoy was interrupted");
        }
    }
}

class Waiter1 implements Runnable {
    private Restaurant1 restaurant;
    public Waiter1(Restaurant1 r) { restaurant = r; }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                synchronized(this) {
                    while(restaurant.meal == null)
                        wait(); // ... for the chef to produce a meal
                }
                System.out.println("Got " + restaurant.meal + "! ");
                synchronized(restaurant.busBoy) {
                    restaurant.meal = null;
                    restaurant.busBoy.notifyAll();
                }
            }
        } catch(InterruptedException e) {
            System.out.println("WaitPerson interrupted");
        }
    }
}

class Chef1 implements Runnable {
    private Restaurant1 restaurant;
    private int count = 0;
    public Chef1(Restaurant1 r) { restaurant = r; }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                synchronized(this) {
                    while(restaurant.meal != null && !restaurant.clean)
                        wait(); // ... for the meal to be taken
                }
                if(++count == 10) {
                    System.out.println("Out of food, closing");
                    restaurant.exec.shutdownNow();
                }
                System.out.println("Order up! ");
                synchronized(restaurant.waiter1) {
                    restaurant.clean = false;
                    restaurant.meal = new Meal(count);
                    restaurant.waiter1.notifyAll();
                }
                TimeUnit.MILLISECONDS.sleep(100);
            }
        } catch(InterruptedException e) {
            System.out.println("Chef interrupted");
        }
    }
}

class Restaurant1 {
    Meal meal;
    boolean clean = true;
    ExecutorService exec = Executors.newCachedThreadPool();
    Waiter1 waiter1 = new Waiter1(this);
    Chef1 chef = new Chef1(this);
    BusBoy busBoy = new BusBoy(this);
    public Restaurant1() {
        exec.execute(chef);
        exec.execute(waiter1);
        exec.execute(busBoy);
    }
}

