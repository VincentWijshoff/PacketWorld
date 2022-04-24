package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import agent.behavior.basic.Memory;

import static agent.behavior.basic.Basic.communicateInfo;

public class UnblockDestination extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        communicateInfo(agentState, agentCommunication);

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        agentAction.skip();
        String target = agentState.getMemoryFragment(Memory.MemKey.TARGET.toString());
        //TODO implement
        // Agent should move to target
        // Pick up packet at target
        // 'Move away' (<- how do we choose this?)
        // Then drop packet somewhere random
    }
}
