import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

public class VMWriter {

    private PrintWriter mPrintWriter;

    private HashMap<Segment, String> mSegmentStringHashMap = new HashMap<>();
    private HashMap<Command, String> mCommandStringHashMap = new HashMap<>();

    public enum Segment {
        CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP
    }
    public enum Command {
        ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
    }

    public VMWriter(File outFile) throws FileNotFoundException {
        mPrintWriter = new PrintWriter(outFile);

        mSegmentStringHashMap.put(Segment.CONST, "constant");
        mSegmentStringHashMap.put(Segment.ARG, "argument");
        mSegmentStringHashMap.put(Segment.LOCAL, "local");
        mSegmentStringHashMap.put(Segment.STATIC, "static");
        mSegmentStringHashMap.put(Segment.THIS, "this");
        mSegmentStringHashMap.put(Segment.THAT, "that");
        mSegmentStringHashMap.put(Segment.POINTER, "pointer");
        mSegmentStringHashMap.put(Segment.TEMP, "temp");

        mCommandStringHashMap.put(Command.ADD, "add");
        mCommandStringHashMap.put(Command.SUB, "sub");
        mCommandStringHashMap.put(Command.NEG, "neg");
        mCommandStringHashMap.put(Command.EQ, "eq");
        mCommandStringHashMap.put(Command.GT, "gt");
        mCommandStringHashMap.put(Command.LT, "lt");
        mCommandStringHashMap.put(Command.AND, "and");
        mCommandStringHashMap.put(Command.OR, "or");
        mCommandStringHashMap.put(Command.NOT, "not");
    }

    public void writePush(Segment segment, int index) {
        mPrintWriter.print("push " + mSegmentStringHashMap.get(segment) + " " + index + "\n");
    }

    public void writePop(Segment segment, int index) {
        mPrintWriter.print("pop " + mSegmentStringHashMap.get(segment) + " " + index + "\n");
    }

    public void writeArithmetic(Command command) {
        mPrintWriter.print(mCommandStringHashMap.get(command) + "\n");
    }

    public void writeLabel(String label) {
        mPrintWriter.print("label " + label + "\n");
    }

    public void writeGoto(String label) {
        mPrintWriter.print("goto " + label + "\n");
    }

    public void writeIf(String label) {
        mPrintWriter.print("if-goto " + label + "\n");
    }

    public void writeCall(String name, int nArgs) {
        mPrintWriter.print("call " + name + " " + nArgs + "\n");
    }

    public void writeFunction(String name, int nLocals) {
        mPrintWriter.print("function " + name + " " + nLocals + "\n");
    }

    public void writeReturn() {
        mPrintWriter.print("return\n");
    }

    public void printLine(String string) {
        mPrintWriter.print(string + "\n");
    }

    public void close() {
        mPrintWriter.close();
    }
}
