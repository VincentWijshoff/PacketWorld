package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Basic;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.Perception;
import environment.world.destination.DestinationRep;
import environment.world.energystation.EnergyStationRep;
import environment.world.packet.PacketRep;
import util.MyColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class SeesGoal extends BehaviorChange {

    private boolean seesDestination, seesPacket, remembersDestination, seesCharger, lowBattery;

    public SeesGoal() {
        this.seesPacket = false;
        this.seesDestination = false;
        this.lowBattery = false;
    }

    @Override
    public void updateChange() {
        this.seesPacket = !getAgentState().hasCarry() && getAgentState().seesPacket();
        this.seesDestination = getAgentState().hasCarry() && getAgentState().seesDestination(getAgentState().getCarry().get().getColor());
        this.remembersDestination = getAgentState().hasCarry() && Memory.knowsDestOf(getAgentState(), getAgentState().getCarry().get().getColor());
        this.seesCharger = (!findOfType(EnergyStationRep.class, getAgentState()).isEmpty() || (!Memory.chargers().isEmpty(getAgentState())));
        if (seesCharger) {
            // needs charger and sees one
            if (!findOfType(EnergyStationRep.class, getAgentState()).isEmpty()) {
                // sees one (sorted list)
                List<CellPerception> destlist = findOfType(EnergyStationRep.class, getAgentState());
                if (lowBattery(destlist.get(0).getX(), destlist.get(0).getY() - 1)) {
                    lowBattery = true;
                    Memory.setTarget(getAgentState(), new int[]{ destlist.get(0).getX(), destlist.get(0).getY() - 1 }); //agent needs to move to space 1 cell above charger to charge.
                } else lowBattery = false;
            } else {
                // remembers one
                List<Basic.Node> chargerlist = new ArrayList<>();
                List<int[]> storedChargers = Memory.chargers().getAllStoredPos(getAgentState());
                for (int[] pos : storedChargers) {
                    chargerlist.add(new Basic.Node(pos[0], pos[1]));
                }

                //agent needs to move to space 1 cell above charger to charge.
                chargerlist = new ArrayList<>(chargerlist.stream().map((node -> {
                    return new Basic.Node(node.x, node.y - 1);
                })).toList());

                // find closest one
                chargerlist.sort((p1, p2) -> {
                    double d1 = Perception.distance(getAgentState().getX(), getAgentState().getY(), p1.x, p1.y);
                    double d2 = Perception.distance(getAgentState().getX(), getAgentState().getY(), p2.x, p2.y);
                    return Double.compare(d1, d2);
                });
                if (lowBattery(chargerlist.get(0).x, chargerlist.get(0).y)) {
                    lowBattery = true;
                    Memory.setTarget(getAgentState(), new int[]{ chargerlist.get(0).x, chargerlist.get(0).y });
                } else lowBattery = false;
            }
        }
        if (!lowBattery) {
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
    }

    private boolean lowBattery(int x, int y) {
        double dist = Perception.manhattanDistance(getAgentState().getX(), getAgentState().getY(), x, y);
        double cost = dist*10*1.5;
        if (getAgentState().hasCarry()) cost *= 2; // carrying a packet costs 20 energy per step instead of 10
        // cost is the (approximate) minimal cost to get to the charger
        // we set an upper value to create an interval
        double maxCost = cost * (Math.random()+1);// random number between 1 and 2
        if (getAgentState().getBatteryState() <= maxCost) {
            System.out.println(getAgentState().getName() + ": wants to recharge with battery: " + getAgentState().getBatteryState() + " & distance: " + dist);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket || this.remembersDestination || (this.seesCharger && this.lowBattery);
    }
}
