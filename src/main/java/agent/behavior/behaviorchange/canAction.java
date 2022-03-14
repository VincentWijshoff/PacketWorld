package agent.behavior.behaviorchange;

import agent.behavior.BehaviorChange;

public class canAction extends BehaviorChange {
    @Override
    public void updateChange() {

    }

    @Override
    public boolean isSatisfied() {
        return getAgentState().hasCarry();
    }
}
