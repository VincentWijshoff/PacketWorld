package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Basic;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.Perception;
import environment.world.destination.DestinationRep;
import environment.world.energystation.EnergyStationRep;
import environment.world.flag.FlagRep;
import environment.world.packet.PacketRep;
import util.MyColor;

import java.awt.*;
import java.util.ArrayList;
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
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket() && getAgentState().getBatteryState() > 20;
        this.seesDestination = getAgentState().hasCarry() && getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
        this.remembersDestination = getAgentState().hasCarry() && Memory.knowsDestOf(getAgentState(), getAgentState().getCarry().get().getColor());

        if (seesPacket) {
            List<CellPerception> closePackets = findOfType(PacketRep.class, getAgentState());
            CellPerception closestPacket = closePackets.get(0);
            Memory.setTarget(getAgentState(), closestPacket);
        } else if (seesDestination) {
            Color packetColor = getAgentState().getCarry().get().getColor();
            List<CellPerception> closeDests = findOfType(DestinationRep.class, getAgentState());
            CellPerception closestDest = closeDests.stream()
                    .filter(dest -> dest.containsDestination(packetColor))
                    .findFirst().orElse(null);
            Memory.setTarget(getAgentState(), closestDest);
        } else if (remembersDestination) {
            List<String[]> destLocations = Memory.destinations().getAllStored(getAgentState())
                    .stream()
                    .filter(attributes -> MyColor.getName(getAgentState().getCarry().get().getColor()).equals(attributes[2]))
                    .toList();

            // We need to find the closest destination
            double closestDist = Double.POSITIVE_INFINITY;
            int[] closestDestLoc = new int[]{};
            for (String[] attr : destLocations) {
                int[] loc = new int[]{ Integer.parseInt(attr[0]), Integer.parseInt(attr[1]) };
                double dist = Math.abs(getAgentState().getX() - loc[0]) + Math.abs(getAgentState().getY() - loc[1]);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestDestLoc = loc;
                }
            }
            // and save the shortest distance in memory
            Memory.setTarget(getAgentState(), closestDestLoc);
        }
    }



    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket || this.remembersDestination;
    }
}
