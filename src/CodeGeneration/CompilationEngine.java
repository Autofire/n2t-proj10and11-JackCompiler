package CodeGeneration;

import SyntaxAnalysis.JackTokenizer;
import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.InvalidObjectException;
import java.io.PrintStream;
import java.util.InputMismatchException;

public class CompilationEngine {

    //private BufferedReader reader;
    private JackTokenizer tokenizer;
    private PrintStream writer;

    private final static String INDENT = "  ";
    private int indentLevel = 0;

    public CompilationEngine(BufferedReader reader, PrintStream writer) {
        //this.reader = reader;
        tokenizer = new JackTokenizer(reader);
        this.writer = writer;
    }

    private String getIndent() {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }

        return sb.toString();
    }

    private void write(String value) {
        writer.print(getIndent());
        writer.println(value);
    }

    public void compile() {
        try {
            compileClass();
        }
        catch(InputMismatchException e) {
            // TODO Get line number
            throw new InputMismatchException("(Line ???) " + e.getMessage());
        }
    }

    private static <T> T safeCast(Token token, Class<T> type) {
        return (type.isInstance(token) ? (T) token : null);
    }

    private <T extends Token> T getOrDie(Class<T> type) {
        Token rawToken = tokenizer.next();
        T castToken = safeCast(rawToken, type);

        if(castToken == null) {
            throw new InputMismatchException("Bad token: " + rawToken.toXML());
        }

        return castToken;
    }

    private void compileClass() {
        // Get tokens
        KeywordToken classToken = getOrDie(KeywordToken.class);
        IdentifierToken className = getOrDie(IdentifierToken.class);
        SymbolToken openCurly = getOrDie(SymbolToken.class);

        if(classToken.getValue() == KeywordToken.Keyword.CLASS &&
           openCurly.getValue() == '{')
        {
            // TODO Code gen
            write("<class>");
            indentLevel++;

            write(classToken.toXML());
            write(className.toXML());
            write(openCurly.toXML());

            boolean done = false;
            while(!done) {
                Token nextToken = tokenizer.peek();
                KeywordToken keyword = safeCast(nextToken, KeywordToken.class);
                SymbolToken symbol = safeCast(nextToken, SymbolToken.class);

                if(symbol != null && symbol.getValue() == '}') {
                    write(nextToken.toXML());
                    tokenizer.next();
                    done = true;
                }
                else {
                    write(nextToken.toXML() + "(Unsupported)");
                    tokenizer.next();   // Normally the child function consume this
                }
            }

            indentLevel--;
            write("</class>");
        }
        else {
            throw new InputMismatchException("Expected class keyword");
        }
    }

    private void compileClassVarDec() {

    }

    private void compileSubroutine() {

    }

    private void compileParameterList() {

    }

    private void compileVarDec() {

    }

    private void compileStatements() {

    }

    private void compileDo() {

    }

    private void compileLet() {

    }

    private void compileWhile() {

    }

    private void compileReturn() {

    }

    private void compileIf() {

    }

    private void compileExpression() {

    }

    private void compileTerm() {

    }

    private void compileExpressionList() {

    }



}
