package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;

public class FoundPacket extends BehaviorChange {

    private boolean foundPacket;

    public  FoundPacket(){
        this.foundPacket = false;
    }

    @Override
    public void updateChange() {
        this.foundPacket = getAgentState().seesPacket();
    }

    @Override
    public boolean isSatisfied() {
        return this.foundPacket;
    }
}
