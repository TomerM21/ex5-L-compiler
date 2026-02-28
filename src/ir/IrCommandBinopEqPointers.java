package ir;

import temp.Temp;
import java.util.HashSet;
import java.util.Set;

public class IrCommandBinopEqPointers extends IrCommand {

    public Temp dst;
    public Temp t1;
    public Temp t2;

    public IrCommandBinopEqPointers(Temp dst, Temp t1, Temp t2) {
        this.dst = dst;
        this.t1 = t1;
        this.t2 = t2;
    }

    @Override
    public Set<String> getReadTemps() {
        Set<String> s = new HashSet<>();
        s.add("Temp_" + t1.getSerialNumber());
        s.add("Temp_" + t2.getSerialNumber());
        return s;
    }

    @Override
    public Set<String> getWriteTemps() {
        Set<String> s = new HashSet<>();
        s.add("Temp_" + dst.getSerialNumber());
        return s;
    }
}