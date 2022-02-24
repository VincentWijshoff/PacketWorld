package agent.behavior.wander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.Coordinate;
import environment.Perception;

public class Wander extends Behavior {

    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        // No communication
    }

    
    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        if(agentState.hasCarry()){
            findDestination(agentState, agentAction);
        }
        findPacket(agentState, agentAction);
    }

    private void findDestination(AgentState agentState, AgentAction agentAction){
        if(!agentState.seesDestination()){
            // cannot see any destination, so walk in a random direction in the hopes of finding one next move
            walkRandom(agentState, agentAction);
        }
        // placeholder, something always has to happen
        walkRandom(agentState, agentAction);
    }

    private void findPacket(AgentState agentState, AgentAction agentAction){
        if(!agentState.seesPacket()){
            // cannot see any packet, so walk in a random direction in the hopes of finding one on the next move
            walkRandom(agentState, agentAction);
        }
        // locate the closest packet
            // first check the neighbours
        for(CellPerception p : agentState.getPerception().getNeighbours()){
            if(p == null) continue;
            if(p.containsPacket()){
                // pickup the packet
                agentAction.pickPacket(p.getX(), p.getY());
                return;
            }
        }
            // no packet in the neighbouring area, search rest of the perception of the agent
            // get all packets in a list
        CellPerception[][] fullArea = getViewArea(agentState);
        ArrayList<CellPerception> packetList = new ArrayList<>();
        for (int i = 0; i < fullArea.length; i++) {
            for (int j = 0; j < fullArea[0].length; j++) {
                if(fullArea[i][j] != null && fullArea[i][j].containsPacket()){
                    packetList.add(fullArea[i][j]);
                }
            }
        }

            // if no packets, walk randomly
        if(packetList.size() == 0){
            walkRandom(agentState, agentAction);
            return;
        }

            // now get the packet closest to the agent
        double closestDist = Double.POSITIVE_INFINITY;
        CellPerception closestPacket = null;
        for (CellPerception cell : packetList) {
            if(Perception.distance(agentState.getX(), agentState.getY(), cell.getX(), cell.getY()) < closestDist){
                closestDist = Perception.distance(agentState.getX(), agentState.getY(), cell.getX(), cell.getY());
                closestPacket = cell;
            }
        }
            // now we determine the move to get to the packet as fast as possible
        moveTo(closestPacket.getX(), closestPacket.getY(), agentState, agentAction);
    }

    private CellPerception[][] getViewArea(AgentState agentState){
        CellPerception[][] perceptionList = new CellPerception[agentState.getPerception().getWidth()][agentState.getPerception().getHeight()];
        int width = Math.floorDiv(agentState.getPerception().getWidth(), 2);
        int height = Math.floorDiv(agentState.getPerception().getHeight(), 2);
        // TODO shouldnt all views be even?
        for(int i = -width; i<width ; i++){
            for(int j = -height; j<height ; j++){
                perceptionList[i+width][j+height] = agentState.getPerception().getCellPerceptionOnRelPos(i, j);
            }
        }
        return perceptionList;
    }

    private void moveTo(int i, int j, AgentState agentState, AgentAction agentAction){
        System.out.println("X: " + i);
        System.out.println("Y: " + j);
        // TODO make move to most optimal position to get to new position
        walkRandom(agentState, agentAction);
    }

    private void walkRandom(AgentState agentState, AgentAction agentAction){
        // Potential moves an agent can make (radius of 1 around the agent)
        List<Coordinate> moves = new ArrayList<>(List.of(
                new Coordinate(1, 1), new Coordinate(-1, -1),
                new Coordinate(1, 0), new Coordinate(-1, 0),
                new Coordinate(0, 1), new Coordinate(0, -1),
                new Coordinate(1, -1), new Coordinate(-1, 1)
        ));

        // Shuffle moves randomly
        Collections.shuffle(moves);

        // Check for viable moves
        for (var move : moves) {
            var perception = agentState.getPerception();
            int x = move.getX();
            int y = move.getY();

            // If the area is null, it is outside the bounds of the environment
            //  (when the agent is at any edge for example some moves are not possible)
            if (perception.getCellPerceptionOnRelPos(x, y) != null && perception.getCellPerceptionOnRelPos(x, y).isWalkable()) {
                agentAction.step(agentState.getX() + x, agentState.getY() + y);
                return;
            }
        }

        // No viable moves, skip turn
        agentAction.skip();
    }
}
