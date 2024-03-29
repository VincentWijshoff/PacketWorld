package agent.behavior.basic;

import java.util.*;
import java.util.List;

import agent.AgentAction;
import agent.AgentCommunication;
import agent.AgentState;
import environment.*;
import environment.world.agent.Agent;
import environment.world.agent.AgentRep;
import environment.world.wall.WallRep;
import util.Pair;

public class Basic {

    public static boolean optimization1 = true; // move away from walls
    public static boolean optimization2 = true; // store destination locations, walls and pathfinder
    public static boolean optimization3 = true; // don't go back to recently visited positions
    public static boolean communication = true; // Allow agents to communicate

    public static void communicateInfo(AgentState agentState, AgentCommunication agentCommunication){
        if (!communication) return;

        // broadcast charger locations
        Set<String> keys = agentState.getMemoryFragmentKeys();
        if (keys.contains(Memory.MemKey.CHARGERS.toString())) {
            String message = Memory.MemKey.CHARGERS + "=" + agentState.getMemoryFragment(Memory.MemKey.CHARGERS.toString());
            agentCommunication.broadcastMessage(message);
        }

        //find all agent reps
        List<CellPerception> perceptions = findOfType(AgentRep.class, agentState);
        //send to all agent reps
        for (CellPerception perc:perceptions) {
            AgentRep agent = perc.getAgentRepresentation().get();

            for (Memory.MemKey memkey: new Memory.MemKey[] {Memory.MemKey.DESTINATIONS, Memory.MemKey.WALLS, Memory.MemKey.BLOCKING_PACKETS, Memory.MemKey.CLEARED_PACKETS}) {
                if (keys.contains(memkey.toString())) {
                    String message = memkey + "=" + agentState.getMemoryFragment(memkey.toString());
                    agentCommunication.sendMessage(agent, message);
                }
            }
        }



        Collection<Mail> messages = agentCommunication.getMessages();

        // messages have the following structure: TYPE=DATA
        for (Mail m : messages) {
            String[] info = m.getMessage().split("=");
            Memory.MemKey key = Memory.getKey(info[0]);
            if(Objects.equals(key, Memory.MemKey.RECENTLY_VISITED) || Objects.equals(key, Memory.MemKey.TARGET)) continue;

            Memory.MemoryFragment frag = Memory.getFragment(info[0]);
            if (frag.isEmpty(agentState)) {
                agentState.addMemoryFragment(info[0], info[1]);
            } else {
                frag.storeAll(agentState, info[1]);
            }
        }
        agentCommunication.clearMessages();
        //Memory.printMemory(agentState);
    }

    public static boolean dropPacketIfDying(AgentState agentState, AgentAction agentAction) {
        if (agentState.getBatteryState() <= 20 && agentState.hasCarry()) {
            CellPerception[] neighbours = agentState.getPerception().getNeighbours();
            CellPerception freeCell = Arrays.stream(neighbours).filter(
                    cellPerception -> {
                        return cellPerception != null &&
                                cellPerception.isFree() &&
                                agentState.getPerception().getCellPerceptionOnAbsPos(cellPerception.getX(), cellPerception.getY() - 1) != null &&
                                !agentState.getPerception().getCellPerceptionOnAbsPos(cellPerception.getX(), cellPerception.getY() - 1).containsEnergyStation(); //dont place packet on charging station
                    }
            ).findFirst().get();

            agentAction.putPacket(freeCell.getX(), freeCell.getY());
            return true;
        }
        return false;
    }


    private static String encode(List<Node> nodes) {
        StringBuilder fin = new StringBuilder();
        for (Node n : nodes) {
            fin.append(n.x).append(";").append(n.y).append("-");
        }
        fin.deleteCharAt(fin.length()-1);
        return fin.toString();
    }

