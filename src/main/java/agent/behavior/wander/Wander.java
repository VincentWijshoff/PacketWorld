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
            return;
        }
        findPacket(agentState, agentAction);
    }

    private void findDestination(AgentState agentState, AgentAction agentAction){
        if(!agentState.seesDestination(agentState.getCarry().get().getColor())){
            // cannot see any destination, so walk in a random direction in the hopes of finding one next move
            walkRandom(agentState, agentAction);
            return;
        }
        // locate the closest destination
        // first check the neighbours
        for(CellPerception p : agentState.getPerception().getNeighbours()){
            if(p == null) continue;
            if(p.containsDestination(agentState.getCarry().get().getColor())){
                // drop the packet
                agentAction.putPacket(p.getX(), p.getY());
                return;
            }
        }
        // no destination in the neighbouring area, search rest of the perception of the agent
        // get the closest destination by going through all destinations in view
        CellPerception[][] fullArea = getViewArea(agentState);
        double closestDist = Double.POSITIVE_INFINITY;
        CellPerception closestDest = null;
        for (int i = 0; i < fullArea.length; i++) {
            for (int j = 0; j < fullArea[0].length; j++) {
                if(fullArea[i][j] != null && fullArea[i][j].containsDestination(agentState.getCarry().get().getColor())){
                    if(Perception.distance(agentState.getX(), agentState.getY(), fullArea[i][j].getX(), fullArea[i][j].getY()) < closestDist){
                        closestDist = Perception.distance(agentState.getX(), agentState.getY(), fullArea[i][j].getX(), fullArea[i][j].getY());
                        closestDest = fullArea[i][j];
                    }
                }
            }
        }

        // if no packets, walk randomly
        if(closestDest == null){
            walkRandom(agentState, agentAction);
            return;
        }

        // now we determine the move to get to the destination as fast as possible
        moveTo(closestDest.getX(), closestDest.getY(), agentState, agentAction);
    }

    private void findPacket(AgentState agentState, AgentAction agentAction){
        if(!agentState.seesPacket()){
            // cannot see any packet, so walk in a random direction in the hopes of finding one on the next move
            walkRandom(agentState, agentAction);
            return;
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
            // get the closest packet by going through all packets in view
        CellPerception[][] fullArea = getViewArea(agentState);
        double closestDist = Double.POSITIVE_INFINITY;
        CellPerception closestPacket = null;
        for (int i = 0; i < fullArea.length; i++) {
            for (int j = 0; j < fullArea[0].length; j++) {
                if(fullArea[i][j] != null && fullArea[i][j].containsPacket()){
                    if(Perception.distance(agentState.getX(), agentState.getY(), fullArea[i][j].getX(), fullArea[i][j].getY()) < closestDist){
                        closestDist = Perception.distance(agentState.getX(), agentState.getY(), fullArea[i][j].getX(), fullArea[i][j].getY());
                        closestPacket = fullArea[i][j];
                    }
                }
            }
        }

            // if no packets, walk randomly
        if(closestPacket == null){
            walkRandom(agentState, agentAction);
            return;
        }

            // now we determine the move to get to the packet as fast as possible
        moveTo(closestPacket.getX(), closestPacket.getY(), agentState, agentAction);
    }

    private CellPerception[][] getViewArea(AgentState agentState){
        CellPerception[][] perceptionList = new CellPerception[agentState.getPerception().getWidth()][agentState.getPerception().getHeight()];
        int left = agentState.getPerception().getOffsetX() - agentState.getX();
        int top = agentState.getPerception().getOffsetY() - agentState.getY();
        int right = agentState.getPerception().getOffsetX() + agentState.getPerception().getWidth() - agentState.getX();
        int bottom = agentState.getPerception().getOffsetY() + agentState.getPerception().getHeight() - agentState.getY();
        for(int i = left; i<right ; i++){
            for(int j = top; j<bottom ; j++){
                perceptionList[i-left][j-top] = agentState.getPerception().getCellPerceptionOnRelPos(i, j);
            }
        }
        return perceptionList;
    }

    private void moveTo(int i, int j, AgentState agentState, AgentAction agentAction){
        // TODO make an optimal path (checking obstacles and stuff) ideally generate path to all packets in sight and take shortest
        if(i == agentState.getX()){
            step(i,  j-agentState.getY() > 0 ? agentState.getY()+1 : agentState.getY()-1, agentState, agentAction);
            return;
        }
        if(j == agentState.getY()){
            step(i-agentState.getX() > 0 ? agentState.getX()+1 : agentState.getX()-1, j, agentState, agentAction);
            return;
        }
        step(i-agentState.getX() > 0 ? agentState.getX()+1 : agentState.getX()-1,
                j-agentState.getY() > 0 ? agentState.getY()+1 : agentState.getY()-1, agentState, agentAction);
    }

    private void step(int x, int y, AgentState agentState, AgentAction agentAction){
        var perception = agentState.getPerception();
        if (perception.getCellPerceptionOnAbsPos(x, y) != null && perception.getCellPerceptionOnAbsPos(x, y).isWalkable()) {
            agentAction.step(x, y);
            return;
        }
        walkRandom(agentState, agentAction);
    }

    private void walkRandom(AgentState agentState, AgentAction agentAction){
        // NOTE: do not use the local step() function here, it will have infinite loops
        // we want to first try to move away from walls, after that a random walk should happen
        int left = agentState.getPerception().getOffsetX() - agentState.getX();
        int top = agentState.getPerception().getOffsetY() - agentState.getY();
        int right = agentState.getPerception().getOffsetX() + agentState.getPerception().getWidth() - agentState.getX()-1;
        int bottom = agentState.getPerception().getOffsetY() + agentState.getPerception().getHeight() - agentState.getY()-1;
        int xStep = 0;
        int yStep = 0;
        // check if against left or right wall
        if(left*-1 < right){ // left wall
            xStep += 1;
        }else if(left*-1 > right){ // right wall
            xStep -= 1;
        }
        // check if against upper or lower wall
        if(top*-1 < bottom){ // left wall
            yStep += 1;
        }else if(top*-1 > bottom){ // right wall
            yStep -= 1;
        }
        // now move in the direction, else move randomly
        if(xStep != 0 || yStep!= 0){
            var perception = agentState.getPerception();
            if (perception.getCellPerceptionOnRelPos(xStep, yStep) != null && perception.getCellPerceptionOnRelPos(xStep, yStep).isWalkable()) {
                agentAction.step(agentState.getX() + xStep, agentState.getY() + yStep);
                return;
            }
        }
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
