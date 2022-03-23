package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class ArrivedOnEmpty extends BehaviorChange {

    private boolean arrived;

    public ArrivedOnEmpty(){
        arrived = false;
    }

    @Override
    public void updateChange() {
        this.arrived = Integer.parseInt(getAgentState().getMemoryFragment("x")) == getAgentState().getX()
                    && Integer.parseInt(getAgentState().getMemoryFragment("y")) == getAgentState().getY();
    }

    @Override
    public boolean isSatisfied() {
        return arrived;
    }
}
