package com.project;


import java.util.*;
import java.util.concurrent.*;


public class TrafficLight {

    static final ConcurrentMap<String, MultiValueMap<Integer, Integer>> threadData = new ConcurrentHashMap<>();
    static  int count_threads = 4;

    static List<String> activeThreads;

    private static CyclicBarrier barrier = new CyclicBarrier(4);

    public static void changeBarrier(int count_threads){
        if (count_threads != TrafficLight.count_threads && count_threads>0){
            TrafficLight.count_threads = count_threads;
            barrier = new CyclicBarrier(count_threads);
        }
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

        static private void setDirection() {
            direction = new ArrayList<>();
            direction.add("straight");
            direction.add("right");
            direction.add("left");
        }

        static private void setFrom() {
            from = new ArrayList<>();
            from.add("south");
            from.add("north");
            from.add("west");
            from.add("east");
        }


        static private Integer leftFrom(Integer fromIndex) {
            String from = Car.from.get(fromIndex);
            return switch (from) {
                case "south" -> findIndex(Car.from, "west");
                case "west" -> findIndex(Car.from, "north");
                case "north" -> findIndex(Car.from, "east");
                case "east" -> findIndex(Car.from, "south");
                default -> throw new IllegalArgumentException("Invalid direction: " + from);
            };
        }

        static Integer rightFrom(Integer fromIndex) {
            String from = Car.from.get(fromIndex);
            return switch (from) {
                case "south" -> findIndex(Car.from, "east");
                case "east" -> findIndex(Car.from, "north");
                case "north" -> findIndex(Car.from, "west");
                case "west" -> findIndex(Car.from, "south");
                default -> throw new IllegalArgumentException("Invalid direction: " + from);
            };
        }

        static private Integer reverseFrom(Integer fromIndex) {
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

            //System.out.println("Параллельные направления для " + Car.from.get(fromIndex) + " -> " + direction + ":");
            for (Map.Entry<Integer, Collection<Integer>> entry : canRide.entrySet()) {
                Integer key = entry.getKey();
                Collection<Integer> values = entry.getValue();
                for (Integer value : values) {
                    //System.out.println(Car.from.get(key) + " -> " + Car.direction.get(value));
                }
            }
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
                //System.out.println(threadName + " добавляет свои данные в threadData: " + canRide);
                threadData.put(threadName, canRide);
            }

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.err.println(threadName + " был прерван.");
            }
            int k =0;
//            boolean flag = false;
            while (!threadData.isEmpty() && threadData.containsKey(threadName)){
                k+=1;
                if (threadName.equals(threadData.keySet().iterator().next())){
                    System.out.println("\n\tШаг выполнения: "+ k);
                }
                //System.out.println("Current threadData( " + threadName+  " ): " + threadData);

                    if (activeThreads == null) {
                        activeThreads = new ArrayList<>();
                        //System.out.println("<" + threadName + " - " + k + "> создает массив активных потоков");
                    }
                    if (TrafficLight.activeThreads.isEmpty()) {
                        TrafficLight.activeThreads.add(threadName);
                    } else {
                        if (canRideParallel(threadName, activeThreads)) {
                            activeThreads.add(threadName);
                        }
                    }
                    //System.out.println("<" + threadName + " - " + k + "> activeThreeads" + activeThreads);

                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    System.err.println(threadName + " был прерван.");
                    break;
                }

                //System.out.println("<" + threadName +  " - " + k +"> Двигаемся дальше");


                //System.out.println("1) <" + threadName +  " - " + k +"> delete thread:" + activeThreads);
                if (activeThreads.contains(threadName)){
                    activeThreads.remove(threadName);
                    //System.out.println("2) <" + threadName +  " - " + k +"> NewactiveThreads: " + activeThreads);
                    threadData.remove(threadName);
                    //System.out.println("3) <" + threadName +  " - " + k +"> threadDATA "  + threadData);
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

        private boolean canRideParallel(String threadName, List<String> activeThreads) {
            MultiValueMap<Integer, Integer> currentThreadData = threadData.get(threadName);

            for (String activeThread : activeThreads) {
                MultiValueMap<Integer, Integer> activeThreadData = threadData.get(activeThread);

                for (Map.Entry<Integer, Collection<Integer>> entry : currentThreadData.entrySet()) {
                    Integer key = entry.getKey();
                    if (activeThreadData.containsKey(key)) {
                        Collection<Integer> values = entry.getValue();
                        for (Integer value : values) {
                            if (activeThreadData.get(key).contains(value)) {
                                return false;
                            }
                        }
                    }
                }
            }

            return true;
        }


        private boolean canRideParallel(String threadName, String otherThread) {
                MultiValueMap<Integer, Integer> otherThreadData = threadData.get(otherThread);

                for (Map.Entry<Integer, Collection<Integer>> entry : canRide.entrySet()) {
                    Integer key = entry.getKey();
                    if (otherThreadData.containsKey(key)) {
                        Collection<Integer> values = entry.getValue();
                        for (Integer value : values) {
                            if (otherThreadData.get(key).contains(value)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }

    }

    public static void main(String[] args) {
        //System.out.println("Запуск потоков...");
        Thread t1 = new Thread(new Car("south", "straight"), "south-straight");
        Thread t2 = new Thread(new Car("east", "left"), "east-left");
        Thread t3 = new Thread(new Car("north", "straight"), "north-straight");
        Thread t4 = new Thread(new Car("west", "straight"), "west-straight");
        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }
}