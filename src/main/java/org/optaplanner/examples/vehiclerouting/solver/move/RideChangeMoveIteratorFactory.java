package org.optaplanner.examples.vehiclerouting.solver.move;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import org.optaplanner.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import org.optaplanner.core.impl.heuristic.move.CompositeMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.move.NoChangeMove;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import org.optaplanner.core.impl.heuristic.selector.move.generic.chained.ChainedChangeMove;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.examples.vehiclerouting.domain.Ride;
import org.optaplanner.examples.vehiclerouting.domain.Standstill;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.Visit;

// Here be dragons...
// This is an experiment so we can build a nice, generic move selector in optaplanner-core.
public class RideChangeMoveIteratorFactory implements MoveIteratorFactory<VehicleRoutingSolution> {

    private GenuineVariableDescriptor<VehicleRoutingSolution> variableDescriptor = null;
    private SingletonInverseVariableSupply inverseVariableSupply = null;

    @Override
    public long getSize(ScoreDirector<VehicleRoutingSolution> scoreDirector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<? extends Move<VehicleRoutingSolution>> createOriginalMoveIterator(ScoreDirector<VehicleRoutingSolution> scoreDirector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<? extends Move<VehicleRoutingSolution>> createRandomMoveIterator(
            ScoreDirector<VehicleRoutingSolution> scoreDirector, Random workingRandom) {
        // TODO DIRTY HACK due to lack of lifecycle methods
        if (inverseVariableSupply == null) {
            // TODO DO NOT USE InnerScoreDirector!
            // No seriously, write a custom move instead of reusing ChainedChangeMove so you don't need any of this
            // Yes, I know, chain correction like in ChainedChangeMove is a big pain to write yourself...
            InnerScoreDirector<VehicleRoutingSolution> innerScoreDirector = (InnerScoreDirector<VehicleRoutingSolution>) scoreDirector;
            variableDescriptor
                    = innerScoreDirector.getSolutionDescriptor().findEntityDescriptorOrFail(Visit.class)
                    .getGenuineVariableDescriptor("previousStandstill");
            // TODO DO NOT demand supplies without returning them
            inverseVariableSupply = innerScoreDirector.getSupplyManager()
                    .demand(new SingletonInverseVariableDemand(variableDescriptor));
        }

        VehicleRoutingSolution solution = scoreDirector.getWorkingSolution();
        // TODO perf loss because it happens at the start of every step but it doesn't different during the phase?
        List<Standstill> standstillList = new ArrayList<>(solution.getVehicleList().size() + solution.getVisitList().size());
        standstillList.addAll(solution.getVehicleList());
        standstillList.addAll(solution.getVisitList());
        return new RideChangeMoveIterator(solution.getRideList(), standstillList, workingRandom);
    }

    private class RideChangeMoveIterator implements Iterator<Move<VehicleRoutingSolution>> {

        private final List<Ride> rideList;
        private final List<Standstill> standstillList;
        private final Random workingRandom;

        public RideChangeMoveIterator(List<Ride> rideList, List<Standstill> standstillList, Random workingRandom) {
            this.rideList = rideList;
            this.standstillList = standstillList;
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return !rideList.isEmpty() && standstillList.size() >= 4;
        }

        @Override
        public Move<VehicleRoutingSolution> next() {
            Ride ride = rideList.get(workingRandom.nextInt(rideList.size()));
            Visit fromPickupVisit = ride.getPickupVisit();
            Visit fromDeliveryVisit = ride.getDeliveryVisit();
            Standstill toPickupVisit = standstillList.get(workingRandom.nextInt(standstillList.size()));
            List<Standstill> potentialToDeliveryVisitList = new ArrayList<>();
            potentialToDeliveryVisitList.add(fromPickupVisit);
            Visit visit = toPickupVisit.getNextVisit();
            while (visit != null) {
                if (visit != fromPickupVisit && visit != fromDeliveryVisit) {
                    potentialToDeliveryVisitList.add(visit);
                }
                visit = visit.getNextVisit();
            }
            Standstill toDeliveryVisit = potentialToDeliveryVisitList.get(workingRandom.nextInt(potentialToDeliveryVisitList.size()));
            ChainedChangeMove<VehicleRoutingSolution> pickupMove = new ChainedChangeMove<>(
                    fromPickupVisit, variableDescriptor, inverseVariableSupply, toPickupVisit);
            ChainedChangeMove<VehicleRoutingSolution> deliveryMove = new ChainedChangeMove<>(
                    fromDeliveryVisit, variableDescriptor, inverseVariableSupply, toDeliveryVisit);

            // With 6.x this workaround code is needed. The CompositeMove will still exclude moves as undoable that shouldn't be excluded
            // With master (7.5.x), this code is entirely fixed by PLANNER-897, which makes this workaround code obsolete
//            if (fromDeliveryVisit.getPreviousStandstill() == fromPickupVisit && fromPickupVisit.getPreviousStandstill() == toDeliveryVisit) {
//                // Delivery move will end up undoable (but isn't undoable to start with)
//                return pickupMove;
//            }
//            if (toDeliveryVisit == fromPickupVisit && toPickupVisit == fromDeliveryVisit.getPreviousStandstill()) {
//                // Delivery move will end up undoable (but isn't undoable to start with)
//                return pickupMove;
//            }
            return new CompositeMove<>(pickupMove, deliveryMove);
        }

    }

}
