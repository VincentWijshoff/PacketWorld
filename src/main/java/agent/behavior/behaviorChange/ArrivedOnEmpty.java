package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Memory;

import java.util.Objects;

public class ArrivedOnEmpty extends BehaviorChange {

    private boolean arrived;
    private boolean isChargingStation;

    public ArrivedOnEmpty(){
        arrived = false;
        isChargingStation = false;
    }


    @Override
    public void updateChange() {
        int[] target = Objects.requireNonNull(Memory.getTarget(getAgentState()));
        this.arrived = target[0] == getAgentState().getX() && target[1] == getAgentState().getY();
        this.isChargingStation = getAgentState().getPerception().getCellPerceptionOnAbsPos(getAgentState().getX(), getAgentState().getY() + 1).containsEnergyStation();
    }

    @Override
    public boolean isSatisfied() {
        return arrived && !isChargingStation;
    }
}
