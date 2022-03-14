package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class SeesGoal extends BehaviorChange {

    private boolean seesDestination, seesPacket;

    public SeesGoal(){
        this.seesPacket = false;
        this.seesDestination = false;
    }

    @Override
    public void updateChange() {
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket();
        this.seesDestination = getAgentState().hasCarry() && getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
    }

    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket;
    }
}
