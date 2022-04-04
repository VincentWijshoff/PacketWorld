package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class CanCharge extends BehaviorChange {

    private int x, y, agentX, agentY;

    public CanCharge(){
    }

    @Override
    public void updateChange() {
        this.agentX = getAgentState().getX();
        this.agentY = getAgentState().getY();
        this.x = Integer.parseInt(getAgentState().getMemoryFragment("x"));
        this.y = Integer.parseInt(getAgentState().getMemoryFragment("y"));
    }

    @Override
    public boolean isSatisfied() {
        return getAgentState().getBatteryState() <= 20 &&
                Math.abs(this.agentX - this.x) <= 1 &&
                Math.abs(this.agentY - this.y) <= 1;
    }
}
