package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Memory;

import java.util.Objects;

public class CanCharge extends BehaviorChange {

    private int x, y, agentX, agentY;

    public CanCharge(){
    }

    @Override
    public void updateChange() {
        this.agentX = getAgentState().getX();
        this.agentY = getAgentState().getY();
        int[] target = Objects.requireNonNull(Memory.getTarget(getAgentState()));
        this.x = target[0];
        this.y = target[1];
    }

    @Override
    public boolean isSatisfied() {
        return Math.abs(this.agentX - this.x) == 0 &&
                Math.abs(this.agentY - this.y) == 0;
    }
}
