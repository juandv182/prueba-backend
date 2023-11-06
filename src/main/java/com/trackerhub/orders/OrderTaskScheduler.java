package com.trackerhub.orders;

import com.trackerhub.scheduling.*;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import static com.trackerhub.backend.services.OrderScheduler.timer;

public class OrderTaskScheduler extends Thread {
    private final Map<VehicleType, Map<VehicleState, Map<String, VehicleThread>>> vehicleThreads;
    private final BlockingQueue<OrderTask> ordersScheduled;
    private final BlockingQueue<VehicleStateChange> vehicleStateChanges;

    public OrderTaskScheduler(Map<VehicleType, Map<VehicleState, Map<String, VehicleThread>>> vehicleThreads,
                              BlockingQueue<OrderTask> ordersScheduled,
                              BlockingQueue<VehicleStateChange> vehicleStateChanges) {
        this.vehicleThreads = vehicleThreads;
        this.ordersScheduled = ordersScheduled;
        this.vehicleStateChanges = vehicleStateChanges;
    }

    @Override
    public void run() {
        while (true) {
            try {
                var order = ordersScheduled.take();
                var stateChanges = new HashMap<VehicleThread, Queue<OrderTask>>();
                var availableCars = Map.copyOf(vehicleThreads.get(VehicleType.CAR).get(VehicleState.AVAILABLE));
                var availableBikes = Map.copyOf(vehicleThreads.get(VehicleType.MOTORCYCLE).get(VehicleState.AVAILABLE));

                if (availableCars.isEmpty() && availableBikes.isEmpty()) {
                    ordersScheduled.put(order);
                    // TODO: signal rescheduler
                    if (order.getOrder().getHoursLimit() <= 8) {
                        tryToReschedule();
                    }
                    // pero, como evitar que a cada rato solicite varios?
                    // se necesita un estado adicional, RETURNING
                    // cada ejecucion debe contar la cantidad de vehiculos retornando
                    // y verificar si es suficiente para
                    Thread.sleep(1000);
                    continue;
                }

                // extract all orders for processing
                var ordersToSchedule = new PriorityQueue<OrderTask>();
                ordersToSchedule.add(order);
                ordersScheduled.drainTo(ordersToSchedule);

                // car scheduling
                if (!availableCars.isEmpty()) {
                    var iter = availableCars.entrySet().iterator();
                    while (true) {
                        Map.Entry<String, VehicleThread> carThread;
                        try {
                            carThread = iter.next();
                        } catch (NoSuchElementException ex) {
                            break;
                        }
                        var carOrders = new PriorityQueue<OrderTask>();
                        int sum = fillOrderQueue(ordersToSchedule, carOrders, VehicleCapacity.CAR.capacity);
                        if (sum == 0) {
                            break;
                        }
                        if (sum >= 10 || availableBikes.isEmpty()) {
//                                System.out.println("Scheduling car with " + sum + " packages and " + carOrders.size() + " tasks.");
                            stateChanges.put(carThread.getValue(), carOrders);
                        } else {
                            // return orders for bike usage
                            ordersToSchedule.addAll(carOrders);
                            break;
                        }
                    }
                }

                // bike scheduling
                if (!availableBikes.isEmpty()) {
                    var iter = availableBikes.entrySet().iterator();
                    while (true) {
                        Map.Entry<String, VehicleThread> bikeThread;
                        try {
                            bikeThread = iter.next();
                        } catch (NoSuchElementException ex) {
                            break;
                        }
                        var bikeOrders = new PriorityQueue<OrderTask>();
                        int sum = fillOrderQueue(ordersToSchedule, bikeOrders, VehicleCapacity.MOTORCYCLE.capacity);
                        if (sum == 0) break;
                        stateChanges.put(bikeThread.getValue(), bikeOrders);
                    }
                }

                // apply state changes
                for (var entry: stateChanges.entrySet()) {
                    var thread = entry.getKey();
                    var task = entry.getValue();
                    vehicleStateChanges.put(new VehicleStateChange(VehicleState.AVAILABLE, VehicleState.BUSY, thread.getVehicleType(), thread.getVehicleId()));
                    thread.assignTask(task);
                }

                // return any remaining orders
                for (var remaining: ordersToSchedule) {
                    ordersScheduled.put(remaining);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private int fillOrderQueue(Queue<OrderTask> toSchedule, Queue<OrderTask> scheduled, int capacity) {
        int totalSum = 0;
        while (true) {
            var task = toSchedule.poll();
            if (task == null) break;
            totalSum += task.getNumPackages();
            if (totalSum > capacity) {
                totalSum -= task.getNumPackages();
                toSchedule.add(task);
                return totalSum;
            } else {
                scheduled.add(task);
            }
        }
        return totalSum;
    }

    private void tryToReschedule() {
        var busyCars = Map.copyOf(vehicleThreads.get(VehicleType.CAR).get(VehicleState.BUSY));
        var busyBikes = Map.copyOf(vehicleThreads.get(VehicleType.MOTORCYCLE).get(VehicleState.BUSY));
        // calculate total orders in queue
        long sum = 0;
        for (var order: ordersScheduled) {
            sum += order.getNumPackages();
        }
        // check if returning vehicles are enough
        var returningCars = getReturningVehicles(busyCars);
        var returningBikes = getReturningVehicles(busyBikes);
        var totalReturningCapacity = returningBikes.size() * VehicleCapacity.MOTORCYCLE.capacity
                + returningCars.size() * VehicleCapacity.CAR.capacity;
        if (totalReturningCapacity >= sum) {
            return;
        }
        // request one vehicle at a time
        var reschedulableCars = getReschedulableVehicles(busyCars);
        var reschedulableBikes = getReschedulableVehicles(busyBikes);
        if (reschedulableBikes.isEmpty() && reschedulableCars.isEmpty()) {
            return;
        }
        if (reschedulableCars.size() > 1) {
            reschedulableCars.get(0).triggerReschedule();
        } else if (reschedulableBikes.size() > 1) {
            reschedulableBikes.get(0).triggerReschedule();
        }
    }

    private ArrayList<VehicleThread> getReschedulableVehicles(Map<String, VehicleThread> vehicles) {
        var result = new ArrayList<VehicleThread>();
        vehicles.forEach((k, v) -> {
            if (v.isReturning()) {
                return;
            }
            // todos los pedidos deben tener + 8 horas
            if (v.getCurrentTask() != null && timer.until(v.getCurrentTask().getOrder().getDateLimit(), ChronoUnit.HOURS) < 8) {
                return;
            }
            for (var order: v.getTasks()) {
                int hoursLimit = order.getOrder().getHoursLimit();
                if ((hoursLimit != 0 && timer.until(order.getOrder().getDateLimit(), ChronoUnit.HOURS) < 8) || order.isRescue()) {
                    return;
                }
            }
            result.add(v);
        });
        return result;
    }

    private ArrayList<VehicleThread> getReturningVehicles(Map<String, VehicleThread> vehicles) {
        var result = new ArrayList<VehicleThread>();
        vehicles.forEach((k, v) -> {
            if (v.isReturning()) {
                result.add(v);
            }
        });
        return result;
    }
}
