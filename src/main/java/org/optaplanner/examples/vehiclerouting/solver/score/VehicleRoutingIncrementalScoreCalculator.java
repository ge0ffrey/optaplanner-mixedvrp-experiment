/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.vehiclerouting.solver.score;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.impl.score.director.incremental.AbstractIncrementalScoreCalculator;
import org.optaplanner.examples.vehiclerouting.domain.Visit;
import org.optaplanner.examples.vehiclerouting.domain.Standstill;
import org.optaplanner.examples.vehiclerouting.domain.Vehicle;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedVisit;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedVehicleRoutingSolution;

public class VehicleRoutingIncrementalScoreCalculator extends AbstractIncrementalScoreCalculator<VehicleRoutingSolution> {

    private boolean timeWindowed;
    private Map<Vehicle, Integer> vehicleDemandMap;

    private long hardScore;
    private long softScore;

    @Override
    public void resetWorkingSolution(VehicleRoutingSolution solution) {
        timeWindowed = solution instanceof TimeWindowedVehicleRoutingSolution;
        List<Vehicle> vehicleList = solution.getVehicleList();
        vehicleDemandMap = new HashMap<>(vehicleList.size());
        for (Vehicle vehicle : vehicleList) {
            vehicleDemandMap.put(vehicle, 0);
        }
        hardScore = 0L;
        softScore = 0L;
        for (Visit visit : solution.getVisitList()) {
            insertPreviousStandstill(visit);
            insertVehicle(visit);
            // Do not do insertNextCustomer(visit) to avoid counting distanceFromLastCustomerToDepot twice
            if (timeWindowed) {
                insertArrivalTime((TimeWindowedVisit) visit);
            }
        }
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(Object entity) {
        if (entity instanceof Vehicle) {
            return;
        }
        insertPreviousStandstill((Visit) entity);
        insertVehicle((Visit) entity);
        // Do not do insertNextCustomer(customer) to avoid counting distanceFromLastCustomerToDepot twice
        if (timeWindowed) {
            insertArrivalTime((TimeWindowedVisit) entity);
        }
    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
        if (entity instanceof Vehicle) {
            return;
        }
        switch (variableName) {
            case "previousStandstill":
                retractPreviousStandstill((Visit) entity);
                break;
            case "vehicle":
                retractVehicle((Visit) entity);
                break;
            case "nextVisit":
                retractNextCustomer((Visit) entity);
                break;
            case "arrivalTime":
                retractArrivalTime((TimeWindowedVisit) entity);
                break;
            default:
                throw new IllegalArgumentException("Unsupported variableName (" + variableName + ").");
        }
    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        if (entity instanceof Vehicle) {
            return;
        }
        switch (variableName) {
            case "previousStandstill":
                insertPreviousStandstill((Visit) entity);
                break;
            case "vehicle":
                insertVehicle((Visit) entity);
                break;
            case "nextVisit":
                insertNextCustomer((Visit) entity);
                break;
            case "arrivalTime":
                insertArrivalTime((TimeWindowedVisit) entity);
                break;
            default:
                throw new IllegalArgumentException("Unsupported variableName (" + variableName + ").");
        }
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        if (entity instanceof Vehicle) {
            return;
        }
        retractPreviousStandstill((Visit) entity);
        retractVehicle((Visit) entity);
        // Do not do retractNextCustomer(customer) to avoid counting distanceFromLastCustomerToDepot twice
        if (timeWindowed) {
            retractArrivalTime((TimeWindowedVisit) entity);
        }
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        // Do nothing
    }

    private void insertPreviousStandstill(Visit visit) {
        Standstill previousStandstill = visit.getPreviousStandstill();
        if (previousStandstill != null) {
            // Score constraint distanceToPreviousStandstill
            softScore -= visit.getDistanceFromPreviousStandstill();
        }
    }

    private void retractPreviousStandstill(Visit visit) {
        Standstill previousStandstill = visit.getPreviousStandstill();
        if (previousStandstill != null) {
            // Score constraint distanceToPreviousStandstill
            softScore += visit.getDistanceFromPreviousStandstill();
        }
    }

    private void insertVehicle(Visit visit) {
        Vehicle vehicle = visit.getVehicle();
        if (vehicle != null) {
            // Score constraint vehicleCapacity
            int capacity = vehicle.getCapacity();
            int oldDemand = vehicleDemandMap.get(vehicle);
            int newDemand = oldDemand + visit.getDemand();
            hardScore += Math.min(capacity - newDemand, 0) - Math.min(capacity - oldDemand, 0);
            vehicleDemandMap.put(vehicle, newDemand);
            if (visit.getNextVisit() == null) {
                // Score constraint distanceFromLastCustomerToDepot
                softScore -= visit.getLocation().getDistanceTo(vehicle.getLocation());
            }
        }
    }

    private void retractVehicle(Visit visit) {
        Vehicle vehicle = visit.getVehicle();
        if (vehicle != null) {
            // Score constraint vehicleCapacity
            int capacity = vehicle.getCapacity();
            int oldDemand = vehicleDemandMap.get(vehicle);
            int newDemand = oldDemand - visit.getDemand();
            hardScore += Math.min(capacity - newDemand, 0) - Math.min(capacity - oldDemand, 0);
            vehicleDemandMap.put(vehicle, newDemand);
            if (visit.getNextVisit() == null) {
                // Score constraint distanceFromLastCustomerToDepot
                softScore += visit.getLocation().getDistanceTo(vehicle.getLocation());
            }
        }
    }

    private void insertNextCustomer(Visit visit) {
        Vehicle vehicle = visit.getVehicle();
        if (vehicle != null) {
            if (visit.getNextVisit() == null) {
                // Score constraint distanceFromLastCustomerToDepot
                softScore -= visit.getLocation().getDistanceTo(vehicle.getLocation());
            }
        }
    }

    private void retractNextCustomer(Visit visit) {
        Vehicle vehicle = visit.getVehicle();
        if (vehicle != null) {
            if (visit.getNextVisit() == null) {
                // Score constraint distanceFromLastCustomerToDepot
                softScore += visit.getLocation().getDistanceTo(vehicle.getLocation());
            }
        }
    }

    private void insertArrivalTime(TimeWindowedVisit customer) {
        Long arrivalTime = customer.getArrivalTime();
        if (arrivalTime != null) {
            long dueTime = customer.getDueTime();
            if (dueTime < arrivalTime) {
                // Score constraint arrivalAfterDueTime
                hardScore -= (arrivalTime - dueTime);
            }
        }
        // Score constraint arrivalAfterDueTimeAtDepot is a built-in hard constraint in VehicleRoutingImporter
    }

    private void retractArrivalTime(TimeWindowedVisit customer) {
        Long arrivalTime = customer.getArrivalTime();
        if (arrivalTime != null) {
            long dueTime = customer.getDueTime();
            if (dueTime < arrivalTime) {
                // Score constraint arrivalAfterDueTime
                hardScore += (arrivalTime - dueTime);
            }
        }
    }

    @Override
    public HardSoftLongScore calculateScore() {
        return HardSoftLongScore.valueOf(hardScore, softScore);
    }

}
