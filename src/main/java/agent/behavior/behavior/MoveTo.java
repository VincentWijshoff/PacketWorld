package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.Coordinate;
import environment.Perception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static agent.behavior.basic.Basic.*;

public class MoveTo extends Behavior {

    private int x, y;

    public MoveTo(){
    }

    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        // also here store walls and destinations
        if(optimization2){
            storeDestinations(agentState);
        }
        if(optimization4){
            storeWalls(agentState);
        }
        this.x = Integer.parseInt(agentState.getMemoryFragment("x"));
        this.y = Integer.parseInt(agentState.getMemoryFragment("y"));
        this.moveTo(this.x, this.y, agentState, agentAction);
    }

    private void moveTo(int i, int j, AgentState agentState, AgentAction agentAction) {
        // we need to find a path from the current position to the given destination keeping into account the walls
        // if no walls in memory, just take the next best step
        if(agentState.getMemoryFragment("walls") == null){
            moveToBestPosition(i, j, agentState, agentAction);
            return;
        }
        // calculate the optimal path and fetch the best next step
        // first add all walls as nodes
        ArrayList<Node> nodeList = new ArrayList<>();
        for(String posStr: agentState.getMemoryFragment("walls").split("-")){
            String[] pos = posStr.split(";");
            nodeList.add(new Node(Integer.parseInt(pos[0]), Integer.parseInt(pos[1])));
        }
        // now get the next best position from the optimal path
        int[] bestPos = getBestNextMove(agentState.getX(), agentState.getY(), i, j, nodeList, agentState);
        System.out.println(Arrays.toString(bestPos));
        agentAction.step(bestPos[0], bestPos[1]);
    }

    private void moveToBestPosition(int i, int j, AgentState agentState, AgentAction agentAction) {
        // Potential moves an agent can make (radius of 1 around the agent)
        List<Coordinate> potMoves = new ArrayList<>(List.of(
                new Coordinate(1, 1), new Coordinate(-1, -1),
                new Coordinate(1, 0), new Coordinate(-1, 0),
                new Coordinate(0, 1), new Coordinate(0, -1),
                new Coordinate(1, -1), new Coordinate(-1, 1)
        ));
        Coordinate bestMove = potMoves.stream()
                // Map relative to abs coordinates
                .map(move -> new Coordinate(move.getX() + agentState.getX(), move.getY() + agentState.getY()))
                // Filter impossible moves away
                .filter(move -> {
                    CellPerception c = agentState.getPerception().getCellPerceptionOnAbsPos(move.getX(), move.getY());
                    return (c != null && c.isWalkable());
                })
                // Use move that results in minimum distance to target
                .min((move1, move2) -> {
                    double d1 = Perception.manhattanDistance(move1.getX(), move1.getY(), i, j);
                    double d2 = Perception.manhattanDistance(move2.getX(), move2.getY(), i, j);
                    return Double.compare(d1, d2);
                }).orElse(null);

        // If bestMove == null, agent stuck?
        agentAction.step(bestMove.getX(), bestMove.getY());
    }
}
