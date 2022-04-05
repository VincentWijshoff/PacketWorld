package agent.behavior.basic;

import java.util.*;
import java.util.List;

import agent.AgentCommunication;
import agent.AgentState;
import environment.*;
import environment.world.destination.DestinationRep;
import environment.world.energystation.EnergyStationRep;

public class Basic {

    public static boolean optimization1 = true; // move away from walls
    public static boolean optimization2 = true; // store destination locations, walls and pathfinder
    public static boolean optimization3 = true; // don't go back to recently visited positions

    public static void communicateInfo(AgentState agentState, AgentCommunication agentCommunication){
        Collection<Mail> messages = agentCommunication.getMessages();
        agentCommunication.clearMessages();
        // messages have the following structure: TYPE=DATA
        for (Mail m : messages) {
            String[] info = m.getMessage().split("=");
            String knownData = agentState.getMemoryFragment(info[0]);
            if(knownData == null){
                agentState.addMemoryFragment(info[0], info[1]);
                continue;
            }
            List<Node> knownNodes = new ArrayList<>();
            List<Node> receivedNodes = new ArrayList<>();
            for (String knownPoint : knownData.split("-")) {
                knownNodes.add(new Node(Integer.parseInt(knownPoint.split(";")[0]), Integer.parseInt(knownPoint.split(";")[1])));
            }
            for (String recPoint : info[1].split("-")) {
                receivedNodes.add(new Node(Integer.parseInt(recPoint.split(";")[0]), Integer.parseInt(recPoint.split(";")[1])));
            }
            knownNodes.removeIf((node -> {
                List<Node> copyOfRecNodes = receivedNodes.stream().toList();
                copyOfRecNodes.removeIf(node1 -> node.x != node1.x || node.y != node1.y);
                return !copyOfRecNodes.isEmpty();
            }));
            agentState.addMemoryFragment(info[0], encode(knownNodes) + "-" + encode(receivedNodes));
        }
        // now broadcast all known info
        Set<String> keys = agentState.getMemoryFragmentKeys();
        for (String key : keys) {
            String message = key + "=" + agentState.getMemoryFragment(key);
            agentCommunication.broadcastMessage(message);
        }
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
    private static CellPerception[][] getViewArea(AgentState agentState) {
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

    /**
     * Stores all destinations in view into the agent's memory.
     * @param agentState The agent seeing and storing destinations.
     * @note Destinations that are already in memory are ignored to avoid duplicates.
     */
    /*public static void storeDestinations(AgentState agentState) {
        storeChargingStations(agentState);
        if (!agentState.seesDestination()) return;
        List<CellPerception> dests = findOfType(DestinationRep.class, agentState);
        DESTS: for (CellPerception dest : dests) {
            DestinationRep destRep = dest.getRepOfType(DestinationRep.class);
            String data = destRep.getX() + ";" + destRep.getY();
            if (agentState.getMemoryFragment(destRep.getColor().toString()) != null) {
                // now we need to check if the destination was not already stored in memory
                String mem = agentState.getMemoryFragment(destRep.getColor().toString());
                String[] destinations = mem.split("-");
                for (String destination : destinations) {
                    if (data.equals(destination)) continue DESTS; // already in memory
                }
                // not in memory
                agentState.addMemoryFragment(destRep.getColor().toString(), mem + "-" + data);
            } else {
                agentState.addMemoryFragment(destRep.getColor().toString(), data);
            }
        }
    }

    public static void storeChargingStations(AgentState agentState){
        List<CellPerception> dests = findOfType(EnergyStationRep.class, agentState);
        DESTS: for (CellPerception dest : dests) {
            EnergyStationRep destRep = dest.getRepOfType(EnergyStationRep.class);
            String data = destRep.getX() + ";" + destRep.getY();
            if (agentState.getMemoryFragment("chargers") != null) {
                // now we need to check if the destination was not already stored in memory
                String mem = agentState.getMemoryFragment("chargers");
                String[] destinations = mem.split("-");
                for (String destination : destinations) {
                    if (data.equals(destination)) continue DESTS; // already in memory
                }
                // not in memory
                agentState.addMemoryFragment("chargers", mem + "-" + data);
            } else {
                agentState.addMemoryFragment("chargers", data);
            }
        }
    }*/

    public static void storeView(AgentState agentState) {
        List<CellPerception> viewArea = new ArrayList<>();
        for (CellPerception[] c1 : getViewArea(agentState)) {
            for (CellPerception c2 : c1)
                viewArea.add(c2);
        }
        Memory.add(agentState, viewArea);
    }

    /**
     * Stores all walls in view into the agent's memory.
     * @param agentState The agent seeing and storing walls.
     * @note Walls that are already in memory are ignored to avoid duplicates.
     */
    /*public static void storeWalls(AgentState agentState) {
        // Get all in view range
        CellPerception[][] fullArea = getViewArea(agentState);

        // every null is a wall
        List<Node> fullWallList = new ArrayList<>();
        List<Node> fullAirList = new ArrayList<>();
        for (int i = 0; i < fullArea.length; i++) {
            for (int j = 0; j < fullArea[i].length; j++) {
                Node node = new Node(i + agentState.getPerception().getOffsetX()
                        , j + agentState.getPerception().getOffsetY());

                if(fullArea[i][j] == null || fullArea[i][j].containsWall()) fullWallList.add(node);
                else fullAirList.add(node);
            }
        }

        // Add previously stored Air and Walls to lists
        addStoredToList(fullAirList, "air", agentState);
        addStoredToList(fullWallList, "walls", agentState);

        // now we check wall list against air list
        ArrayList<Node> toRemove = new ArrayList<>();
        for (Node wall : fullWallList) {
            for (Node air : fullAirList) {
                if (wall.x == air.x && wall.y == air.y) {
                    // this is definitely not a wall
                    toRemove.add(wall);
                }
            }
        }
        for (Node n : toRemove) fullWallList.remove(n);

        // and finally we add both to memory
        StringBuilder wallMem = new StringBuilder();
        StringBuilder airMem = new StringBuilder();

        if (fullWallList.size() > 0) {
            for (Node wall : fullWallList) {
                wallMem.append("-").append(wall.x).append(";").append(wall.y);
            }
            wallMem.deleteCharAt(0);
        }

        if (fullAirList.size() > 0) {
            for (Node air : fullAirList) {
                airMem.append("-").append(air.x).append(";").append(air.y);
            }
            airMem.deleteCharAt(0);
        }

        agentState.addMemoryFragment("walls", wallMem.toString());
        agentState.addMemoryFragment("air", airMem.toString());
    }*/

    /**
     * Adds stored positions in the agent's memory to the given list.
     * @param listOfNodes The list to add the stored positions to.
     * @param memoryKey The type of cells to look for in memory.
     * @param agentState The agent state
     */
    private static void addStoredToList(List<Node> listOfNodes, String memoryKey, AgentState agentState) {
        if (agentState.getMemoryFragment(memoryKey) == null) return;

        String[] allStored = agentState.getMemoryFragment(memoryKey).split("-");
        for (String storedPos : allStored) {
            if (storedPos.length() == 0) continue;

            String[] pos = storedPos.split(";");
            // Don't add when pos is already in list
            boolean inList = false;
            for (Node n : listOfNodes) {
                if (n.x == Integer.parseInt(pos[0]) && n.y == Integer.parseInt(pos[1])) {
                    inList = true;
                    break;
                }
            }
            if (!inList) listOfNodes.add(new Node(Integer.parseInt(pos[0]), Integer.parseInt(pos[1])));
        }
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

    public static int[] getBestNextMove(int x, int y, int xDest, int yDest, ArrayList<Node> wallList, AgentState agentState) {
        ArrayList<Node> visitedDest = new ArrayList<>();
        visitedDest.add(new Node(x, y));
        ArrayList<Node> endPoints = new ArrayList<>();
        endPoints.add(new Node(x, y));
        // Basic breadth-first search which will give the shortest path to the destination
        while (foundFinnish(endPoints, xDest, yDest) == null) {
            // we will expand each endpoint
            endPoints = getBestStep(endPoints, wallList, visitedDest, agentState, xDest, yDest);
            visitedDest.addAll(endPoints);
        }
        Node finnish = foundFinnish(endPoints, xDest, yDest);
        while (finnish != null && finnish.prev != null && finnish.prev.prev != null) {
            finnish = finnish.prev;
        }

        // the next pos should be the first one in the chain
        return new int[]{finnish.x, finnish.y};
    }

    private static Node foundFinnish(List<Node> endPoints, int xDest, int yDest) {
        for (Node n : endPoints) {
            if (n.x == xDest && n.y == yDest) return n;
        }
        return null;
    }

    private static ArrayList<Node> getBestStep(List<Node> endPoints,
                                          ArrayList<Node> wallList,
                                          ArrayList<Node> visitedDest,
                                               AgentState agentState,
                                               int xDest,
                                               int yDest) {
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

                        // check if it is actually walkable (if we can see it)
                        if (agentState.getPerception().getCellPerceptionOnAbsPos(move.getX(), move.getY()) == null) {
                            // cannot see this position
                            // we need a check for the edges off the world
                            // only if the new position is 1 away from the current position, otherwise dont care
                            return Math.abs(move.getX() - agentState.getX()) > 1 ||
                                    Math.abs(move.getY() - agentState.getY()) > 1;
                            // False = cannot walk here so may not be an option
                            // True = cannot see this and not an option for next move because > 1 away, so assume is walkable
                        }
                        if (!Objects.requireNonNull(agentState.getPerception().
                                getCellPerceptionOnAbsPos(move.getX(), move.getY())).isWalkable()) return false;

                        return true;
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
