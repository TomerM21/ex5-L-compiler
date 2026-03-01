package cfg;

import ir.IrCommand;
import ir.IrCommandLabel;
import java.util.*;

/**
 * Computes liveness for temporaries (Temp_N) within one function's IR segment.
 * Named variables are accessed via lw/sw and do not need register allocation.
 */
public class LivenessAnalysis {
    private List<IrCommand> commands;
    // per-instruction live_out sets (index matches commands list)
    private List<Set<String>> liveOut;

    public LivenessAnalysis(List<IrCommand> commands) {
        this.commands = commands;
    }

    public void compute() {
        // 1. Build basic blocks from commands (reuse Fullcfggraph partitioning logic)
        // 2. Compute use/def per block (temps only — filter out names not starting with "Temp_")
        // 3. Backward dataflow: live_in[B] = use[B] ∪ (live_out[B] − def[B])
        //                       live_out[B] = ∪ live_in[succ(B)]
        // 4. Iterate until stable (worklist)
        // 5. Reconstruct per-instruction live_out by backwards simulation within each block
    }

    /** Returns live_out set for command at index i. Call after compute(). */
    public Set<String> getLiveOutAt(int i) {
        return liveOut.get(i);
    }

    public List<IrCommand> getCommands() { return commands; }
}