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
        agentAction.putPacket(Integer.parseInt(agentState.getMemoryFragment("x")),
                Integer.parseInt(agentState.getMemoryFragment("y")));
    }
}
