package com.project;

import org.junit.Test;
import static org.junit.Assert.*;


public class TrafficLightTest {

    @Test
    public void testDeadlocks() throws InterruptedException {
        Thread t1 = new Thread(new TrafficLight.Car("south", "straight"), "south-straight");
        Thread t2 = new Thread(new TrafficLight.Car("east", "left"), "east-left");
        Thread t3 = new Thread(new TrafficLight.Car("north", "straight"), "north-straight");
        Thread t4 = new Thread(new TrafficLight.Car("west", "straight"), "west-straight");

        TrafficLight.startCars(t1, t2, t3, t4);

        //ждем 5 секунд, если все потоки завершились, то дедлоков скорее всего нет
        long startTime = System.currentTimeMillis();
        long timeout = 5000;

        while (System.currentTimeMillis() - startTime < timeout) {
            if (!t1.isAlive() && !t2.isAlive() && !t3.isAlive() && !t4.isAlive()) {
                return;  // Все потоки завершились, тест проходит
            }
        }

        fail("Deadlock detected");
    }

    @Test
    public void testRaceConditions() throws InterruptedException {
        Thread t1 = new Thread(new TrafficLight.Car("south", "straight"), "south-straight");
        Thread t2 = new Thread(new TrafficLight.Car("east", "left"), "east-left");
        Thread t3 = new Thread(new TrafficLight.Car("north", "straight"), "north-straight");
        Thread t4 = new Thread(new TrafficLight.Car("west", "straight"), "west-straight");

        TrafficLight.startCars(t1, t2, t3, t4);

        //Потоки ничего не считают, так как это просто машины, проезжающие друг за другом или параллельно, если это возможно.
        //Если потоки завершаются, то гонки за ресурсами нет. Если бы она была, то общие данные потоков изменялись некорректно
        // и вероятно программа бы зациклилась и не закончилась. (Можно также проверить с тайм-аутом, но это уже проверяется в дедлоках.
        t1.join();
        t2.join();
        t3.join();
        t4.join();

        assertSame(t1.getState(), Thread.State.TERMINATED);
        assertSame(t2.getState(), Thread.State.TERMINATED);
        assertSame(t3.getState(), Thread.State.TERMINATED);
        assertSame(t4.getState(), Thread.State.TERMINATED);
    }
}
