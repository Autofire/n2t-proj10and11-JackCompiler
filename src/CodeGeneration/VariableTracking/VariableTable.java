package CodeGeneration.VariableTracking;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import SyntaxAnalysis.Tokens.IdentifierToken;

import java.util.HashMap;
import java.util.Map;

public class VariableTable {

    //private Map<String, Variable> classVariables;
    //private Map<String, Variable> subroutineVariables;

    private Map<VariableKind, Map<String, Variable>> variables;

    public VariableTable() {
        //classVariables = new HashMap<>();
        variables = new HashMap<>();
        variables.put(VariableKind.STATIC, new HashMap<>());
        variables.put(VariableKind.FIELD, new HashMap<>());
    }

    public void startSubroutine() {
        //subroutineVariables = new HashMap<>();
        variables.put(VariableKind.ARG, new HashMap<>());
        variables.put(VariableKind.VAR, new HashMap<>());
    }

    public Variable define(IdentifierToken token, String type, VariableKind kind) {
        return define(token.getValue(), type, kind);
    }

    public Variable define(String name, String type, VariableKind kind) {
        if(get(name) != null) {
            throw new IllegalArgumentException("There's already a variable with the name " + name);
        }
        else {
            Map<String, Variable> targetMap = variables.get(kind);
            int index = varCount(kind);
            Variable newVar = new Variable(name, type, kind, index);
            targetMap.put(name, newVar);

            return newVar;
        }
    }

    public int varCount(VariableKind kind) {
        return variables.get(kind).size();
    }

    public Variable get(IdentifierToken token) {
        return get(token.getValue());
    }

    public Variable get(String name) {
        for(Map<String, Variable> subTable : variables.values()) {
            Variable result = subTable.get(name);

            if(result != null) {
                return result;
            }
        }

        return null;
    }

}
