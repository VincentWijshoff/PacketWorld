package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Memory;
import environment.world.packet.PacketRep;

import java.util.Objects;

import static agent.behavior.basic.Basic.findOfType;

public class NoGoal extends BehaviorChange {

    @Override
    public void updateChange() {
        return;
    }

    @Override
    public boolean isSatisfied() {
        return true;
    }
}
