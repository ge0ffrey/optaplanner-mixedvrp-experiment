package org.optaplanner.examples.vehiclerouting.domain;

import org.optaplanner.core.api.domain.solution.cloner.DeepPlanningClone;
import org.optaplanner.examples.common.domain.AbstractPersistable;

@DeepPlanningClone
public class Ride extends AbstractPersistable {

    protected Visit pickupVisit;
    protected Visit deliveryVisit;

    protected int size;

    public Visit getPickupVisit() {
        return pickupVisit;
    }

    public void setPickupVisit(Visit pickupVisit) {
        this.pickupVisit = pickupVisit;
    }

    public Visit getDeliveryVisit() {
        return deliveryVisit;
    }

    public void setDeliveryVisit(Visit deliveryVisit) {
        this.deliveryVisit = deliveryVisit;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
