package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import agent.behavior.basic.Memory;

import java.util.Objects;

import static agent.behavior.basic.Basic.communicateInfo;

public class ReleasePacket extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        communicateInfo(agentState, agentCommunication);
    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        this.releasePacket(agentState, agentAction);
    }

    private void releasePacket(AgentState agentState, AgentAction agentAction) {
        int[] target = Objects.requireNonNull(Memory.getTarget(agentState));
        agentAction.putPacket(target[0], target[1]);
    }
}
