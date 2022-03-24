package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import environment.CellPerception;
import environment.world.destination.DestinationRep;
import environment.world.packet.PacketRep;

import java.awt.*;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class SeesGoal extends BehaviorChange {

    private boolean seesDestination, seesPacket, remembersDestination;

    public SeesGoal() {
        this.seesPacket = false;
        this.seesDestination = false;
    }

    @Override
    public void updateChange() {
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket();
        this.seesDestination = getAgentState().hasCarry() && getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
        this.remembersDestination = getAgentState().hasCarry() && getAgentState().getMemoryFragment(getAgentState().getCarry().get().getColor().toString()) != null;
        if (seesPacket) {
            List<CellPerception> closePackets = findOfType(PacketRep.class, getAgentState());
            CellPerception closestPacket = closePackets.get(0);
            getAgentState().addMemoryFragment("x", Integer.toString(closestPacket.getX()));
            getAgentState().addMemoryFragment("y", Integer.toString(closestPacket.getY()));
        } else if (seesDestination) {
            Color packetColor = getAgentState().getCarry().get().getColor();
            List<CellPerception> closeDests = findOfType(DestinationRep.class, getAgentState());
            CellPerception closestDest = closeDests.stream()
                    .filter(dest -> dest.containsDestination(packetColor))
                    .findFirst().orElse(null);
            getAgentState().addMemoryFragment("x", Integer.toString(closestDest.getX()));
            getAgentState().addMemoryFragment("y", Integer.toString(closestDest.getY()));
        } else if (remembersDestination) {
            String mem = getAgentState().getMemoryFragment(getAgentState().getCarry().get().getColor().toString());
            // this is a list of locations looking like this:
            // x1;y1-x2;y2-x3;y3
            // we need to find the closest destination
            String[] locations = mem.split("-");
            double closestDist = Double.POSITIVE_INFINITY;
            String[] closestPoint = new String[0];
            for (String location : locations) {
                String[] point = location.split(";");
                double dist = Math.abs(getAgentState().getX() - Integer.parseInt(point[0])) +
                        Math.abs(getAgentState().getY() - Integer.parseInt(point[1]));
                if (dist < closestDist) {
                    closestDist = dist;
                    closestPoint = point;
                }
            }
            // and save the shortest distance in memory
            getAgentState().addMemoryFragment("x", closestPoint[0]);
            getAgentState().addMemoryFragment("y", closestPoint[1]);
        }
    }

    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket || this.remembersDestination;
    }
}
