package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static agent.behavior.basic.Basic.*;
import static agent.behavior.basic.Basic.dropPacketIfDying;

public class UnblockDestination extends Behavior {

    private boolean first = false;

    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        communicateInfo(agentState, agentCommunication);

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        if (first) {
            first = false;
            if (agentState.hasCarry()) {
                for (CellPerception neighbour : agentState.getPerception().getNeighbours()) {
                    if (neighbour != null && neighbour.isFree()) {
                        agentAction.putPacket(neighbour.getX(), neighbour.getY());
                        break;
                    }
                }
            }
        }

        if (optimization2) {
            storeView(agentState);
        }

        if(dropPacketIfDying(agentState, agentAction)) return; // only 1 action per move
        int[] target = Objects.requireNonNull(Memory.getTarget(agentState));

        //store current location to recentlyvisited
        List<String[]> prevRecentlyVisitedList = Memory.recentVisits().getAllStored(agentState);
        if (prevRecentlyVisitedList.size() >= 5) prevRecentlyVisitedList.remove(0);
        prevRecentlyVisitedList.add(new String[]{ agentState.getX() + "", agentState.getY() + "" });
        Memory.recentVisits().setAllStored(agentState, prevRecentlyVisitedList);

        // Agent should move to target
        if (!agentState.hasCarry()) {

            // Pick up packet at target
            if (Math.abs(target[0] - agentState.getX()) <= 1 && Math.abs(target[1] - agentState.getY()) <= 1) {
                agentAction.pickPacket(target[0], target[1]);

                CellPerception clearedPacket = agentState.getPerception().getCellPerceptionOnAbsPos(target[0], target[1]);
                Memory.clearedPackets().add(agentState, clearedPacket);

                String[] furthestVisit = prevRecentlyVisitedList.get(0);
                Memory.setTarget(agentState, new int[] {Integer.parseInt(furthestVisit[0]), Integer.parseInt(furthestVisit[1])});
            }
            // Move to target
            else{
                moveTo(agentState, agentAction, target);
            }
        }

        else {
            // 'Move away' (<- how do we choose this?)
            // Then drop packet somewhere random

            if (Math.abs(target[0] - agentState.getX()) <= 1 && Math.abs(target[1] - agentState.getY()) <= 1) {
                if (agentState.getPerception().getCellPerceptionOnAbsPos(target[0], target[1]).isFree()) {
                    agentAction.putPacket(target[0], target[1]);
                }
                else {
                    for (CellPerception neighbour : agentState.getPerception().getNeighbours()) {
                        if (neighbour != null && neighbour.isFree()) {
                            agentAction.putPacket(neighbour.getX(), neighbour.getY());
                            break;
                        }
                    }
                }
                Memory.setTarget(agentState, new int[]{-1, -1}); // This is not a good solution
                first = true;
            }
            else{
                moveTo(agentState, agentAction, target);
            }
        }



    }

}
