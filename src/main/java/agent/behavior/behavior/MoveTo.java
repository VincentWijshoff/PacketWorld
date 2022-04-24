package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import agent.behavior.basic.Memory;
import environment.CellPerception;
import environment.Coordinate;
import environment.Perception;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static agent.behavior.basic.Basic.*;

public class MoveTo extends Behavior {

    private int x, y;

    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        communicateInfo(agentState, agentCommunication);
    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        // also here store walls and destinations
        if (optimization2) {
            storeView(agentState);
        }

        if(dropPacketIfDying(agentState, agentAction)) return; // only 1 action per move

        int[] target = Objects.requireNonNull(Memory.getTarget(agentState));
        this.x = target[0];
        this.y = target[1];

        this.moveTo(this.x, this.y, agentState, agentAction);
    }

    private void moveTo(int i, int j, AgentState agentState, AgentAction agentAction) {
        // we need to find a path from the current position to the given destination keeping into account the walls
        // if no walls in memory, just take the next best step
        // calculate the optimal path and fetch the best next step
        // first add all walls as nodes

        // now get the next best position from the optimal path
        Pair<int[], ArrayList<Node>> result = getBestNextMove(agentState.getX(), agentState.getY(), i, j, agentState, true);
        int[] bestPos = result.first;
        ArrayList<Node> packetsOnPath = result.second;
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
