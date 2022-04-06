package agent.behavior.basic;

import agent.AgentState;
import com.google.common.collect.Table;
import environment.CellPerception;
import environment.world.destination.DestinationRep;
import util.MyColor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Memory {

    // The MemoryFragmentKeys (MAX_MEMORY_FRAGMENTS = 10 in AgentImp)
    private enum MemKey {
        AIR,
        WALLS,
        CHARGERS,
        DESTINATIONS,
        TARGET, // kept separate, not in mem-map
        RECENTLY_VISITED, // optimization3
        DEFAULT, // Packets, other agents, ...
    }
    public static MemoryFragment air() { return mem.get(MemKey.AIR); }
    public static MemoryFragment walls() { return mem.get(MemKey.WALLS); }
    public static MemoryFragment chargers() { return mem.get(MemKey.CHARGERS); }
    public static MemoryFragment destinations() { return mem.get(MemKey.DESTINATIONS); }
    public static MemoryFragment recentVisits() { return mem.get(MemKey.RECENTLY_VISITED); }
    public static MemoryFragment defaults() { return mem.get(MemKey.DEFAULT); }


    // MemoryFragment specific functions
    private static final Map<MemKey, MemoryFragment> mem = new HashMap<>() {{
        put(MemKey.AIR, new MemoryFragment(MemKey.AIR) {
            @Override
            public boolean check(CellPerception cell) { return cell.isFree(); }
        });

        put(MemKey.WALLS, new MemoryFragment(MemKey.WALLS) {
            @Override
            public boolean check(CellPerception cell) { return cell.containsWall(); }
        });

        put(MemKey.CHARGERS, new MemoryFragment(MemKey.CHARGERS) {
            @Override
            public boolean check(CellPerception cell) { return cell.containsEnergyStation(); }
        });

        put(MemKey.DESTINATIONS, new MemoryFragment(MemKey.DESTINATIONS) {
            @Override
            public boolean check(CellPerception cell) { return cell.containsAnyDestination(); }

            @Override // Encode color of destination too
            public String encode(CellPerception cell) {
                DestinationRep dest = cell.getRepOfType(DestinationRep.class);
                return encode(new String[]{
                        Integer.toString(dest.getX()),
                        Integer.toString(dest.getY()),
                        MyColor.getName(dest.getColor())
                });
            }
        });
        put(MemKey.RECENTLY_VISITED, new MemoryFragment(MemKey.RECENTLY_VISITED) {
        });
        put(MemKey.DEFAULT, new MemoryFragment(MemKey.DEFAULT) {
        });
    }};

    // Default functionality for all memory fragments.
    public abstract static class MemoryFragment {

        // Is used to extract (2,0) (10,555) (0,0) (6,-4) and (-45,10,green)
        // from a string like "(2,0)-(10,555)(0,0)(6,-4);;;(-45,10,green)"
        private static final Pattern pattern = Pattern.compile("\\((-?)\\w*,(-?)\\w*(,\\w*)?\\)");
        public final String memoryFragment;

        public MemoryFragment(MemKey memoryFragment) {
            this.memoryFragment = memoryFragment.toString();
        }

        // Default encoding for cells in memory (@Overwrite these to change encoding per MemKey).
        public String encode(CellPerception cell) {
            return encode(new String[]{ Integer.toString(cell.getX()), Integer.toString(cell.getY()) });
        }
        public String encode(int[] loc) {
            return encode(new String[]{ Integer.toString(loc[0]), Integer.toString(loc[1]) });
        }

        /**
         * Encodes an array of strings.
         * @param attributes the strings
         * @return (attr1, attr2, ...)
         */
        public static String encode(String[] attributes) {
            StringBuilder result = new StringBuilder("(");
            for (String attr : attributes) result.append(attr).append(",");
            result.setCharAt(result.length()-1, ')');
            return result.toString();
        }

        /**
         * Gets values from the stored (x,y,...) string.
         * @param storedCell the string in memory
         * @return the values
         */
        static String[] decode(String storedCell) {
            if (storedCell == null) return null;
            String str = storedCell.substring(1, storedCell.length()-1); // remove brackets
            return str.split(","); // get stored values
        }

        // Method used to check if cell is of this type (@Overwrite)
        public boolean check(CellPerception cell) { return true; }

        // Add cells to memory
        public void add(AgentState agentState, Collection<CellPerception> cells) {
            for (CellPerception cell : cells) add(agentState, cell);
        }
        public void add(AgentState agentState, CellPerception cell) {
            if (check(cell)) store(agentState, cell);
        }

        // Stores a cell into 'this' MemoryFragment of the agent's memory.
        private void store(AgentState agentState, CellPerception cell) {
            String rawData = agentState.getMemoryFragment(memoryFragment);
            String newCell = this.encode(cell);

            // The agent doesn't have a memory fragment for this type of cell yet.
            if (rawData == null || rawData.length() == 0)
                agentState.addMemoryFragment(memoryFragment, newCell);

            // Add the cell to the existing list of this memory fragment, if it is not in memory yet.
            else {
                if (rawData.contains(newCell)) return;
                else if (getMemory(agentState).contains(newCell)) removeFromMemory(agentState, newCell);

                agentState.addMemoryFragment(memoryFragment, rawData + newCell);
            }
        }

        /**
         * Gets all cells, stored in memory, of 'this' type by converting memory into a list.
         * @param agentState the state of the agent
         * @return a list of strings representing the attributes [x, y, ...]
         */
        public List<String[]> getAllStored(AgentState agentState) {
            List<String[]> result = new ArrayList<>();
            String rawData = agentState.getMemoryFragment(memoryFragment);
            if (rawData != null) {
                Matcher matcher = pattern.matcher(rawData);
                while (matcher.find()) result.add(decode(matcher.group()));
            }
            return result;
        }

        /**
         * Gets all locations of cells, stored in memory, of 'this' type by converting memory into an int[] list.
         * @param agentState the state of the agent
         * @return a list of locations (x, y) without extra attributes.
         */
        public List<int[]> getAllStoredPos(AgentState agentState) {
            return getAllStored(agentState).stream()
                    .map(attr -> new int[]{ Integer.parseInt(attr[0]), Integer.parseInt(attr[1]) })
                    .toList();
        }

        /**
         * Sets an entire memory attribute to the given list of cell attributes.
         * @param agentState the state of the agent
         * @param listOfCellAttributes a list of strings representing the attributes [x, y, (color)]
         */
        public void setAllStored(AgentState agentState, List<String[]> listOfCellAttributes) {
            StringBuilder str = new StringBuilder();
            for (String[] attributes : listOfCellAttributes) {
                str.append(encode(attributes));
            }
            agentState.addMemoryFragment(memoryFragment, str.toString());
        }

        /**
         * Checks if the agent has an empty memory fragment of 'this' type.
         * @param agentState the state of the agent
         */
        public boolean isEmpty(AgentState agentState) {
            return agentState.getMemoryFragment(memoryFragment) == null ||
                    agentState.getMemoryFragment(memoryFragment).isEmpty();
        }
    }


    // General femory functions

    // Adds all cells to the correct memory fragment
    public static void addAll(AgentState agentState, Collection<CellPerception> cells) {
        List<MemoryFragment> memoryFragmentsToCheck = List.of(destinations(), walls(), chargers(), air());
        CELL: for (CellPerception cell : cells) {
            for (MemoryFragment frag : memoryFragmentsToCheck) {
                if (frag.check(cell)) {
                    frag.add(agentState, cell);
                    continue CELL;
                }
            }
            // If not in memoryFragmentsToCheck, the cell is of an unknown type.
            defaults().add(agentState, cell);
        }
    }

    // Get full memory as one string, except the data from the fragments in the blacklist
    public static String getMemory(AgentState agentState) {
        List<String> blacklist = List.of(MemKey.TARGET.toString(), MemKey.RECENTLY_VISITED.toString());
        StringBuilder str = new StringBuilder();
        for (String frag : agentState.getMemoryFragmentKeys()) {
            if (blacklist.contains(frag)) continue;
            str.append(agentState.getMemoryFragment(frag));
        }
        return str.toString();
    }

    // Removes data from all fragments except the ones in the blacklist
    public static void removeFromMemory(AgentState agentState, String toRemove) {
        List<String> blacklist = List.of(MemKey.TARGET.toString(), MemKey.RECENTLY_VISITED.toString());
        for (String frag : agentState.getMemoryFragmentKeys()) {
            if (blacklist.contains(frag)) continue;
            String rawData = agentState.getMemoryFragment(frag);
            String newMemory = rawData.replace(toRemove, "");
            agentState.addMemoryFragment(frag, newMemory);
        }
    }

    // Gets the target of an agent
    public static int[] getTarget(AgentState agentState) {
        String[] target = MemoryFragment.decode(agentState.getMemoryFragment(MemKey.TARGET.name()));
        return target == null ? null : new int[]{Integer.parseInt(target[0]), Integer.parseInt(target[1])};
    }

    // Sets the target of an agent
    public static void setTarget(AgentState agentState, CellPerception cell) {
        setTarget(agentState, new int[]{ cell.getX(), cell.getY() });
    }
    // Sets the target of an agent
    public static void setTarget(AgentState agentState, int[] loc) {
        agentState.addMemoryFragment(MemKey.TARGET.name(), defaults().encode(loc));
    }

    // Checks if the agent has a destination of a certain color in memory.
    public static boolean knowsDestOf(AgentState agentState, Color color) {
        List<String[]> storedDests = destinations().getAllStored(agentState);
        for (String[] dest : storedDests) {
            if (MyColor.getName(color).equals(dest[2])) return true;
        }
        return false;
    }

    // Print memory of agent for testing
    public static synchronized void printMemory(AgentState agentState) {
        System.out.println("Memory of agent " + agentState.getName() + ":");
        for (String key : agentState.getMemoryFragmentKeys()) {
            System.out.println(key + ": " + agentState.getMemoryFragment(key));
        }
        System.out.println("---------- \n");
    }
}

