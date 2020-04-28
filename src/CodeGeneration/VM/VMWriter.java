package CodeGeneration.VM;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import java.io.PrintStream;

public class VMWriter {
    private PrintStream writer;

    public VMWriter(PrintStream writer) {
        this.writer = writer;
    }

    private void write(String s) {
        writer.println(s);
    }

    public void writePush(Segment seg, int index) {
        write("push " + seg.toString().toLowerCase() + " " + index);
    }

    public void writePop(Segment seg, int index) {
        write("pop " + seg.toString().toLowerCase() + " " + index);
    }

    public void writeArithmetic(ArithmeticCommand cmd) {
        write(cmd.toString().toLowerCase());
    }

    public void writeLabel(String label) {
        write("label " + label);
    }

    public void writeGoto(String label) {
        write("goto " + label);
    }

    public void writeIf(String label) {
        write("if-goto " + label);
    }

    public void writeCall(String name, int nArgs) {
        write("call " + name + " " + nArgs);
    }

    public void writeFunction(String name, int nLocals) {
        write("function " + name + " " + nLocals);
    }

    public void writeReturn() {
        write("return");
    }
}
