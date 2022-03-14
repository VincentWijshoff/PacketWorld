package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class CanPickup extends BehaviorChange {

    private int x, y, agentX, agentY;

    public CanPickup(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public void updateChange() {
        this.agentX = getAgentState().getX();
        this.agentY = getAgentState().getY();
    }

    @Override
    public boolean isSatisfied() {
        return Math.abs(this.agentX - this.x) <=1 && Math.abs(this.agentY - this.y) <= 1;
    }
}
