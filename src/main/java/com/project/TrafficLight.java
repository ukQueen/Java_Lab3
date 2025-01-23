package com.project;


import java.util.*;
import java.util.concurrent.*;


public class TrafficLight {

    private static final ConcurrentMap<String, MultiValueMap<Integer, Integer>> threadData = new ConcurrentHashMap<>();
    private static int count_threads = 4;

    private static List<String> activeThreads;

    private static CyclicBarrier barrier = new CyclicBarrier(4);
//    private static final Object lock = new Object();

    public static void changeBarrier(int count_threads){
        if (count_threads != TrafficLight.count_threads && count_threads>0){
            TrafficLight.count_threads = count_threads;
            barrier = new CyclicBarrier(count_threads);
        }
    }

    public static void startCars(Thread t1, Thread t2, Thread t3, Thread t4){
        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }

    public static class Car implements Runnable {
        static List<String> direction;
        static List<String> from;

        Integer fromIndex;
        Integer directionIndex;
        MultiValueMap<Integer, Integer> canRide;


        public Car(String from, String direction) {
            setFrom();
            setDirection();
            fromIndex = findIndex(Car.from, from);
            directionIndex = findIndex(Car.direction, direction);
            canRide();
        }

        private static void setDirection() {
            direction = new ArrayList<>();
            direction.add("straight");
            direction.add("right");
            direction.add("left");
        }

        private static void setFrom() {
            from = new ArrayList<>();
            from.add("south");
            from.add("north");
            from.add("west");
            from.add("east");
        }

        private static Integer leftFrom(Integer fromIndex) {
            String from = Car.from.get(fromIndex);
            return switch (from) {
                case "south" -> findIndex(Car.from, "west");
                case "west" -> findIndex(Car.from, "north");
                case "north" -> findIndex(Car.from, "east");
                case "east" -> findIndex(Car.from, "south");
                default -> throw new IllegalArgumentException("Invalid direction: " + from);
            };
        }

        private static Integer rightFrom(Integer fromIndex) {
            String from = Car.from.get(fromIndex);
            return switch (from) {
                case "south" -> findIndex(Car.from, "east");
                case "east" -> findIndex(Car.from, "north");
                case "north" -> findIndex(Car.from, "west");
                case "west" -> findIndex(Car.from, "south");
                default -> throw new IllegalArgumentException("Invalid direction: " + from);
            };
        }

        private static Integer reverseFrom(Integer fromIndex) {
            String from = Car.from.get(fromIndex);
            return switch (from) {
                case "south" -> findIndex(Car.from, "north");
                case "east" -> findIndex(Car.from, "west");
                case "north" -> findIndex(Car.from, "south");
                case "west" -> findIndex(Car.from, "east");
                default -> throw new IllegalArgumentException("Invalid direction: " + from);
            };
        }

        //то с каких сторон и в какие направления может проехать параллельно
        private void canRide() {
            this.canRide = new MultiValueMap<>();
            String direction = Car.direction.get(directionIndex);
            List<Integer> left = new ArrayList<>();
            List<Integer> right = new ArrayList<>();
            List<Integer> reverse = new ArrayList<>();

            Integer directionLeftIndex = Car.findIndex(Car.direction, "left");
            Integer directionRightIndex = Car.findIndex(Car.direction, "right");
            Integer directionStraightIndex = Car.findIndex(Car.direction, "straight");
            switch (direction) {
                case "straight":
                    reverse.add(directionStraightIndex);
                    reverse.add(directionRightIndex);

                    left.add(directionRightIndex);
                    break;
                case "left":
                    right.add(directionRightIndex);

                    left.add(directionRightIndex);
                    break;
                case "right":
                    reverse.add(directionStraightIndex);
                    reverse.add(directionRightIndex);

                    left.add(directionRightIndex);

                    right.add(directionStraightIndex);
                    right.add(directionLeftIndex);
                    right.add(directionRightIndex);
                    break;
            }
            canRide.put(reverseFrom(fromIndex), reverse);
            canRide.put(leftFrom(fromIndex), left);
            canRide.put(rightFrom(fromIndex), right);
        }

        public static int findIndex(List<String> array, String value) {
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).equals(value)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            synchronized (threadData) {
                threadData.put(threadName, canRide);
            }

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.err.println(threadName + " был прерван.");
            }

            int k =0;
            while (!threadData.isEmpty() && threadData.containsKey(threadName)){
                k+=1;
                if (threadName.equals(threadData.keySet().iterator().next())){
                    System.out.println("\n\tШаг выполнения: "+ k);
                }
//                synchronized (lock) {
                    if (activeThreads == null) {
                        activeThreads = new ArrayList<>();
                    }
                    if (TrafficLight.activeThreads.isEmpty()) {
                        TrafficLight.activeThreads.add(threadName);
                    } else {
                        if (canRideParallel(activeThreads)) {
                            activeThreads.add(threadName);
                        }
                    }
//                }

                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    System.err.println(threadName + " был прерван.");
                    break;
                }

                if (activeThreads.contains(threadName)){
                    activeThreads.remove(threadName);
                    threadData.remove(threadName);
                    System.out.println(threadName);
                }

                try {
                    barrier.await();

                    int count_threads = threadData.size();
                    changeBarrier(count_threads);
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    System.err.println(threadName + " был прерван.");
                    break;
                }
            }
        }

        private boolean canRideParallel(List<String> activeThreads) {
            for (String activeThread : activeThreads) {
                MultiValueMap<Integer, Integer> activeThreadData = threadData.get(activeThread); //canRide
                boolean flag = false;

                for (Map.Entry<Integer, Collection<Integer>> threadData : activeThreadData.entrySet()) { // ключ со значениями в canRide
                    Integer key = threadData.getKey();
                    if (key.equals(fromIndex)) {
                        Collection<Integer> values = threadData.getValue();
                        if(values.contains(directionIndex)){
                            flag = true;
                            break;
                        }
                    }
                }
                if (!flag){
                    return false;
                }
            }
            return true;
        }
    }


    public static void main(String[] args) {
        Thread t1 = new Thread(new Car("south", "straight"), "south-straight");
        Thread t2 = new Thread(new Car("east", "left"), "east-left");
        Thread t3 = new Thread(new Car("north", "straight"), "north-straight");
        Thread t4 = new Thread(new Car("west", "straight"), "west-straight");

        TrafficLight.startCars(t1, t2, t3, t4);
    }
}