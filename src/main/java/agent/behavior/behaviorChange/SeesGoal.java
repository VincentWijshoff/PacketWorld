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
import util.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;
import static agent.behavior.basic.Basic.getBestNextMove;

public class SeesGoal extends BehaviorChange {

    private boolean seesPacket, remembersDestination;
    public SeesGoal() {
        this.seesPacket = false;
    }

    @Override
    public void updateChange() {
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket() && getAgentState().getBatteryState() > 20;
        this.remembersDestination = getAgentState().hasCarry() && Memory.knowsDestOf(getAgentState(), getAgentState().getCarry().get().getColor());

        if (seesPacket) {
            List<CellPerception> closePackets = findOfType(PacketRep.class, getAgentState());
            for (CellPerception perc : closePackets) {
                Color packetColor = perc.getRepOfType(PacketRep.class).getColor();
                //if reachable
                Pair<int[], ArrayList<Basic.Node>> result = getBestNextMove(getAgentState().getX(), getAgentState().getY(), perc.getX(), perc.getY(), getAgentState(), false);
                boolean reachable = result.getFirst() != null && result.getSecond().size() == 0;
                if (Memory.knowsDestOf(getAgentState(), packetColor) && reachable &&
                        (getAgentState().getColor().isEmpty() || getAgentState().getColor().get() == packetColor)) {
                    Memory.setTarget(getAgentState(), perc);
                    seesPacket = true;
                    break;
                }
                else seesPacket = false;
            }
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
        return this.seesPacket || this.remembersDestination;
    }
}
