package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.world.packet.PacketRep;

import java.util.List;

import static agent.behavior.basic.Basic.communicateInfo;
import static agent.behavior.basic.Basic.findOfType;

public class PickupPacket extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        communicateInfo(agentState, agentCommunication);
    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        agentAction.pickPacket(Integer.parseInt(agentState.getMemoryFragment("x")),
                Integer.parseInt(agentState.getMemoryFragment("y")));
    }
}
