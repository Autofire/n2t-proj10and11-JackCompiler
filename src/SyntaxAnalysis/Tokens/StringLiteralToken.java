package SyntaxAnalysis.Tokens;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

public class StringLiteralToken extends AbstractToken {

    public static boolean isValid(String value) {
        return value.matches("^\".*\"$") && !value.endsWith("\\\"");
    }

    private String value;
    public StringLiteralToken(int line, String value) {
        super(line);

        if(isValid(value)) {
            this.value = value.substring(1, value.length()-1);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String toXML() {
        return "<stringConstant> " + value + " </stringConstant>";
    }
}
