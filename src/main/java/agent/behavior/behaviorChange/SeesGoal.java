package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import environment.CellPerception;
import environment.world.destination.DestinationRep;
import environment.world.packet.PacketRep;

import java.awt.*;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class SeesGoal extends BehaviorChange {

    private boolean seesDestination, seesPacket;

    public SeesGoal(){
        this.seesPacket = false;
        this.seesDestination = false;
    }

    @Override
    public void updateChange() {
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket();
        this.seesDestination = getAgentState().hasCarry() && getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
        if(seesPacket){
            List<CellPerception> closePackets = findOfType(PacketRep.class, getAgentState());
            CellPerception closestPacket = closePackets.stream().findFirst().orElse(null);
            getAgentState().addMemoryFragment("x", Integer.toString(closestPacket.getX()));
            getAgentState().addMemoryFragment("y", Integer.toString(closestPacket.getY()));
        }else if(seesDestination){
            Color packetColor = getAgentState().getCarry().get().getColor();
            List<CellPerception> closeDests = findOfType(DestinationRep.class, getAgentState());
            CellPerception closestDest = closeDests.stream()
                    .filter(dest -> dest.containsDestination(packetColor))
                    .findFirst().orElse(null);
            getAgentState().addMemoryFragment("x", Integer.toString(closestDest.getX()));
            getAgentState().addMemoryFragment("y", Integer.toString(closestDest.getY()));
        }
    }

    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket;
    }
}
