package CodeGeneration.VariableTracking;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

public class Variable {
    String name;
    String type;
    VariableKind kind;
    int index;

    // Yes, we WANT this to not be public. We only want the variable table
    // to generate these objects.
    Variable(String name, String type, VariableKind kind, int index) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public VariableKind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    public String toString() {
        return name + " (" +
               "type: " + type + ", " +
               "kind: " + kind.toString().toLowerCase() + ", " +
               "index: " + index +
               ") ";
    }

    public String toXML() {
        return toXML("");
    }

    public String toXML(String note) {
        return "<identifier> " + toString() + note + " </identifier>";
    }
}
