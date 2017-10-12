package org.optaplanner.examples.vehiclerouting.solver.move;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import org.optaplanner.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import org.optaplanner.core.impl.heuristic.move.CompositeMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import org.optaplanner.core.impl.heuristic.selector.move.generic.chained.ChainedSwapMove;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.examples.vehiclerouting.domain.Ride;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.Visit;

// Here be dragons...
// This is an experiment so we can build a nice, generic move selector in optaplanner-core.
public class RideSwapMoveIteratorFactory implements MoveIteratorFactory<VehicleRoutingSolution> {

    private List<GenuineVariableDescriptor<VehicleRoutingSolution>> variableDescriptorList = null;
    private List<SingletonInverseVariableSupply> inverseVariableSupplyList = null;

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
        if (inverseVariableSupplyList == null) {
            // TODO DO NOT USE InnerScoreDirector!
            // No seriously, write a custom move instead of reusing ChainedSwapMove so you don't need any of this
            // Yes, I know, chain correction like in ChainedSwapMove is a big pain to write yourself...
            InnerScoreDirector<VehicleRoutingSolution> innerScoreDirector = (InnerScoreDirector<VehicleRoutingSolution>) scoreDirector;
            GenuineVariableDescriptor<VehicleRoutingSolution> variableDescriptor
                    = innerScoreDirector.getSolutionDescriptor().findEntityDescriptorOrFail(Visit.class)
                    .getGenuineVariableDescriptor("previousStandstill");
            variableDescriptorList = Collections.singletonList(variableDescriptor);
            inverseVariableSupplyList = Collections.singletonList(innerScoreDirector.getSupplyManager()
                    .demand(new SingletonInverseVariableDemand(variableDescriptor)));
        }

        VehicleRoutingSolution solution = scoreDirector.getWorkingSolution();
        return new RideSwapMoveIterator(solution.getRideList(), workingRandom);
    }

    private class RideSwapMoveIterator implements Iterator<Move<VehicleRoutingSolution>> {

        private final List<Ride> rideList;
        private final Random workingRandom;

        public RideSwapMoveIterator(List<Ride> rideList, Random workingRandom) {
            this.rideList = rideList;
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return rideList.size() >= 2;
        }

        @Override
        public Move<VehicleRoutingSolution> next() {
            int leftRideIndex = workingRandom.nextInt(rideList.size());
            Ride leftRide = rideList.get(leftRideIndex);
            int rightRideIndex = workingRandom.nextInt(rideList.size() - 1);
            if (rightRideIndex >= leftRideIndex) {
                rightRideIndex++;
            }
            Ride rightRide = rideList.get(rightRideIndex);

            // TODO if one of the 2 moves is undoable, we'd still want to do the other move...
            return new CompositeMove<>(
                    new ChainedSwapMove<>(variableDescriptorList, inverseVariableSupplyList,
                            leftRide.getPickupVisit(), rightRide.getPickupVisit()),
                    new ChainedSwapMove<>(variableDescriptorList, inverseVariableSupplyList,
                            leftRide.getDeliveryVisit(), rightRide.getDeliveryVisit())
            );

        }

    }

}
