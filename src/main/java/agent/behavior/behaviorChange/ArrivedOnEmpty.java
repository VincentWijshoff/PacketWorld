package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Memory;

import java.util.Objects;

public class ArrivedOnEmpty extends BehaviorChange {

    private boolean arrived;

    public ArrivedOnEmpty(){
        arrived = false;
    }

    @Override
    public void updateChange() {
        int[] target = Objects.requireNonNull(Memory.getTarget(getAgentState()));
        this.arrived = target[0] == getAgentState().getX() && target[1] == getAgentState().getY();
    }

    @Override
    public boolean isSatisfied() {
        return arrived;
    }
}
