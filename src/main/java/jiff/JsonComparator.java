package jiff;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;

/**
 * Implementations of this interface compares two JSON nodes, and
 * records the differences into a differences list
 *
 * @author bserdar
 */
public interface JsonComparator {
    /**
     * Returns true if there is a difference. 
     *
     * @param delta Record differences here
     * @param context The field name being compared
     * @param parent1 The node containing the first node
     * @param node1 First node
     * @param parent2 The node containing the second node
     * @param node2 Second node
     */
    boolean compare(List<JsonDelta> delta,
                    String context,
                    ContainerNode parent1,
                    JsonNode node1,
                    ContainerNode parent2,
                    JsonNode node2);
}
