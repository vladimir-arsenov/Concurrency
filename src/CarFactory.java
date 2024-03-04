import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;


public class CarFactory {
    public static void main(String[] args) throws InterruptedException {
        CarQueue chassisQueue = new CarQueue(),
                finishingQueue = new CarQueue();
        ExecutorService exec = Executors.newCachedThreadPool();
        RobotPool pool = new RobotPool();
        exec.execute(new EngineRobot(pool));
        exec.execute(new DriveTrainRobot(pool));
        exec.execute(new WheelsRobot(pool));
        exec.execute(new Assembler(chassisQueue, finishingQueue, pool));
        exec.execute(new Reporter(finishingQueue));
        exec.execute(new ChassisBuilder(chassisQueue));
        TimeUnit.SECONDS.sleep(7);
        exec.shutdownNow();
    }
}


class Car {
    private final int id;
    private boolean engine = false,
            driveTrain = false,
            wheels = false;

    Car(int id) {this.id = id;}

    Car() { id = -1; }

    public synchronized int getId() { return id; }
    public synchronized void addEngine() { engine = true; }
    public synchronized void addDriveTrain() { driveTrain = true; }
    public synchronized void addWheels() { wheels = true; }

    @Override
    public synchronized String toString() {
        return "Car{" +
                "id=" + id +
                ", engine=" + engine +
                ", driveTrain=" + driveTrain +
                ", wheels=" + wheels +
                '}';
    }
}

class CarQueue extends LinkedBlockingQueue<Car> {}

class ChassisBuilder implements Runnable {
    private CarQueue carQueue;
    private int counter = 0;

    public ChassisBuilder(CarQueue carQueue) {
        this.carQueue = carQueue;
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(500);
                Car car = new Car(counter++);
                System.out.println("Chassis Builder created " + car);
                carQueue.put(car);
            }
        } catch (InterruptedException e) {
            System.out.println("Chassis Builder was interrupted");
        }
        System.out.println("Chassis Builder off");
    }
}

class Assembler implements Runnable {

    private CarQueue chassisQueue, finishingQueue;
    private RobotPool robotPool;
    private Car car;
    private CyclicBarrier barrier = new CyclicBarrier(4);

    public Assembler(CarQueue chassisQueue, CarQueue finishingQueue, RobotPool robotPool) {
        this.chassisQueue = chassisQueue;
        this.finishingQueue = finishingQueue;
        this.robotPool = robotPool;
    }

    public CyclicBarrier barrier() { return barrier; }
    public Car car() { return car; }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                car = chassisQueue.take();
                robotPool.hire(EngineRobot.class, this);
                robotPool.hire(DriveTrainRobot.class, this);
                robotPool.hire(WheelsRobot.class, this);
                barrier.await();
                finishingQueue.put(car);
            }
        } catch (InterruptedException e) {
            System.out.println("Assembler was interrupted");
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println("Assembler off");
    }
}

class Reporter implements Runnable {
    private CarQueue carQueue;

    public Reporter(CarQueue carQueue) {
        this.carQueue = carQueue;
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                System.out.println(carQueue.take());
            }
        }catch (InterruptedException e ) {
            System.out.println("Reporter was interrupted");
        }
        System.out.println("Reporter off");
    }
}

abstract class Robot implements Runnable {
    protected Assembler assembler;
    private RobotPool pool;
    private boolean engage = false;

    public Robot(RobotPool pool) {
        this.pool = pool;

    }

    public Robot assignAssembler(Assembler assembler) {
        this.assembler = assembler;
        return this;
    }

    public synchronized void engage() {
        engage = true;
        notifyAll();
    }

    abstract protected void performService();

    @Override
    public void run() {
        try{
            powerDown();
            while(!Thread.interrupted()) {
                performService();
                assembler.barrier().await();
                powerDown();
            }
        } catch (InterruptedException e) {
            System.out.println(this + " was interrupted");
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        System.out.println(this + " off");
    }


    private synchronized void powerDown() throws InterruptedException {
        engage = false;
        assembler = null;

        pool.release(this);
        while (engage == false)
            wait();
    }

    @Override
    public String toString() {return getClass().getName();}
}


class EngineRobot extends Robot {
    public EngineRobot(RobotPool pool) {
        super(pool);
    }

    @Override
    protected void performService() {
        System.out.println(this + " installing Engine");
        assembler.car().addEngine();
    }
}


class DriveTrainRobot extends Robot {
    public DriveTrainRobot(RobotPool pool) {
        super(pool);
    }

    @Override
    protected void performService() {
        System.out.println(this + " installing DriveTrain");
        assembler.car().addDriveTrain();
    }
}


class WheelsRobot extends Robot {
    public WheelsRobot(RobotPool pool) {
        super(pool);
    }

    @Override
    protected void performService() {
        System.out.println(this + " installing Wheels");
        assembler.car().addWheels();
    }
}

class RobotPool{
    private Set<Robot> pool = new HashSet<>();

    public synchronized void add(Robot r) {
        pool.add(r);
        notifyAll();
    }

    public synchronized void hire(Class<? extends Robot> robotType, Assembler assembler) throws InterruptedException {
        for(Robot r : pool)
            if (r.getClass().equals(robotType)) {
                pool.remove(r);
                r.assignAssembler(assembler);
                r.engage();
                return;
            }
        wait();
        hire(robotType, assembler);
    }
    public synchronized void release(Robot robot) { pool.add(robot); }
}