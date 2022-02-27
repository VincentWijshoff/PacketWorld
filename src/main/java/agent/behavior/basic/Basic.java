package agent.behavior.basic;

import java.awt.*;
import java.util.*;
import java.util.List;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import agent.behavior.Behavior;
import environment.CellPerception;
import environment.Coordinate;
import environment.Perception;
import environment.Representation;
import environment.world.destination.DestinationRep;
import environment.world.packet.PacketRep;

public class Basic extends Behavior {

    @Override
    public void communicate(AgentState agentState, AgentCommunication agentCommunication) {
        // No communication
    }


    @Override
    public void act(AgentState agentState, AgentAction agentAction) {
        if (agentState.hasCarry()) {
            findDestination(agentState, agentAction);
            return;
        }
        findPacket(agentState, agentAction);
    }

    // precondition: agentState.getCarry().isPresent()
    private void findDestination(AgentState agentState, AgentAction agentAction) {
        Color packetColor = agentState.getCarry().get().getColor();

        // Get all Destinations by going through view, sorted by distance from agent
        List<CellPerception> closeDests = findOfType(DestinationRep.class, agentState);
        // Get the closest destination with the correct color
        CellPerception closestDest = closeDests.stream()
                .filter(dest -> dest.containsDestination(packetColor))
                .findFirst().orElse(null);

        if (closestDest == null) walkRandom(agentState, agentAction);
        else if (isNeighbour(closestDest, agentState)) agentAction.putPacket(closestDest.getX(), closestDest.getY());
        else moveTo(closestDest.getX(), closestDest.getY(), agentState, agentAction);
    }

    private void findPacket(AgentState agentState, AgentAction agentAction) {
        // Get all Packets by going through view, sorted by distance from agent: first element is the closest
        List<CellPerception> closePackets = findOfType(PacketRep.class, agentState);
        CellPerception closestPacket = closePackets.stream().findFirst().orElse(null);

        if (closestPacket == null) walkRandom(agentState, agentAction);
        else if (isNeighbour(closestPacket, agentState)) agentAction.pickPacket(closestPacket.getX(), closestPacket.getY());
        else moveTo(closestPacket.getX(), closestPacket.getY(), agentState, agentAction);
    }

    /**
     * Finds all CellPerceptions in the view that are of a given type.
     * @param clazz The type to look for
     * @param agentState The current agent state
     * @return A list, sorted by distance to agent (small to big)
     */
    private <T extends Representation> List<CellPerception> findOfType(Class<T> clazz, AgentState agentState) {
        // Get all in view
        CellPerception[][] fullArea = getViewArea(agentState);

        // Convert to list of CellPerception with Representation of type clazz
        List<CellPerception> fullAreaList = new ArrayList<>();
        for (CellPerception[] c1 : fullArea) {
            for (CellPerception c2 : c1)
                if (c2 != null && c2.getRepOfType(clazz) != null) fullAreaList.add(c2);
        }
        // Sort according to distance to agent, smallest distance first in list
        fullAreaList.sort((p1, p2) -> {
            double d1 = Perception.distance(agentState.getX(), agentState.getY(), p1.getX(), p1.getY());
            double d2 = Perception.distance(agentState.getX(), agentState.getY(), p2.getX(), p2.getY());
            return Double.compare(d1, d2);
        });
        return fullAreaList;
    }

    // Returns true if the given cellPerception is a current neighbour of agentState
    private boolean isNeighbour(CellPerception cellPerception, AgentState agentState) {
        for (CellPerception neighbour : agentState.getPerception().getNeighbours()) {
            if (cellPerception.equals(neighbour)) return true;
        }
        return false;
    }

    private CellPerception[][] getViewArea(AgentState agentState) {
        CellPerception[][] perceptionList = new CellPerception[agentState.getPerception().getWidth()][agentState.getPerception().getHeight()];
        int left = agentState.getPerception().getOffsetX() - agentState.getX();
        int top = agentState.getPerception().getOffsetY() - agentState.getY();
        int right = agentState.getPerception().getOffsetX() + agentState.getPerception().getWidth() - agentState.getX();
        int bottom = agentState.getPerception().getOffsetY() + agentState.getPerception().getHeight() - agentState.getY();
        for (int i = left; i < right ; i++) {
            for (int j = top; j < bottom ; j++) {
                perceptionList[i - left][j - top] = agentState.getPerception().getCellPerceptionOnRelPos(i, j);
            }
        }
        return perceptionList;
    }

    private void moveTo(int i, int j, AgentState agentState, AgentAction agentAction) {
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

        // No viable moves, skip turn
        agentAction.skip();
    }
}
