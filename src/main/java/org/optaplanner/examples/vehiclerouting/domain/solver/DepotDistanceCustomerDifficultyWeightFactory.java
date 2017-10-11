/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.examples.vehiclerouting.domain.solver;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import org.optaplanner.examples.vehiclerouting.domain.Visit;
import org.optaplanner.examples.vehiclerouting.domain.Depot;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;

/**
 * On large datasets, the constructed solution looks like a Matryoshka doll.
 */
public class DepotDistanceCustomerDifficultyWeightFactory
        implements SelectionSorterWeightFactory<VehicleRoutingSolution, Visit> {

    @Override
    public DepotDistanceCustomerDifficultyWeight createSorterWeight(VehicleRoutingSolution vehicleRoutingSolution, Visit visit) {
        Depot depot = vehicleRoutingSolution.getDepotList().get(0);
        return new DepotDistanceCustomerDifficultyWeight(visit,
                visit.getLocation().getDistanceTo(depot.getLocation())
                        + depot.getLocation().getDistanceTo(visit.getLocation()));
    }

    public static class DepotDistanceCustomerDifficultyWeight
            implements Comparable<DepotDistanceCustomerDifficultyWeight> {

        private final Visit visit;
        private final long depotRoundTripDistance;

        public DepotDistanceCustomerDifficultyWeight(Visit visit,
                long depotRoundTripDistance) {
            this.visit = visit;
            this.depotRoundTripDistance = depotRoundTripDistance;
        }

        @Override
        public int compareTo(DepotDistanceCustomerDifficultyWeight other) {
            return new CompareToBuilder()
                    .append(depotRoundTripDistance, other.depotRoundTripDistance) // Ascending (further from the depot are more difficult)
                    .append(visit.getRideSize(), other.visit.getRideSize())
                    .append(visit.getLocation().getLatitude(), other.visit.getLocation().getLatitude())
                    .append(visit.getLocation().getLongitude(), other.visit.getLocation().getLongitude())
                    .append(visit.getId(), other.visit.getId())
                    .toComparison();
        }

    }

}
