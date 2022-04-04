package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Basic;
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
        this.remembersDestination = getAgentState().hasCarry() && getAgentState().getMemoryFragment(getAgentState().getCarry().get().getColor().toString()) != null;
        this.seesCharger = getAgentState().getBatteryState() < 20
                && (!findOfType(EnergyStationRep.class, getAgentState()).isEmpty() || getAgentState().getMemoryFragment("chargers") != null);
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
        } else if (seesCharger){
            // needs charger and sees one
            if(!findOfType(EnergyStationRep.class, getAgentState()).isEmpty()){
                // sees one (sorted list)
                List<CellPerception> destlist = findOfType(EnergyStationRep.class, getAgentState());
                getAgentState().addMemoryFragment("x", Integer.toString(destlist.get(0).getX()));
                getAgentState().addMemoryFragment("y", Integer.toString(destlist.get(0).getY()));
            }else{
                // remembers one
                String[] clst = getAgentState().getMemoryFragment("chargers").split("-");
                List<Basic.Node> chargerlist = new ArrayList<>();
                for (String s : clst) {
                    chargerlist.add(new Basic.Node(Integer.parseInt(s.split(";")[0]),
                            Integer.parseInt(s.split(";")[0])));
                }
                // find closest one
                chargerlist.sort((p1, p2) -> {
                    double d1 = Perception.distance(getAgentState().getX(), getAgentState().getY(), p1.x, p1.y);
                    double d2 = Perception.distance(getAgentState().getX(), getAgentState().getY(), p2.x, p2.y);
                    return Double.compare(d1, d2);
                });
                getAgentState().addMemoryFragment("x", Integer.toString(chargerlist.get(0).x));
                getAgentState().addMemoryFragment("y", Integer.toString(chargerlist.get(0).y));
            }
        }
    }

    @Override
    public boolean isSatisfied() {
        return this.seesDestination || this.seesPacket || this.remembersDestination || this.seesCharger;
    }
}
