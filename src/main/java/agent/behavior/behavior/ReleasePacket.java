package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.world.destination.DestinationRep;

import java.awt.*;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class ReleasePacket extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        this.releasePacket(agentState, agentAction);
    }

    private void releasePacket(AgentState agentState, AgentAction agentAction){
        Color packetColor = agentState.getCarry().get().getColor();
        List<CellPerception> closeDests = findOfType(DestinationRep.class, agentState);
        // Get the closest destination with the correct color
        CellPerception closestDest = closeDests.stream()
                .filter(dest -> dest.containsDestination(packetColor))
                .findFirst().orElse(null);
        agentAction.putPacket(closestDest.getX(), closestDest.getY());
    }

}
