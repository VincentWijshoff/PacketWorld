package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Memory;

import java.util.Objects;

public class CanRelease extends BehaviorChange {

    private int x, y, agentX, agentY;

    public CanRelease(){
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
        // Distance 1 away form destination
        if (getAgentState().getPerception().getCellPerceptionOnAbsPos(this.x, this.y) == null) return false;
        return getAgentState().hasCarry() &&
                getAgentState().getPerception().getCellPerceptionOnAbsPos(this.x, this.y)
                        .containsDestination(getAgentState().getCarry().get().getColor()) &&
                Math.abs(this.agentX - this.x) <= 1 &&
                Math.abs(this.agentY - this.y) <= 1;
    }
}
