import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;



public class Sandwiches {
    public static void main(String[] args) throws Exception {
//        ToastQueue dryQueue = new ToastQueue(),
//                butteredQueue = new ToastQueue(),
//                finishedQueue = new ToastQueue();
//        ExecutorService exec = Executors.newCachedThreadPool();
//        exec.execute(new Toaster(dryQueue));
//        exec.execute(new Butterer(dryQueue, butteredQueue));
//        exec.execute(new Jammer(butteredQueue, finishedQueue));
//        exec.execute(new Eater(finishedQueue));
//        TimeUnit.SECONDS.sleep(5);
//        exec.shutdownNow();

        ToastQueue dryQueue = new ToastQueue(),
                peanutButteredQueue = new ToastQueue(),
                jelliedQueue = new ToastQueue();
        SandwichQueue finishedQueue = new SandwichQueue();
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Toaster(dryQueue));
        exec.execute(new PeanutButterer(dryQueue, peanutButteredQueue));
        exec.execute(new Jellier(dryQueue, jelliedQueue));
        exec.execute(new SandwichMaker(peanutButteredQueue, jelliedQueue, finishedQueue));
        exec.execute(new AnotherEater(finishedQueue));
        TimeUnit.SECONDS.sleep(5);
        exec.shutdownNow();
    }
}


class Toast {
    public enum Status { DRY, BUTTERED, JAMMED, PEANUT_BUTTERED, JELLIED }
    private Status status = Status.DRY;
    private final int id;
    public Toast(int idn) { id = idn; }
    public void butter() { status = Status.BUTTERED; }
    public void jam() { status = Status.JAMMED; }
    public void peanutButter() { status = Status.PEANUT_BUTTERED; }
    public void jelly() { status = Status.JELLIED; }
    public Status getStatus() { return status; }
    public int getId() { return id; }
    public String toString() {
        return "Toast " + id + ": " + status;
    }
}

class ToastQueue extends LinkedBlockingQueue<Toast> {}

class Sandwich {
    private Toast toast1;
    private Toast toast2;
    public Sandwich(Toast toast1, Toast toast2) {
        this.toast1 = toast1;
        this.toast2 = toast2;
    }
    @Override
    public String toString() {
        return "Sandwich(" + toast1 + " AND " + toast2 + ")";
    }

    public Toast getToast1() {
        return toast1;
    }

    public Toast getToast2() {
        return toast2;
    }
}

class SandwichQueue extends LinkedBlockingQueue<Sandwich> {}

class Toaster implements Runnable {
    private ToastQueue toastQueue;
    private int count = 0;
    private Random rand = new Random(47);
    public Toaster(ToastQueue tq) { toastQueue = tq; }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(
                        100 + rand.nextInt(500));
                // Make toast
                Toast t = new Toast(count++);
                System.out.println(t);
                // Insert into queue
                toastQueue.put(t);
            }
        } catch(InterruptedException e) {
            System.out.println("Toaster interrupted");
        }
        System.out.println("Toaster off");
    }
}

// Apply butter to toast:
class Butterer implements Runnable {
    private ToastQueue dryQueue, butteredQueue;
    public Butterer(ToastQueue dry, ToastQueue buttered) {
        dryQueue = dry;
        butteredQueue = buttered;
    }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = dryQueue.take();
                t.butter();
                System.out.println(t);
                butteredQueue.put(t);
            }
        } catch(InterruptedException e) {
            System.out.println("Butterer interrupted");
        }
        System.out.println("Butterer off");
    }
}

// Apply jam to buttered toast:
class Jammer implements Runnable {
    private ToastQueue butteredQueue, finishedQueue;
    public Jammer(ToastQueue buttered, ToastQueue finished) {
        butteredQueue = buttered;
        finishedQueue = finished;
    }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = butteredQueue.take();
                t.jam();
                System.out.println(t);
                finishedQueue.put(t);
            }
        } catch(InterruptedException e) {
            System.out.println("Jammer interrupted");
        }
        System.out.println("Jammer off");
    }
}

