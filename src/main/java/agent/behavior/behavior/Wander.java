package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import agent.behavior.basic.Basic;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.Coordinate;
import environment.Perception;
import environment.world.destination.DestinationRep;
import environment.world.wall.WallRep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static agent.behavior.basic.Basic.*;

public class Wander extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        communicateInfo(agentState, agentCommunication);
    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        if (optimization2) {
            storeView(agentState);
        }

        this.walkRandom(agentState, agentAction);
    }

    private void walkRandom(AgentState agentState, AgentAction agentAction) {
        // Potential moves an agent can make (radius of 1 around the agent)
        List<Coordinate> moves = new ArrayList<>(List.of(
                new Coordinate(1, 1), new Coordinate(-1, -1),
                new Coordinate(1, 0), new Coordinate(-1, 0),
                new Coordinate(0, 1), new Coordinate(0, -1),
                new Coordinate(1, -1), new Coordinate(-1, 1)
        ));

        dropPacketIfDying(agentState, agentAction);

        // Shuffle moves randomly
        Collections.shuffle(moves);


        if (optimization1) {
            //if rightmost cell is further away than left most -> move right
            //the idea is that this maximizes the perception -> packets/destinations should be discovered quicker
            //NOTE: it is important to prioritise moves by swapping with counterpart, not by placing the move first. This causes the agent to be stuck in places such as 'tight hallways'. TODO: draw diagram of this scenario
            Perception pc = agentState.getPerception();
            if (pc.getWidth() - pc.getSelfX() - 1 > pc.getSelfX()) {
                prioritizeC1(moves, new Coordinate(1, 0), new Coordinate(-1, 0));
            }
            if (pc.getWidth() - pc.getSelfX() - 1 < pc.getSelfX()) {
                prioritizeC1(moves, new Coordinate(-1, 0), new Coordinate(1, 0));

            }
            if (pc.getHeight() - pc.getSelfY() - 1 > pc.getSelfY()) {
                prioritizeC1(moves, new Coordinate(0, 1), new Coordinate(0, -1));

            }
            if (pc.getHeight() - pc.getSelfY() - 1 < pc.getSelfY()) {
                prioritizeC1(moves, new Coordinate(0, -1), new Coordinate(0, 1));
            }
        }

        if (optimization3) {
            moves = recentlyVisited(moves, agentState);
        }




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
    }

    private void prioritizeC1(List<Coordinate> moves, Coordinate c1, Coordinate c2) {
        if (moves.indexOf(c1) > moves.indexOf(c2)) {
            Collections.swap(moves, moves.indexOf(c1), moves.indexOf(c2));
        }
    }

    private List<Coordinate> recentlyVisited(List<Coordinate> moves, AgentState agentState) {
        //remove recently visited from moves
        if (!Memory.recentVisits().isEmpty(agentState)) {
            for (int[] prevVisited : Memory.recentVisits().getAllStoredPos(agentState)) {
                Coordinate diff = new Coordinate(prevVisited[0], prevVisited[1]).diff(new Coordinate(agentState.getX(), agentState.getY()));
                if (moves.contains(diff)) { //dont just remove moves, this can cause an agent to get stuck
                    moves.remove(diff);
                    moves.add(moves.size(), diff);
                }
            }
        }

        int nbLocationsToRemember = 4;
        List<String[]> prevRecentlyVisitedList = Memory.recentVisits().getAllStored(agentState);

        //store current location to recentlyvisited
        if (prevRecentlyVisitedList.size() >= nbLocationsToRemember) prevRecentlyVisitedList.remove(0);
        prevRecentlyVisitedList.add(new String[]{ agentState.getX() + "", agentState.getY() + "" });
        Memory.recentVisits().setAllStored(agentState, prevRecentlyVisitedList);
        return moves;
    }
}
