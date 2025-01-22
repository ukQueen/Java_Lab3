package com.project;


import javax.swing.plaf.multi.MultiToolTipUI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;


public class TrafficLight {



//    public TrafficLight(){
//        direction = new ArrayList<>();
//
//        direction.add("straight");
//        direction.add("right");
//        direction.add("left");
//
//        from = new ArrayList<>();
//        from.add("south");
//        from.add("north");
//        from.add("west");
//        from.add("east");
//    }

    static final ConcurrentMap<String, MultiValueMap<Integer, Integer>> threadData = new ConcurrentHashMap<>();
    private static final Object lock = new Object(); // Для синхронизации потоков
    private static final CountDownLatch latch;

    static {
        latch = new CountDownLatch(4); // Количество потоков
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

//        static private String leftFrom(String from){
//            return switch (from) {
//                case "south" -> "west";
//                case "west" -> "north";
//                case "north" -> "east";
//                case "east" -> "south";
//                default -> throw new IllegalArgumentException("Invalid direction: " + from);
//            };
//        }

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

//        static String rightFrom(String from){
//            return switch (from) {
//                case "south" -> "east";
//                case "east" -> "north";
//                case "north" -> "west";
//                case "west" -> "south";
//                default -> throw new IllegalArgumentException("Invalid direction: " + from);
//            };
//        }

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

//        static private String reverseFrom(String from){
//            return switch (from) {
//                case "south" -> "north";
//                case "east" -> "west";
//                case "north" -> "south";
//                case "west" -> "east";
//                default -> throw new IllegalArgumentException("Invalid direction: " + from);
//            };
//        }

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

        //то с каких сторон и в какие направления модет проехать параллельно
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

            System.out.println("Параллельные направления для " + Car.from.get(fromIndex) + " -> " + direction + ":");
            for (Map.Entry<Integer, Collection<Integer>> entry : canRide.entrySet()) {
                Integer key = entry.getKey();
                Collection<Integer> values = entry.getValue();
                for (Integer value : values) {
                    System.out.println(Car.from.get(key) + " -> " + Car.direction.get(value));
                }
            }
        }

        public static int findIndex(List<String> array, String value) {
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).equals(value)) {
//                    System.out.println("Найден индекс: " + i + " для значения: " + value);
                    return i;
                }
            }
//            System.out.println("Не найден индекс для значения: " + value);
            return -1;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();

            // Добавление данных потока в общую карту
            synchronized (threadData) {
                System.out.println(threadName + " добавляет свои данные в threadData: " + canRide);
                threadData.put(threadName, canRide);
            }

            try {
                latch.countDown(); // Уменьшаем счетчик latch
                latch.await(); // Ожидаем, пока все потоки не будут добавлены
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            List<String> parallelThreads = new ArrayList<>();
            synchronized (threadData) {
                for (String otherThread : threadData.keySet()) {
                    if (!otherThread.equals(threadName)) {
                        // Проверка на пересечение данных
                        if (canRideParallel(threadName, otherThread)) {
                            parallelThreads.add(otherThread);
                        }
                    }
                }
            }

            // Выводим информацию о том, с какими потоками можем работать параллельно
            if (!parallelThreads.isEmpty()) {
                System.out.println(threadName + " может работать параллельно с: " + parallelThreads);
            } else {
                System.out.println(threadName + " не может работать параллельно с другими потоками.");
            }

            // Выполнение поочередно, если параллельное выполнение невозможно
            synchronized (lock) {
                try {
                    if (parallelThreads.isEmpty()) {
                        System.out.println(threadName + " выполняется поочередно");
                        Thread.sleep(1000); // Имитация работы потока
                    } else {
                        System.out.println(threadName + " выполняется параллельно с: " + parallelThreads);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                lock.notifyAll();
            }

            // Удаление данных потока после выполнения
            synchronized (threadData) {
                threadData.remove(threadName);
            }
        }

//            String threadName = Thread.currentThread().getName();
//
//
//                threadData.put(threadName, canRide);
//
//                while (!canRideParallel(threadName)) {
//                    try {
//                        lock.wait();
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//
//                System.out.println(threadName + " выполняется...");
//                // Симулируем выполнение
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//
//                // Уведомляем другие потоки
//                threadData.remove(threadName);
//                lock.notifyAll();

//        }

//        private boolean canRideParallel(String threadName){
//            Set<String> checkedThreads = new HashSet<>();
//            for (String otherThread: threadData.keySet()){
//                if (!otherThread.equals(threadName) && !checkedThreads.contains(otherThread)){
//                    MultiValueMap<Integer, Integer> otherData = threadData.get(otherThread);
//                    if (checkConflict(canRide, otherData)){
//                        return true;
//                    }
//                    checkedThreads.add(otherThread);
//                }
//            }
//            return false;
//        }
//
//        private boolean checkConflict(MultiValueMap<Integer, Integer> current, MultiValueMap<Integer, Integer> other) {
//            for (Map.Entry<Integer, Collection<Integer>> entry : current.entrySet()) {
//                Integer key = entry.getKey();
//                if (other.containsKey(key)) {
//                    for (Integer value : entry.getValue()) {
//                        if (other.get(key).contains(value)) {
//                            return true;
//                        }
//                    }
//                }
//            }
//            return false;
//        }


//        private boolean canRideParallel(String threadName) {
//            for (Map.Entry<String, MultiValueMap<Integer, Integer>> entry : threadData.entrySet()) {
//                if (!entry.getKey().equals(threadName)) {
//                    MultiValueMap<Integer, Integer> otherThread = entry.getValue();
//                    System.out.println(threadName + " проверяет пересечение с данными потока: " + entry.getKey());
//                    for (Map.Entry<Integer, Collection<Integer>> rideEntry : canRide.entrySet()) {
//                        Integer key = rideEntry.getKey();
//                        if (otherThread.containsKey(key)) {
//                            Collection<Integer> values = rideEntry.getValue();
//                            for (Integer value: values) {
//                                if (otherThread.get(key).contains(value)) {
//                                    System.out.println(threadName + " и " + entry.getKey() + " пересекаются по ключу: " + key + " и значению: " + value);
//                                    return true;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            return false;
//        }

            private boolean canRideParallel(String threadName, String otherThread) {
                MultiValueMap<Integer, Integer> otherThreadData = threadData.get(otherThread);

                // Сравниваем данные потока и другого потока на наличие конфликта
                for (Map.Entry<Integer, Collection<Integer>> entry : canRide.entrySet()) {
                    Integer key = entry.getKey();
                    if (otherThreadData.containsKey(key)) {
                        Collection<Integer> values = entry.getValue();
                        for (Integer value : values) {
                            if (otherThreadData.get(key).contains(value)) {
//                                System.out.println(threadName + " и " + otherThread + " пересекаются по ключу: " + key + " и значению: " + value);
                                return false; // Есть конфликт, не можем выполнять параллельно
                            }
                        }
                    }
                }
                return true; // Нет конфликта, можно выполнять параллельно
            }

    }

    public static void main(String[] args) {
        System.out.println("Запуск потоков...");
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