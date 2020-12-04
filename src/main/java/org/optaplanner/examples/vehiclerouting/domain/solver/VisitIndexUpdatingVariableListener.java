/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

import java.util.Objects;

import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.examples.vehiclerouting.domain.Standstill;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.Visit;

public class VisitIndexUpdatingVariableListener implements VariableListener<VehicleRoutingSolution, Visit> {

    @Override
    public void beforeEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        updateVisitIndex(scoreDirector, visit);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        updateVisitIndex(scoreDirector, visit);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // Do nothing
    }

    protected void updateVisitIndex(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit sourceVisit) {
        Standstill previousStandstill = sourceVisit.getPreviousStandstill();
        Integer visitIndex;
        if (previousStandstill == null) {
            visitIndex = null;
        } else {
            visitIndex = previousStandstill.getVisitIndex();
            if (visitIndex != null) {
                visitIndex++;
            }
        }
        Visit shadowVisit = sourceVisit;
        while (shadowVisit != null && !Objects.equals(shadowVisit.getVisitIndex(), visitIndex)) {
            scoreDirector.beforeVariableChanged(shadowVisit, "visitIndex");
            shadowVisit.setVisitIndex(visitIndex);
            scoreDirector.afterVariableChanged(shadowVisit, "visitIndex");
            shadowVisit = shadowVisit.getNextVisit();
            if (visitIndex != null) {
                visitIndex++;
            }
        }
    }

}
