package SyntaxAnalysis.Tokens;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

public class SymbolToken extends AbstractToken {

    public static final String BINARY_OPERANDS = "+-*/&|<>=";
    public static final String UNARY_OPERANDS = "~-";

    public static final String ALL_SYMBOLS = "{}()[].,;+-*/&|<>=~";
    public static boolean isValid(char value) {
        return ALL_SYMBOLS.contains(Character.toString(value));
    }
    public static boolean isValid(String value) {
        return value.length() == 1 && isValid(value.charAt(0));
    }

    private char symbol;

    public SymbolToken(int line, String value) {
        super(line);

        if(isValid(value)) {
            symbol = value.charAt(0);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public char getValue() {
        return symbol;
    }

    public boolean isBinaryOp() {
        return BINARY_OPERANDS.contains(Character.toString(symbol));
    }

    public boolean isUnaryOp() {
        return UNARY_OPERANDS.contains(Character.toString(symbol));
    }

    @Override
    public String toString() {
        return Character.toString(symbol);
    }

    @Override
    public String toXML() {
        String xmlOutput;

        switch(symbol) {
            case '<':
                xmlOutput = "&lt;";
                break;
            case '>':
                xmlOutput = "&gt;";
                break;
            case '&':
                xmlOutput = "&amp;";
                break;
            default:
                xmlOutput = Character.toString(symbol);
        }

        return "<symbol> " + xmlOutput + " </symbol>";
    }
}
