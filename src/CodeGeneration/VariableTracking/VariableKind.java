package CodeGeneration.VariableTracking;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import SyntaxAnalysis.KeywordType;

public enum VariableKind {
    //UNDEF,
    STATIC, FIELD, ARG, VAR;

    public static VariableKind fromKeyword(KeywordType keyword) {
        switch(keyword) {
            case STATIC: return STATIC;
            case FIELD:  return FIELD;
            //case ARGUMENT:  return ARGUMENT;
            case VAR:    return VAR;
            default:
                throw new IllegalArgumentException(
                    "Unable to convert KeywordType " + keyword + " into a VariableKind"
                );
        }
    }
}
