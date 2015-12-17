package jiff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Represents a difference between two instances of a field
 *
 * @author bserdar
 */
public class JsonDelta {

    private final String field;
    private final ContainerNode parent1;
    private final JsonNode node1;
    private final ContainerNode parent2;
    private final JsonNode node2;

    public JsonDelta(String field,
                     ContainerNode parent1,
                     JsonNode node1,
                     ContainerNode parent2,
                     JsonNode node2) {
        this.field=field;
        this.parent1=parent1;
        this.node1=node1;
        this.parent2=parent2;
        this.node2=node2;
    }

    public String getField() {
        return field;
    }

    public ContainerNode getParent1() {
        return parent1;
    }

    public JsonNode getNode1() {
        return node1;
    }

    public ContainerNode getParent2() {
        return parent2;
    }

    public JsonNode getNode2() {
        return node2;
    }

    @Override
    public String toString() {
        return field+"("+describe(node1)+" != "+ describe(node2)+")";
    }

    private String describe(JsonNode node) {
        if(node==null)
            return "null";
        else if(node instanceof ObjectNode)
            return "ObjectNode";
        else if(node instanceof ArrayNode)
            return "ArrayNode";
        else
            return node.toString();
    }

}
