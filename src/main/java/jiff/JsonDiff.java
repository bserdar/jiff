package jiff;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * Compares two json documents and reports differences
 *
 * @author bserdar
 */
public class JsonDiff {

    /**
     * Comparison options
     */
    public static enum Option {
        /**
         * If set, array ordering is significant. That is, [1,2,3] != [1,3,2]. This is the default.
         */
        ARRAY_ORDER_SIGNIFICANT,

        /**
         * If set, array ordering is not significant. That is, [1,2,3] == [1,3,2]
         */
        ARRAY_ORDER_INSIGNIFICANT,

        /**
         * If set, a difference in a field will also be recorded as a difference in the object containing it. That is:
         * <pre>
         *   { a:{ x:1, y:2} }
         *   { a:{ x:2, y:2} }
         * </pre>
         * will report two differences, one for "a", and one for "a.x"
         */
        RETURN_PARENT_DIFFS,
        
        /**
         * If set, a difference in a field will also be recorded for that field, but not its parent. That is:
         * <pre>
         *   { a:{ x:1, y:2} }
         *   { a:{ x:2, y:2} }
         * </pre>
         * will report one difference, "a". In comparing arrays, if array sizes differ, the array field will be 
         * recorded as a difference.
         */
        RETURN_LEAVES_ONLY
    }

    
    private JsonComparator objectComparator=new DefaultObjectNodeComparator();
    private JsonComparator arrayComparator=new DefaultArrayNodeComparator();
    private JsonComparator valueComparator=new DefaultValueNodeComparator();

    private boolean returnParentDiffs=false;

    private Filter filter=INCLUDE_ALL;
    
    private static final Filter INCLUDE_ALL=new Filter() {
            @Override public boolean includeField(List<String> field) {return true;}
        };
    
    private static final JsonComparator NODIFF_CMP=new JsonComparator() {
            @Override
            public boolean compare(List<JsonDelta> delta,List<String> context,JsonNode node1,JsonNode node2) {return false;}
        };
    
    /**
     * Recursively compares the fields in two objects
     */
    public class DefaultObjectNodeComparator implements JsonComparator {
        @Override
        public boolean compare(List<JsonDelta> delta,
                               List<String> context,
                               JsonNode node1,
                               JsonNode node2) {
            boolean ret=false;
            int ctxn=context.size();
            context.add("");
            for(Iterator<Map.Entry<String,JsonNode> > itr=node1.fields();itr.hasNext();) {
                Map.Entry<String,JsonNode> entry=itr.next();
                String field=entry.getKey();
                context.set(ctxn,field);
                JsonNode node1Value=entry.getValue();
                JsonNode node2Value=node2.get(field);
                if(computeDiff(delta,context,node1Value,node2Value))
                    ret=true;
            }
            // Are there any nodes that are in node2, but not in node1?
            for(Iterator<String> node2Names=node2.fieldNames();node2Names.hasNext();) {
                String field=node2Names.next();
                context.set(ctxn,field);
                if(filter.includeField(context)) {
                    if(!node1.has(field)) {
                        delta.add(new JsonDelta(JsonDiff.toString(context),null,node2.get(field)));
                        ret=true;
                    }
                }
            }
            if(ret&&returnParentDiffs)
                delta.add(new JsonDelta(JsonDiff.toString(context),node1,node2));
            context.remove(ctxn);
            return ret;
        }
    }

    /**
     * Recursively compares two arrays. Expects to see the elements in the same order
     */
    public class DefaultArrayNodeComparator implements JsonComparator {
        @Override
        public boolean compare(List<JsonDelta> delta,
                               List<String> context,
                               JsonNode node1,
                               JsonNode node2) {
            boolean ret=false;
            int n=Math.min(node1.size(),node2.size());
            int ctxn=context.size();
            context.add("");
            for(int i=0;i<n;i++) {
                context.set(ctxn,Integer.toString(i));
                JsonNode node1Value=node1.get(i);
                JsonNode node2Value=node2.get(i);
                if(computeDiff(delta,context,node1Value,node2Value))
                    ret=true;
            }
            context.remove(ctxn);
            if((ret&&returnParentDiffs)||node1.size()!=node2.size()) {
                delta.add(new JsonDelta(JsonDiff.toString(context),node1,node2));
                ret=true;
            }
            return ret;
        }
    }

    /**
     * Creates a hash for a json node. The hash is a weak hash, but insensitive to element order.
     */
    private  class HashedNode {
        private final JsonNode node;
        private final Long hash;
        private final int index;
        
