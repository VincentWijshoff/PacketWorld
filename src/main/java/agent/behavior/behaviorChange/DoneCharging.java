package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class DoneCharging  extends BehaviorChange {

    public DoneCharging(){}

    int batteryLife;

    @Override
    public void updateChange() {
        this.batteryLife = getAgentState().getBatteryState();
    }

    @Override
    public boolean isSatisfied() {
        return this.batteryLife >= 100 ;
    }
}
