package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class NoGoal extends BehaviorChange {

    private boolean noDestination, noPacket;

    public NoGoal(){
        this.noDestination = false;
        this.noPacket = false;
    }

    @Override
    public void updateChange() {
        this.noDestination = getAgentState().hasCarry() && !getAgentState().seesDestination();
        this.noPacket = !getAgentState().hasCarry() && !getAgentState().seesPacket();
    }

    @Override
    public boolean isSatisfied() {
        return this.noDestination || this.noPacket;
    }
}
