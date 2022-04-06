package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Basic;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.Perception;
import environment.world.destination.DestinationRep;
import environment.world.energystation.EnergyStationRep;
import environment.world.packet.PacketRep;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class SeesGoal extends BehaviorChange {

    private boolean seesDestination, seesPacket, remembersDestination, seesCharger;

    public SeesGoal() {
        this.seesPacket = false;
        this.seesDestination = false;
    }

    @Override
    public void updateChange() {
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket();
        this.seesDestination = getAgentState().hasCarry() && getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
        this.remembersDestination = getAgentState().hasCarry() && Memory.knowsDestOf(getAgentState(), getAgentState().getCarry().get().getColor());
        this.seesCharger = getAgentState().getBatteryState() < 20
                && (!findOfType(EnergyStationRep.class, getAgentState()).isEmpty() || (!Memory.chargers().isEmpty(getAgentState())));
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
            String mem = getAgentState().getMemoryFragment(getAgentState().getCarry().get().getColor().toString());
            List<int[]> destLocations = Memory.destinations().getAllStoredPos(getAgentState());

            // We need to find the closest destination
            double closestDist = Double.POSITIVE_INFINITY;
            int[] closestDestLoc = new int[]{};
            for (int[] loc : destLocations) {
                double dist = Math.abs(getAgentState().getX() - loc[0]) + Math.abs(getAgentState().getY() - loc[1]);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestDestLoc = loc;
                }
            }
            // and save the shortest distance in memory
            Memory.setTarget(getAgentState(), closestDestLoc);
        } else if (seesCharger){
            // needs charger and sees one
            if(!findOfType(EnergyStationRep.class, getAgentState()).isEmpty()){
                // sees one (sorted list)
                List<CellPerception> destlist = findOfType(EnergyStationRep.class, getAgentState());
                Memory.setTarget(getAgentState(), destlist.get(0));
            }else{
                // remembers one
                List<Basic.Node> chargerlist = new ArrayList<>();
                List<int[]> storedChargers = Memory.chargers().getAllStoredPos(getAgentState());
                for (int[] pos : storedChargers) {
                    chargerlist.add(new Basic.Node(pos[0], pos[1]));
                }
                // find closest one
                chargerlist.sort((p1, p2) -> {
                    double d1 = Perception.distance(getAgentState().getX(), getAgentState().getY(), p1.x, p1.y);
                    double d2 = Perception.distance(getAgentState().getX(), getAgentState().getY(), p2.x, p2.y);
                    return Double.compare(d1, d2);
                });
                Memory.setTarget(getAgentState(), new int[]{ chargerlist.get(0).x, chargerlist.get(0).y });
            }
        }
    }

    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket || this.remembersDestination || this.seesCharger;
    }
}
