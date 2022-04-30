package agent.behavior.behaviorChange;

import agent.behavior.BehaviorChange;
import agent.behavior.basic.Basic;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.world.packet.PacketRep;
import util.MyColor;
import util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static agent.behavior.basic.Basic.getBestNextMove;
import static agent.behavior.basic.Basic.getViewArea;

public class CanUnblock extends BehaviorChange {

    boolean canUnblock = false;

    @Override
    public void updateChange() {
        if (!getAgentState().hasCarry()) {

            List<String[]> clearedPackets = Memory.clearedPackets().getAllStored(getAgentState());
            for (String[] blockingPacket : Memory.blockingPackets().getAllStored(getAgentState())) {
                //if packet of correct color in memory, and not in clearedPackets
                if ( blockingPacket[2].equals(MyColor.getName(getAgentState().getColor().get())) && !clearedPackets.contains(blockingPacket)) {
                    Memory.setTarget(getAgentState(), new CellPerception(Integer.parseInt(blockingPacket[0]), Integer.parseInt(blockingPacket[1])));
                    canUnblock = true;
                    return;
                }
            }


            //scan perception for blocked destinations
            ArrayList<Pair<int[], ArrayList<Basic.Node>>> results = new ArrayList<>();
            for (CellPerception[] percs: getViewArea(getAgentState())) {
                for (CellPerception perc: percs) {
                    if (perc != null && perc.containsAnyDestination()) {
                        results.add(getBestNextMove(getAgentState().getX(), getAgentState().getY(), perc.getX(), perc.getY(), getAgentState(), false));
                    }
                }
            }

            for (Pair<int[], ArrayList<Basic.Node>> result : results) {
                boolean reachable = result.getFirst() != null && result.getSecond().size() == 0;
                ArrayList<Basic.Node> packets = result.getSecond();
                if (!reachable) {
                    //check if blocking packet is correct color
                    for (Basic.Node packet: packets) {
                        CellPerception cell = getAgentState().getPerception().getCellPerceptionOnAbsPos(packet.x, packet.y);
                        PacketRep packetRep = cell.getRepOfType(PacketRep.class);
                            //check if blocking packet is reachable
                            Pair<int[], ArrayList<Basic.Node>> resultPacket = getBestNextMove(getAgentState().getX(), getAgentState().getY(), packet.x, packet.y, getAgentState(), false);
                            if (resultPacket.getFirst() != null && resultPacket.getSecond().size() == 0) {
                                if (getAgentState().getColor().isEmpty() || getAgentState().getColor().get() == packetRep.getColor()) {
                                    //enter Unblock state, with target packet location
                                    Memory.setTarget(getAgentState(), new CellPerception(packet.x, packet.y));
                                    canUnblock = true;
                                    return;
                                }
                                else {
                                    //store blocking packet in memory
                                    Memory.blockingPackets().add(getAgentState(),cell);
                                }
                            }
                    }
                }
            }
        }
        canUnblock = false;
    }

    @Override
    public boolean isSatisfied() {
        return canUnblock;
    }
}
