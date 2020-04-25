package CodeGeneration;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import SyntaxAnalysis.Tokens.Token;

import java.util.InputMismatchException;

public class IllegalSyntaxException extends InputMismatchException {
    private int lineNumber;

    public IllegalSyntaxException() {
        this(-1);
    }

    public IllegalSyntaxException(int lineNumber) {
        this(lineNumber, "");
    }

    public IllegalSyntaxException(int lineNumber, String s) {
        super("(Line " + lineNumber + ") " + s);

        this.lineNumber = lineNumber;
    }

    public IllegalSyntaxException(Token t, String s) {
        this(t.getLineNumber(), s);
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