        public HashedNode(JsonNode node,int index,List<String> context) {
            this.node=node;
            this.index=index;
            int ctxn=context.size();
            context.add(Integer.toString(index));
            hash=computeHash(node,context);
            context.remove(ctxn);
        }
        
        public JsonNode getNode() {
            return node;
        }

        public Long getHash() {
            return hash;
        }

        public int getIndex() {
            return index;
        }

        private long computeHash(JsonNode node,List<String> context) {
            long hash=0;
            if(filter.includeField(context)) {
                if(node instanceof ValueNode) {
                    return node.hashCode();
                } else if(node==null) {
                    hash=0;
                } else if(node instanceof NullNode) {
                    hash=1;
                } else if(node instanceof ObjectNode) {
                    hash=0;
                    int ctxn=context.size();
                    context.add("");
                    for(Iterator<Map.Entry<String,JsonNode>> itr=node.fields();itr.hasNext();) {
                        Map.Entry<String,JsonNode> entry=itr.next();
                        context.set(ctxn,entry.getKey());
                        if(filter.includeField(context)) {
                        	hash+=entry.getKey().hashCode();
                        	hash+=computeHash(entry.getValue(),context);
                        }
                    }
                    context.remove(ctxn);
                } else if(node instanceof ArrayNode) {
                    hash=0;
                    int i=0;
                    int ctxn=context.size();
                    context.add("");
                    for(Iterator<JsonNode> itr=node.elements();itr.hasNext();i++) {
                        context.set(ctxn,Integer.toString(i));
                        hash+=computeHash(itr.next(),context);
                    }
                    context.remove(ctxn);
                } else {
                    hash=node.hashCode();
                }
            }
            return hash;
        }

    }
    
    /**
     * Recursively compares two arrays, treating then as sets (no element order requirement)
     */
    public class SetArrayNodeComparator implements JsonComparator {
        @Override
        public boolean compare(List<JsonDelta> delta,
                               List<String> context,
                               JsonNode node1,
                               JsonNode node2) {
            boolean ret=false;

            int index=0;
            Map<Long,List<HashedNode>> node1Elements=new HashMap<>();
            for(Iterator<JsonNode> itr=node1.elements();itr.hasNext();index++) {
                put(node1Elements,new HashedNode(itr.next(),index,context));
            }
            index=0;
            Map<Long,List<HashedNode>> node2Elements=new HashMap<>();
            for(Iterator<JsonNode> itr=node2.elements();itr.hasNext();index++) {
                put(node2Elements,new HashedNode(itr.next(),index,context));
            }

            List<HashedNode> removed=new ArrayList<>(node1Elements.size());
            for(Map.Entry<Long,List<HashedNode>> entry:node1Elements.entrySet()) {
                List<HashedNode> node1Els=entry.getValue();
                for(HashedNode node1Element:node1Els) {
                    if(!findAndRemove(node2Elements,node1Element,context)) {
                        delta.add(new JsonDelta(JsonDiff.toString(context,Integer.toString(node1Element.getIndex())),node1Element.getNode(),null));
                        ret=true;
                    } else {
                        removed.add(node1Element);
                    }
                }
            }
            for(HashedNode x:removed)
                remove(node1Elements,x);
            
            for(Map.Entry<Long,List<HashedNode>> entry:node2Elements.entrySet()) {
                List<HashedNode> node2Els=entry.getValue();
                for(HashedNode node2Element:node2Els) {
                    if(!findAndRemove(node1Elements,node2Element,context)) {
                        delta.add(new JsonDelta(JsonDiff.toString(context,Integer.toString(node2Element.getIndex())),null, node2Element.getNode()));
                        ret=true;
                    }
                }
             }
            
            if(ret||node1.size()!=node2.size()) {
                if(returnParentDiffs)
                    delta.add(new JsonDelta(JsonDiff.toString(context),node1,node2));
                ret=true;
            }
            return ret;
        }

        private void put(Map<Long,List<HashedNode>> map,HashedNode node) {
            List<HashedNode> list=map.get(node.getHash());
            if(list==null)
                map.put(node.getHash(),list=new ArrayList<>());
            list.add(node);
        }

        private void remove(Map<Long,List<HashedNode>> map,HashedNode node) {
            List<HashedNode> list=map.get(node.getHash());
            if(list!=null)
                list.remove(node);
        }
            
