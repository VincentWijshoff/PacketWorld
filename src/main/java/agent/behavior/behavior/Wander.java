package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.Coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Wander extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
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
    }
}
