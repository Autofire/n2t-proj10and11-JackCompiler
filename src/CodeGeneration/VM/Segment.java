package CodeGeneration.VM;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import CodeGeneration.VariableTracking.VariableKind;

public enum Segment {
    CONSTANT, ARGUMENT, LOCAL, STATIC, THIS, THAT, POINTER, TEMP;

    public static Segment fromVariableKind(VariableKind kind) {
       switch(kind) {
           case STATIC: return STATIC;
           case FIELD:  return THIS;
           case ARG:    return ARGUMENT;
           case VAR:    return LOCAL;
           default:
               throw new IllegalArgumentException("Unsupported variable kind");
       }
    }
}
