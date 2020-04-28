package CodeGeneration.VM;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import SyntaxAnalysis.Tokens.SymbolToken;

public enum ArithmeticCommand {
    ADD, SUB,
    EQ,  GT, LT,
    AND, OR,
    MULTIPLY, DIVIDE,
    NOT, NEG;

    public static ArithmeticCommand fromBinarySymbol(SymbolToken token) {
        return fromBinarySymbol(token.getValue());
    }

    public static ArithmeticCommand fromBinarySymbol(char symbol) {
        switch(symbol) {
            case '+': return ADD;
            case '-': return SUB;
            case '=': return EQ;
            case '>': return GT;
            case '<': return LT;
            case '&': return AND;
            case '|': return OR;
            case '*': return MULTIPLY;
            case '/': return DIVIDE;
            default:
                throw new IllegalArgumentException("Cannot convert " + symbol + " into a command");
        }
    }

    public static ArithmeticCommand fromUnarySymbol(SymbolToken token) {
        return fromUnarySymbol(token.getValue());
    }

    public static ArithmeticCommand fromUnarySymbol(char symbol) {
        switch(symbol) {
            case '-': return NEG;
            case '~': return NOT;
            default:
                throw new IllegalArgumentException("Cannot convert " + symbol + " into a command");
        }
    }
}
