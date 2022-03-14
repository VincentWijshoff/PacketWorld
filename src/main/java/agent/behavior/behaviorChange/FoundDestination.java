package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class FoundDestination extends BehaviorChange {

    private boolean foundDestination;

    public FoundDestination(){
        this.foundDestination = false;
    }

    @Override
    public void updateChange() {
        this.foundDestination = getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
    }

    @Override
    public boolean isSatisfied() {
        return this.foundDestination;
    }
}
