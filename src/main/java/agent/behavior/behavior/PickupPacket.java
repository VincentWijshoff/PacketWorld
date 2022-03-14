package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.world.packet.PacketRep;

import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class PickupPacket extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        List<CellPerception> closePackets = findOfType(PacketRep.class, agentState);
        CellPerception closestPacket = closePackets.stream().findFirst().orElse(null);
        agentAction.pickPacket(closestPacket.getX(), closestPacket.getY());
    }
}