        private boolean findAndRemove(Map<Long,List<HashedNode>> elements,HashedNode elem,List<String> context) {
            List<HashedNode> nodes=elements.get(elem.getHash());
            if(nodes==null)
                return false;
            List<JsonDelta> delta=new ArrayList<>();
            int nctx=context.size();
            context.add("");
            int n=nodes.size();
            for(int i=0;i<n;i++) {
                HashedNode node=nodes.get(i);
                context.set(nctx,Integer.toString(elem.index));
                if(!computeDiff(delta,context,node.getNode(),elem.getNode())) {
                    nodes.remove(i);
                    context.remove(nctx);
                    return true;
                }
            }
            context.remove(nctx);
            return false;
        }
    }

    /**
     * Compares two value nodes
     */
    public class DefaultValueNodeComparator implements JsonComparator {
        @Override
        public boolean compare(List<JsonDelta> delta,
                               List<String> context,
                               JsonNode node1,
                               JsonNode node2) {
            if(node1.isValueNode()&&node2.isValueNode()) {
                if(node1.isNumber()&&node2.isNumber()) {
                    if(!node1.asText().equals(node2.asText())) {
                        delta.add(new JsonDelta(JsonDiff.toString(context),node1,node2));
                        return true;
                    }
                } else if(!node1.equals(node2)) {
                    delta.add(new JsonDelta(JsonDiff.toString(context),node1,node2));
                    return true;
                }
            } else  if(!node1.equals(node2)) {
                delta.add(new JsonDelta(JsonDiff.toString(context),node1,node2));
                return true;
            }
            return false;
        }
    }

    public JsonDiff() {}

    public JsonDiff(Option...options) {
        for(Option x:options)
            setOption(x);
    }
    
    /**
     * Sets an option
     */
    public void setOption(Option option) {
        switch(option) {
        case ARRAY_ORDER_SIGNIFICANT: arrayComparator=new DefaultArrayNodeComparator();break;
        case ARRAY_ORDER_INSIGNIFICANT: arrayComparator=new SetArrayNodeComparator();break;
        case RETURN_PARENT_DIFFS: returnParentDiffs=true;break;
        case RETURN_LEAVES_ONLY: returnParentDiffs=false;break;
        }
            
    }

    /**
     * Sets a filter that determines whether to include fields in comparison
     */
    public void setFilter(Filter f) {
        this.filter=f;
    }

    /**
     * Computes the difference of two JSON strings, and returns the differences
     */
    public List<JsonDelta> computeDiff(String node1,String node2) throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        return computeDiff(mapper.readTree(node1),mapper.readTree(node2));
    }

    /**
     * Computes the difference of two JSON nodes and returns the differences
     */
    public List<JsonDelta> computeDiff(JsonNode node1,JsonNode node2) {
        List<JsonDelta> list=new ArrayList<>();
        computeDiff(list,new ArrayList<String>(),node1,node2);
        return list;
    }

    /**
     * Returns true if there is a difference
     */
    public  boolean computeDiff(List<JsonDelta> delta,List<String> context,JsonNode node1,JsonNode node2) {
        boolean ret=false;
        if(context.size()==0||filter.includeField(context)) {
            JsonComparator cmp=getComparator(context,node1,node2);
            if(cmp!=null)
                ret=cmp.compare(delta,context,node1,node2);
            else {
                delta.add(new JsonDelta(toString(context),node1,node2));
                ret=true;
            }
        }
        return ret;
    }

    /**
     * Returns the comparator for the give field, and nodes. This
     * method can be overriden to customize comparison logic.
     */
    public JsonComparator getComparator(List<String> context,
                                        JsonNode node1,
                                        JsonNode node2) {
        if(node1==null) {
            if(node2==null) {
                return NODIFF_CMP;
            } else {
                return null;
            }
        } else if(node2==null) {
            return null;
        } else {
            if(node1 instanceof NullNode) {
                if(node2 instanceof NullNode) {
                    return NODIFF_CMP;
                } else {
                    return null;
                }
            } else if(node2 instanceof NullNode) {
                return null;
            }
            // Nodes are not null, and they are not null node
            if(node1.isContainerNode()&&node2.isContainerNode()) {
                if(node1 instanceof ObjectNode)
                    return objectComparator;
                else if(node1 instanceof ArrayNode)
                    return arrayComparator;
            } else if(node1.isValueNode()&&node2.isValueNode()) {
                    return valueComparator;
            } 
        }
        return null;
    }

    public static String toString(List<String> list) {
        return toString(list,null);
    }
    
    public static String toString(List<String> list,String addtn) {
        StringBuilder bld=new StringBuilder();
        boolean first=true;
        for(String s:list) {
            if(first)
                first=false;
            else
                bld.append('.');
            bld.append(s);
        }
        if(addtn!=null) {
            if(!first)
                bld.append('.');
            bld.append(addtn);
        }
        return bld.toString();
    }
}

