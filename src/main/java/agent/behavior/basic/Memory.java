package agent.behavior.basic;

import agent.AgentState;
import environment.CellPerception;
import environment.world.destination.DestinationRep;
import util.MyColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Memory {
    DESTINATIONS {
        String encode(CellPerception cell) {
            DestinationRep dest = cell.getRepOfType(DestinationRep.class);
            return "(" + dest.getX() + "," + dest.getY() + "," + MyColor.getName(dest.getColor()) + ")";
        }
    },
    CHARGERS,
    WALLS,
    AIR,
    UNKNOWN;

    // Is used to extract (2,0) (10,555) (0,0) (6,-4) and (-45,10,green)
    // from a string like "(2,0)-(10,555)(0,0)(6,-4);;;(-45,10,green)"
    private static final Pattern pattern = Pattern.compile("\\((-?)\\w*,(-?)\\w*(,\\w*)?\\)");

    private final String memoryFragment = this.toString();

    /**
     * Default encoding for cells in memory.
     * @param cell to encode
     * @return (x,y)
     */
    String encode(CellPerception cell) {
        return "(" + cell.getX() + "," + cell.getY() + ")";
    }

    /**
     * Gets values from the stored (x,y) string.
     * @param storedCell the string in memory
     * @return the values
     */
    String[] decode(String storedCell) {
        String str = storedCell.substring(1, storedCell.length()-2); // remove brackets
        return str.split(","); // get stored values
    }

    /**
     * Adds a cell the agent's memory.
     * @param agentState the state of the agent
     * @param cell to store in memory
     */
    static void add(AgentState agentState, CellPerception cell) {
        if (cell.containsAnyDestination()) Memory.DESTINATIONS.store(agentState, cell);
        else if (cell.containsEnergyStation()) Memory.CHARGERS.store(agentState, cell);
        else if (cell.containsWall()) Memory.WALLS.store(agentState, cell);
        else if (cell.isFree()) Memory.AIR.store(agentState, cell);
        else Memory.UNKNOWN.store(agentState, cell); // Should never be used
    }

    static void add(AgentState agentState, Collection<CellPerception> cells) {
        for (CellPerception cell : cells) add(agentState, cell);
    }

    /**
     * Stores a cell into the correct MemoryFragment of the agent's memory.
     * @param agentState the state of the agent
     * @param cell to store in memory
     */
    private void store(AgentState agentState, CellPerception cell) {
        String rawData = agentState.getMemoryFragment(memoryFragment);
        String newCell = this.encode(cell);

        // The agent doesn't have a memory fragment for this type of cell yet.
        if (rawData == null || rawData.length() == 0)
            agentState.addMemoryFragment(memoryFragment, newCell);

        // Add the cell to the existing list of this memory fragment, if it is not in memory yet.
        else {
            Matcher matcher = pattern.matcher(rawData);
            while (matcher.find()) {
                if (newCell.equals(matcher.group())) return; // Already in memory
            }
            agentState.addMemoryFragment(memoryFragment, rawData + newCell);
        }
    }

    /**
     * Gets all cells, stored in memory, of 'this' type by converting memory into a CellPerception list.
     * @param agentState the state of the agent
     * @return a list of CellPerceptions
     */
    List<String[]> getAllStored(AgentState agentState) {
        List<String[]> result = new ArrayList<>();
        String rawData = agentState.getMemoryFragment(memoryFragment);

        Matcher matcher = pattern.matcher(rawData);
        while (matcher.find()) result.add(decode(matcher.group()));
        return result;
    }

    boolean isEmpty(AgentState agentState) {
        return agentState.getMemoryFragment(memoryFragment) == null ||
                agentState.getMemoryFragment(memoryFragment).isEmpty();
    }
}
