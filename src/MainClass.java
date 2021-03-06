import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;


public class MainClass {
    public static final int CARS_COUNT = 20;
    public static final Semaphore SEMAPHORE_TUNNEL = new Semaphore(CARS_COUNT / 2,true); //количество машин проезжающих по тунелю
    public static CyclicBarrier barrier=new CyclicBarrier(CARS_COUNT+1); // количество действий до вывода сообщений
    public static volatile int WIN_COUNT=3; //маскимальное количество мест победителей гонки
    public static volatile int WIN=0; //место победителя

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Подготовка!!!");
        Race race = new Race(new Road(60), new Tunnel(), new Road(40));
        Car[] cars = new Car[CARS_COUNT];
        Object monitor=new Object();
        for (int i = 0; i < cars.length; i++) {
            cars[i] = new Car(monitor,barrier,race, 20 + (int) (Math.random() * 10));
        }
        for (int i = 0; i < cars.length; i++) {
            new Thread(cars[i]).start();
        }
        barrier.await(); //ждем пока все учасники не будут готовы
        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка началась!!!");
        barrier.await();
        barrier.await(); //ждем пока все учасники не закончат гонку
        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка закончилась!!!");
    }
}

class Car implements Runnable {
    private static int CARS_COUNT;
    private CyclicBarrier barrier;
    private Object monitor;
    private Race race;
    private int speed;
    private String name;

    static {
        CARS_COUNT = 0;
    }


    public String getName() {
        return name;
    }

    public int getSpeed() {
        return speed;
    }

    public Car(Object monitor, CyclicBarrier barrier, Race race, int speed) {
        this.race = race;
        this.speed = speed;
        CARS_COUNT++;
        this.name = "Участник #" + CARS_COUNT;
        this.barrier = barrier;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        try {
            System.out.println(this.name + " готовится");
            Thread.sleep(500 + (int) (Math.random() * 800));
            System.out.println(this.name + " готов");
            barrier.await(); //ждем пока все будут готовы
            barrier.await();// ждем пока не напишим гонка началась.
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < race.getStages().size(); i++) {
            race.getStages().get(i).go(this);
        }

        //проверка победителя
        synchronized (monitor) {
            if (MainClass.WIN_COUNT > 0) {
                System.out.println(this.name + " WIN " + " занял " + ++MainClass.WIN + " место.");
                MainClass.WIN_COUNT--; //уменьшпем места победителей
            }
        }
        try {
            barrier.await(); //ждем пока гонка закончится
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

}





class Race {
    private ArrayList<Stage> stages;

    public ArrayList<Stage> getStages() {
        return stages;
    }

    public Race(Stage... stages) {
        this.stages = new ArrayList<>(Arrays.asList(stages));
    }
}

class Road extends Stage {
    public Road(int length) {
        this.length = length;
        this.description = "Дорога " + length + " метров";
    }

    @Override
    public void go(Car c) {
        try {
            System.out.println(c.getName() + " начал этап: " + description);
            Thread.sleep(length / c.getSpeed() * 1000);
            System.out.println(c.getName() + " закончил этап: " + description);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


abstract class Stage {
    protected int length;
    protected String description;

    public String getDescription() {
        return description;
    }

    public abstract void go(Car c);
}


class Tunnel extends Stage {
    public Tunnel() {
        this.length = 80;
        this.description = "Тоннель " + length + " метров";
    }

    @Override
    public void go(Car c) {
        try {
            try {
                System.out.println(c.getName() + " готовится к этапу(ждет): " + description);
                MainClass.SEMAPHORE_TUNNEL.acquire(); // въезд в туннель заблокирует, пока семафор не разрешит доступ
                System.out.println(c.getName() + " начал этап: " + description);
                Thread.sleep(length / c.getSpeed() * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(c.getName() + " закончил этап: " + description);
                MainClass.SEMAPHORE_TUNNEL.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}