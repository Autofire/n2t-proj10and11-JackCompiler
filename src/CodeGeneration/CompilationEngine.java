package CodeGeneration;

import SyntaxAnalysis.JackTokenizer;
import SyntaxAnalysis.KeywordType;
import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.security.Key;
import java.util.List;
import java.util.Vector;

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
        // According to the Jack specs, every file must have exactly
        // one class. Thus, we'll make the specific assumption that
        // the first tokens are for a class definition.
        compileClass();

        // Each file is supposed to be exactly one class, so if
        // we're done compiling the class and there's more tokens,
        // something's wrong.
        if(tokenizer.hasMoreTokens()) {
            throw new IllegalSyntaxException( tokenizer.getLineNumber(), "Found symbols after class definition" );
        }

    }

    private static <T> T safeCast(Token token, Class<T> type) {
        return (type.isInstance(token) ? (T) token : null);
    }

    private <T extends Token> T getOrDie(Class<T> type) {
        Token rawToken = tokenizer.next();
        T castToken = safeCast(rawToken, type);

        if(castToken == null) {
            throw new IllegalSyntaxException(rawToken.getLineNumber(), "Bad token: " + rawToken.toXML());
        }

        return castToken;
    }

    private void compileClass() {
        // Get tokens
        KeywordToken classToken = getOrDie(KeywordToken.class);
        IdentifierToken className = getOrDie(IdentifierToken.class);
        SymbolToken openCurly = getOrDie(SymbolToken.class);

        if(classToken.getValue() == KeywordType.CLASS &&
           openCurly.getValue() == '{')
        {
            //region GEN Opening class code
            write("<class>");
            indentLevel++;

            write(classToken.toXML());
            write(className.toXML());
            write(openCurly.toXML());
            //endregion

            boolean done = false;
            while(!done && tokenizer.hasMoreTokens()) {
                Token nextToken = tokenizer.peek();
                KeywordToken keyword = safeCast(nextToken, KeywordToken.class);
                SymbolToken symbol = safeCast(nextToken, SymbolToken.class);

                if(symbol != null && symbol.getValue() == '}') {

                    //region GEN Closing class code
                    tokenizer.next();
                    write(nextToken.toXML());

                    indentLevel--;
                    write("</class>");
                    //endregion

                    done = true;
                }
                else if(keyword != null) {
                    if(keyword.getValue() == KeywordType.STATIC ||
                       keyword.getValue() == KeywordType.FIELD) {
                        compileClassVarDec();
                    }
                    else if(keyword.getValue() == KeywordType.CONSTRUCTOR ||
                            keyword.getValue() == KeywordType.FUNCTION ||
                            keyword.getValue() == KeywordType.METHOD) {
                        compileSubroutine();
                    }
                }
                else {
                    write( nextToken.toXML() + "(Unsupported)");
                    tokenizer.next();   // Normally the child function consume this
                }
            }

            if(!done && !tokenizer.hasMoreTokens()) {
                throw new IllegalSyntaxException(tokenizer.getLineNumber(), "Unexpected end of file");
            }

        }
        else {
            throw new IllegalSyntaxException(classToken.getLineNumber(), "Malformed class definition");
        }
    }

    private void compileClassVarDec() {
        // Figure out the locality.
        KeywordToken locality = getOrDie(KeywordToken.class);
        if(locality.getValue() != KeywordType.STATIC && locality.getValue() != KeywordType.FIELD) {
            throw new IllegalSyntaxException(
                    locality.getLineNumber(),
                    "Class variable declarations must be 'static' or 'field'"
            );
        }

        // Next, figure out the type.
        Token varType = tokenizer.next();
        KeywordToken varTypeAsKeyword = safeCast(varType, KeywordToken.class);
        IdentifierToken varTypeAsIdentifier = safeCast(varType, IdentifierToken.class);
        String typeName;
        if(varTypeAsKeyword != null) {
            if(varTypeAsKeyword.getValue() != KeywordType.INT &&
               varTypeAsKeyword.getValue() != KeywordType.CHAR &&
               varTypeAsKeyword.getValue() != KeywordType.BOOLEAN) {

                throw new IllegalSyntaxException(
                        varType.getLineNumber(),
                        "Cannot use keyword " + varTypeAsKeyword.getValue().toString() + " as a type"
                );
            }
            else {
                typeName = varTypeAsKeyword.getValue().toString().toLowerCase();
            }
        }
        else if(varTypeAsIdentifier != null) {
            typeName = varTypeAsIdentifier.getValue();
        }
        else {
            throw new IllegalSyntaxException(
                    varType.getLineNumber(),
                    "Illegal type " + varType.toString()
            );
        }

        // Now we'll get the variable names. We MUST see at least one
        // name, but we might see more after that. It'll be in the
        // format of "name, name, name, ..., name;"
        //
        // Thus, we read the first name, and then repeat as long
        // as we see commas. If we see anything else, we stop and
        // make sure it's a semicolon.
        List<IdentifierToken> names = new Vector<>();
        names.add(getOrDie(IdentifierToken.class));

        SymbolToken symbolAfterName = getOrDie(SymbolToken.class);
        while(symbolAfterName.getValue() == ',') {
            names.add(getOrDie(IdentifierToken.class));
            symbolAfterName = getOrDie(SymbolToken.class);
        }

        if(symbolAfterName.getValue() != ';') {
            throw new IllegalSyntaxException(
                    symbolAfterName.getLineNumber(),
                    "Unexpected symbol: " + symbolAfterName.getValue()
            );
        }


        // Alright, we have everything we need now.
        // region GEN Variable declaration code
        write("<classVarDec>");
        indentLevel++;

        write(locality.toXML());
        write(varType.toXML());
        write(names.get(0).toXML());
        for(int i = 1; i < names.size(); i++) {
            write(new SymbolToken(-1, ",").toXML());
            write(names.get(i).toXML());
        }
        write(new SymbolToken(-1, ";").toXML());

        indentLevel--;
        write("</classVarDec>");
        // endregion

    }

    private void compileSubroutine() {
        throw new RuntimeException();
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