// Consume the toast:
class Eater implements Runnable {
    private ToastQueue finishedQueue;
    private int counter = 0;
    public Eater(ToastQueue finished) {
        finishedQueue = finished;
    }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Toast t = finishedQueue.take();
                // Verify that the toast is coming in order,
                // and that all pieces are getting jammed:
                if(t.getId() != counter++ ||
                        t.getStatus() != Toast.Status.JAMMED) {
                    System.out.println(">>>> Error: " + t);
                    System.exit(1);
                } else
                    System.out.println("Chomp! " + t);
            }
        } catch(InterruptedException e) {
            System.out.println("Eater interrupted");
        }
        System.out.println("Eater off");
    }
}

// Apply peanut butter to toast
class PeanutButterer implements Runnable {
    private ToastQueue dryQueue, peanutButteredQueue;

    public PeanutButterer(ToastQueue dryQueue, ToastQueue peanutButteredQueue) {
        this.dryQueue = dryQueue;
        this.peanutButteredQueue = peanutButteredQueue;
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                Toast t = dryQueue.take();
                t.peanutButter();
                System.out.println(t);
                peanutButteredQueue.put(t);
            }
        } catch (InterruptedException e) {
            System.out.println("PeanutButterer interrupted");
        }
        System.out.println("PeanutButterer off");
    }
}

// Apply jelly to toast
class Jellier implements Runnable {
    private ToastQueue dryQueue, jelliedQueue;

    public Jellier(ToastQueue dryQueue, ToastQueue jelliedQueue) {
        this.dryQueue = dryQueue;
        this.jelliedQueue = jelliedQueue;
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                Toast t = dryQueue.take();
                t.jelly();
                System.out.println(t);
                jelliedQueue.put(t);
            }
        } catch (InterruptedException e) {
            System.out.println("Jellier interrupted");
        }
        System.out.println("Jellier off");
    }
}

// Merging toast with peanut butter and the one with jelly
class SandwichMaker implements Runnable {
    private ToastQueue peanutButteredQueue, jelliedQueue;
    private SandwichQueue finishedQueue;

    public SandwichMaker(ToastQueue peanutButteredQueue, ToastQueue jelliedQueue, SandwichQueue finishedQueue) {
        this.peanutButteredQueue = peanutButteredQueue;
        this.jelliedQueue = jelliedQueue;
        this.finishedQueue = finishedQueue;
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                Toast t1 = peanutButteredQueue.take();
                Toast t2 = jelliedQueue.take();
                Sandwich s = new Sandwich(t1, t2);
                System.out.println(s);
                finishedQueue.put(s);
            }
        } catch (InterruptedException e) {
            System.out.println("SandwichMaker interrupted");
        }
        System.out.println("SandwichMaker off");
    }
}

class AnotherEater implements Runnable {
    private SandwichQueue finishedQueue;
    private int counter = 0;
    public AnotherEater(SandwichQueue finished) {
        finishedQueue = finished;
    }
    public void run() {
        try {
            while(!Thread.interrupted()) {
                // Blocks until next piece of toast is available:
                Sandwich s = finishedQueue.take();
                // Verify that the toast is coming in order,
                // and that all pieces are getting jammed:
                Toast t1 = s.getToast1(),
                        t2 = s.getToast2();
                if(t1.getId() != counter++ || t2.getId() != counter++ ||
                        t1.getStatus() != Toast.Status.PEANUT_BUTTERED ||
                        t2.getStatus() != Toast.Status.JELLIED) {
                    System.out.println(">>>> Error: " + t1 + " " + t2);
                    System.exit(1);
                } else
                    System.out.println("Chomp! " + s);
            }
        } catch(InterruptedException e) {
            System.out.println("AnotherEater interrupted");
        }
        System.out.println("AnotherEater off");
    }
}