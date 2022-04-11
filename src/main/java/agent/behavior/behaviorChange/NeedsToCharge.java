package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Basic;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.Perception;
import environment.world.energystation.EnergyStationRep;

import java.util.ArrayList;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class NeedsToCharge extends BehaviorChange {

    private boolean lowBattery, seesCharger;

    public NeedsToCharge() {
        this.lowBattery = false;
        this.seesCharger = false;

    }

    @Override
    public void updateChange() {
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
    }

    private boolean lowBattery(int x, int y) {
        double dist = Perception.manhattanDistance(getAgentState().getX(), getAgentState().getY(), x, y);
        double cost = dist*10*1.25;
        if (getAgentState().hasCarry()) cost *= 2; // carrying a packet costs 20 energy per step instead of 10
        // cost is the (approximate) minimal cost to get to the charger
        // we set an upper value to create an interval
        double maxCost = cost * (Math.random()*0.75+1);// random number between 1 and 2
        if (getAgentState().getBatteryState() <= maxCost) {
            System.out.println(getAgentState().getName() + ": wants to recharge with battery: " + getAgentState().getBatteryState() + " & distance: " + dist);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSatisfied() {
        return this.seesCharger && this.lowBattery;
    }
}
