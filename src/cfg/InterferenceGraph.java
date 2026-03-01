package cfg;

import java.util.*;

public class InterferenceGraph {
    // adjacency set: node → set of interfering nodes
    private Map<String, Set<String>> adj = new HashMap<>();

    public void addNode(String temp) {
        adj.putIfAbsent(temp, new HashSet<>());
    }

    public void addEdge(String a, String b) {
        if (a.equals(b)) return;
        adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    public Set<String> neighbors(String node) {
        return adj.getOrDefault(node, Collections.emptySet());
    }

    public int degree(String node) {
        return neighbors(node).size();
    }

    public Set<String> nodes() {
        return adj.keySet();
    }

    /** Build from liveness analysis results. */
    public static InterferenceGraph build(LivenessAnalysis la) {
        InterferenceGraph g = new InterferenceGraph();
        List<ir.IrCommand> cmds = la.getCommands();
        for (int i = 0; i < cmds.size(); i++) {
            ir.IrCommand c = cmds.get(i);
            Set<String> written = c.getWriteTemps();
            Set<String> liveAfter = la.getLiveOutAt(i);
            for (String def : written) {
                g.addNode(def);
                for (String live : liveAfter) {
                    if (!live.equals(def)) {
                        g.addEdge(def, live);
                    }
                }
            }
            // ensure all read temps are nodes too
            for (String r : c.getReadTemps()) g.addNode(r);
        }
        return g;
    }
}