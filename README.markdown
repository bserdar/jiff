# Jiff - JSON diff

Finds the differences between two JSON documents.

## Command line

```
  java -jar jiff.jar [options] file1 file2
```

Options:
  -a1 : Array ordering significant
  -a0 : Array ordering not significant (default)
  -p1 : Return deltas for parents
  -p0 : Return deltas for leaves only (default)

## Java

```
   JsonDiff diff=new JsonDiff(JsonDiff.Option.ARRAY_ORDER_SIGNIFICANT,
                              JsonDiff.Option.RETURN_LEAVES_ONLY);
   List<JsonDelta> delta=diff.computeDiff(jsonDoc1,jsonDoc2);
   for(JsonDelta fieldDifference:delta) {
      ...
   }
```

## Options:

 * ARRAY_ORDER_SIGNIFICANT:
```
    [1,2,3] != [1,3,2]
    
```

 * ARRAY_ORDER_INSIGNIFICANT:
```
    [1,2,3] == [1,3,2]
```

 * RETURN_PARRENT_DIFFS: When this option is set, a delta is returned
 for every different node, and its ancestors. For instance:
```
  { a:{ x:1, y:2} }
  { a:{ x:2, y:2} }
```
 Comparing these two will return three deltas, one for a.x, a, and root.

 * RETURN LEAVES ONLY: When this option set, a delta is returned only
   for the different nodes, and not their ancestors. If an array node
   has different number of elements in two nodes, the array node will
   be returned as a delta.
  