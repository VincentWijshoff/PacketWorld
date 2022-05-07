package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Memory;

public class HasUnblocked extends BehaviorChange {

    boolean done = false;

    @Override
    public void updateChange() {
        int[] target = Memory.getTarget(getAgentState());
        if ((target[0] == -1 && target[1] == -1)) {
            done = true;
            Memory.setTarget(getAgentState(), new int[] {0,0});
        }
        else done = false;

    }

    @Override
    public boolean isSatisfied() {
        return done;
    }
}