    /**
     * Finds all CellPerceptions in the view that are of a given type.
     * @param clazz The type to look for
     * @param agentState The current agent state
     * @return A list, sorted by distance to agent (small to big)
     */
    public static <T extends Representation> List<CellPerception> findOfType(Class<T> clazz, AgentState agentState) {
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

    /**
     * Gets all the cells the agent can currently observe.
     * @param agentState The agent
     * @return 2D CellPerception array relative to the agent's position.
     */
    public static CellPerception[][] getViewArea(AgentState agentState) {
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


    // Add all in-sight in memory
    public static void storeView(AgentState agentState) {
        List<CellPerception> viewArea = new ArrayList<>();
        for (CellPerception[] c1 : getViewArea(agentState)) {
            viewArea.addAll(Arrays.asList(c1));
        }
        // Don't add agent itself to memory
        viewArea.remove(agentState.getPerception().getCellAt(agentState.getX(), agentState.getY()));
        // Don't add null-cells
        viewArea.removeAll(Collections.singleton(null));

        // Don't add unreachable destinations
        Collection<CellPerception> unreachableDestinations = new ArrayList<>();
        for (CellPerception perc: viewArea ) {
            if (perc.containsAnyDestination()) {
                Pair<int[], ArrayList<Node>> result = getBestNextMove(agentState.getX(), agentState.getY(), perc.getX(), perc.getY(), agentState, false);
                boolean reachable = result.getFirst() != null && result.getSecond().size() == 0;
                if (!reachable) unreachableDestinations.add(perc);
            }
        }
        viewArea.removeAll(unreachableDestinations);

        viewArea.addAll(storeOutOfBounds(agentState));

        Memory.addAll(agentState, viewArea);
        // Memory.printMemory(agentState);
    }

    private static List<CellPerception> storeOutOfBounds(AgentState agentState) {
        //Left and top side of the world is already checked with a < 0 check in getBestStep
        ArrayList<CellPerception> outOfBounds = new ArrayList<>();
        Perception perc = agentState.getPerception();
        if (perc.getSelfX() > (perc.getWidth()-1)/2) {
            int relX = perc.getWidth() - perc.getSelfX();
            for (int i = 0; i < perc.getHeight(); i++) {
                int relY = i - perc.getSelfY();
                if (perc.getCellPerceptionOnRelPos(relX, relY) == null && perc.getCellPerceptionOnRelPos(relX-1, relY) != null && !perc.getCellPerceptionOnRelPos(relX -1 , relY).containsWall()) {
                    for (int j = 0; j < perc.getHeight(); j++) {
                        int absY = j - perc.getSelfY() + agentState.getY();
                        int absX = relX + agentState.getX();
                        CellPerception cellPerception = new CellPerception(absX, absY);
                        cellPerception.addRep(new WallRep(absX, absY) {
                            @Override
                            public boolean isSeeThrough() {
                                return false;
                            }
                        });
                        outOfBounds.add(cellPerception);
                    }
                    break;
                }
            }
        }
        if (perc.getSelfY() > (perc.getHeight()-1)/2) {
            int relY = perc.getHeight() - perc.getSelfY();
            for (int i = 0; i < perc.getWidth(); i++) {
                int relX = i - perc.getSelfX();
                if (perc.getCellPerceptionOnRelPos(relX, relY) == null && perc.getCellPerceptionOnRelPos(relX, relY-1) != null && !perc.getCellPerceptionOnRelPos(relX, relY-1).containsWall()) {
                    for (int j = 0; j < perc.getWidth(); j++) {
                        int absX = j - perc.getSelfX() + agentState.getX();
                        int absY = relY + agentState.getY();
                        CellPerception cellPerception = new CellPerception(absX, absY);
                        cellPerception.addRep(new WallRep(absX, absY) {
                            @Override
                            public boolean isSeeThrough() {
                                return false;
                            }
                        });
                        outOfBounds.add(cellPerception);
                    }
                    break;
                }
            }
        }
        return outOfBounds;
    }


    // Helper class for breadth-first optimal path search.
    public static class Node {
        public int x, y;
        public Node prev = null;
        public int pathLength = 0;

        public Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static Pair<int[],ArrayList<Node>> getBestNextMove(int x, int y, int xDest, int yDest, AgentState agentState, boolean ignoreOutOfPerc) {
        ArrayList<Node> visitedDest = new ArrayList<>();
        visitedDest.add(new Node(x, y));
        ArrayList<Node> endPoints = new ArrayList<>();
        endPoints.add(new Node(x, y));

        ArrayList<Node> wallList = new ArrayList<>();
        for (int[] pos : Memory.walls().getAllStoredPos(agentState)) {
            wallList.add(new Node(pos[0], pos[1]));
        }

        // Basic breadth-first search which will give the shortest path to the destination
        while (foundFinish(endPoints, xDest, yDest) == null) {
            // we will expand each endpoint
            endPoints = getBestStep(endPoints, wallList, visitedDest, agentState, xDest, yDest, ignoreOutOfPerc);
            visitedDest.addAll(endPoints);

            if (endPoints.isEmpty()) { // No path found
                return new Pair<>(null, new ArrayList<>());
            }
        }
        Node finish = foundFinish(endPoints, xDest, yDest);

        ArrayList<Node> packetsOnPath = new ArrayList<>();
        while (finish != null && finish.prev != null && finish.prev.prev != null) {
            finish = finish.prev;
            if (agentState.getPerception().getCellPerceptionOnAbsPos(finish.x, finish.y) != null && agentState.getPerception().getCellPerceptionOnAbsPos(finish.x, finish.y).containsPacket()) {
                packetsOnPath.add(new Node(finish.x, finish.y));  //Mark all packet locations, and pass as result
            }
        }

        // the next pos should be the first one in the chain
        return new Pair<>(new int[]{finish.x, finish.y}, packetsOnPath);
    }

    private static Node foundFinish(List<Node> endPoints, int xDest, int yDest) {
        for (Node n : endPoints) {
            if (n.x == xDest && n.y == yDest) return n;
        }
        return null;
    }

    public static void moveTo(AgentState agentState, AgentAction agentAction, int[] target) {
        Pair<int[], ArrayList<Node>> result = getBestNextMove(agentState.getX(), agentState.getY(), target[0], target[1], agentState, true);
        int[] bestPos = result.first;
        ArrayList<Node> packetsOnPath = result.second;
        if (bestPos == null) {
            agentAction.skip();
        }
        else {
            agentAction.step(bestPos[0], bestPos[1]);
        }
    }

    private static ArrayList<Node> getBestStep(List<Node> endPoints,
                                          ArrayList<Node> wallList,
                                          ArrayList<Node> visitedDest,
                                               AgentState agentState,
                                               int xDest,
                                               int yDest,
                                               boolean ignoreOutOfPerception) {
        ArrayList<Node> newEndPoints = new ArrayList<>();
        for (Node n : endPoints) {
            List<Coordinate> potMoves = new ArrayList<>(List.of(
                    new Coordinate(1, 1), new Coordinate(-1, -1),
                    new Coordinate(1, 0), new Coordinate(-1, 0),
                    new Coordinate(0, 1), new Coordinate(0, -1),
                    new Coordinate(1, -1), new Coordinate(-1, 1)
            ));
            List<Coordinate> bestMove = potMoves.stream()
                    // Map relative to abs coordinates
                    .map(move -> new Coordinate(move.getX() + n.x, move.getY() + n.y))
                    // Filter impossible moves away
                    .filter(move -> {
                        // if it is the required destination return it
                        if (xDest == move.getX() && yDest == move.getY()) return true;
                        // walls
                        for (Node n2 : wallList) {
                            if (n2.x == move.getX() && n2.y == move.getY()) return false;
                        }
                        // already been there
                        for (Node n2 : visitedDest) {
                            if (n2.x == move.getX() && n2.y == move.getY()) return false;
                        }
                        // negative positions
                        if (move.getX() < 0 || move.getY() < 0) return false;
                        if (move.getX() > 30 || move.getY() > 30) return false; //We assume there is no world larger than 30x30

                        // check if it is actually walkable (if we can see it)
                        if (agentState.getPerception().getCellPerceptionOnAbsPos(move.getX(), move.getY()) == null) {
                            // cannot see this position
                            // we need a check for the edges off the world
                            // only if the new position is 1 away from the current position, otherwise dont care
                            return ignoreOutOfPerception && (Math.abs(move.getX() - agentState.getX()) > 1 ||
                                    Math.abs(move.getY() - agentState.getY()) > 1);
                            // False = cannot walk here so may not be an option
                            // True = cannot see this and not an option for next move because > 1 away, so assume is walkable
                        }
                        //Cells need to be walkable
                        return Objects.requireNonNull(agentState.getPerception().
                                getCellPerceptionOnAbsPos(move.getX(), move.getY())).isWalkable() ||
                                (Objects.requireNonNull(agentState.getPerception().
                                        getCellPerceptionOnAbsPos(move.getX(), move.getY())).containsPacket() && !ignoreOutOfPerception);  //allow packet cells


                    }).toList();
            // make them all into nodes
            ArrayList<Node> options = new ArrayList<>();
            for (Coordinate c : bestMove) {
                Node newAddition = new Node(c.getX(), c.getY());
                newAddition.prev = n;
                newAddition.pathLength = n.pathLength+1;
                options.add(newAddition);
            }
            // check if another endpoint exists on the same position, but closer to the destination
            for (Node prevNode : newEndPoints) {
                options.removeIf(newNode -> newNode.x == prevNode.x && newNode.y == prevNode.y);
            }
            // now add the new options to the new endpoints
            newEndPoints.addAll(options);
        }
        // return all new endpoints
        return newEndPoints;
    }
}
