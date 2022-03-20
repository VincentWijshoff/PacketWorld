package agent.behavior.behavior;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.Coordinate;
import environment.Perception;
import environment.world.destination.DestinationRep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static agent.behavior.basic.Basic.findOfType;

public class Wander extends Behavior {
    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {

    }

    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        if (agentState.seesDestination()) {
            this.storeDestinations(agentState, agentAction);
        }
        this.walkRandom(agentState, agentAction);
    }

    //Store any destinations currently in vision in memory
    private void storeDestinations(AgentState agentState, AgentAction agentAction) {
        List<CellPerception> Dests = findOfType(DestinationRep.class, agentState);
        for (CellPerception dest: Dests) {
            DestinationRep destRep = dest.getRepOfType(DestinationRep.class);
            String data = destRep.getX() + ";" + destRep.getY();
            agentState.addMemoryFragment(destRep.getColor().toString(), data);
        }
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


        if (true) { //enable optimization
            //if rightmost cell is further away than left most -> move right
            //the idea is that this maximizes the perception -> packets/destinations should be discovered quicker
            //NOTE: it is important to prioritise moves by swapping with counterpart, not by placing the move first. This causes the agent to be stuck in places such as 'tight hallways'. TODO: draw diagram of this scenario
            //TODO: implement diagonal moves
            Perception pc = agentState.getPerception();
            if (pc.getWidth() - pc.getSelfX() - 1 > pc.getSelfX() && moves.indexOf(new Coordinate(1, 0)) > moves.indexOf(new Coordinate(-1, 0))) {
                Collections.swap(moves, moves.indexOf(new Coordinate(1, 0)), moves.indexOf(new Coordinate(-1, 0))); //prioritise right
            }
            if (pc.getWidth() - pc.getSelfX() - 1 < pc.getSelfX() && moves.indexOf(new Coordinate(1, 0)) < moves.indexOf(new Coordinate(-1, 0))) {
                Collections.swap(moves, moves.indexOf(new Coordinate(1, 0)), moves.indexOf(new Coordinate(-1, 0))); //prioritise left
            }
            if (pc.getHeight() - pc.getSelfY() - 1 > pc.getSelfY() && moves.indexOf(new Coordinate(0, 1)) > moves.indexOf(new Coordinate(0, -1))) {
                Collections.swap(moves, moves.indexOf(new Coordinate(0, 1)), moves.indexOf(new Coordinate(0, -1))); //prioritise down
            }
            if (pc.getHeight() - pc.getSelfY() - 1 < pc.getSelfY() && moves.indexOf(new Coordinate(0, 1)) < moves.indexOf(new Coordinate(0, -1))) {
                Collections.swap(moves, moves.indexOf(new Coordinate(0, 1)), moves.indexOf(new Coordinate(0, -1))); //prioritise up
            }
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
}
