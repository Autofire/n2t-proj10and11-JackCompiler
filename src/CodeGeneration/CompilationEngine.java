package CodeGeneration;

import SyntaxAnalysis.JackTokenizer;
import SyntaxAnalysis.KeywordType;
import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.PrintStream;
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

    private String getIndent() {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }

        return sb.toString();
    }

    /**
     * Writes XML to the file. This is indented based indentLevel.
     * @param value String to write.
     */
    private void write(String value) {
        writer.print(getIndent());
        writer.println(value);
    }

    /**
     * Writes XML to the file. This is indented based indentLevel.
     * @param t Token to write. t.toXML is called.
     */
    private void write(Token t) {
        write(t.toXML());
    }

    private static <T> T safeCast(Token token, Class<T> type) {
        return (type.isInstance(token) ? (T) token : null);
    }

    /**
     * Grabs the next token from the Tokenizer, consuming it in the process.
     * It ensures that the token is of the given type or it an exception is thrown.
     * @param type The result of doing the token type's static class variable.
     * @param <T> The type of token we require.
     * @return The token, assuming the cast is successful.
     */
    private <T extends Token> T getTokenOrDie(Class<T> type) {
        T castToken = peekTokenOrDie(type);
        tokenizer.next();

        return castToken;
    }

    /**
     * Grabs the next token from the Tokenizer without consuming it.
     * It ensures that the token is of the given type or it an exception is thrown.
     * @param type The result of doing the token type's static class variable.
     * @param <T> The type of token we require.
     * @return The token, assuming the cast is successful.
     */
    private <T extends Token> T peekTokenOrDie(Class<T> type) {
        Token rawToken = tokenizer.peek();
        T castToken = safeCast(rawToken, type);

        if(castToken == null) {
            throw new IllegalSyntaxException(rawToken, "Unexpected token: " + rawToken.toXML());
        }

        return castToken;
    }

    private SymbolToken getSymbolOrDie(char symbol) {
        SymbolToken token = getTokenOrDie(SymbolToken.class);
        if(token.getValue() != symbol) {
            throw new IllegalSyntaxException(token, "Expected " + symbol + " but got " + token.getValue());
        }

        return token;
    }

    private String convertTokenToTypeName(Token typeToken, boolean allowVoid) {
        KeywordToken typeKeywordToken = safeCast(typeToken, KeywordToken.class);
        IdentifierToken typeIdentifierToken = safeCast(typeToken, IdentifierToken.class);
        String typeName;
        if(typeKeywordToken != null) {
            if(typeKeywordToken.getValue() == KeywordType.INT ||
               typeKeywordToken.getValue() == KeywordType.CHAR ||
               typeKeywordToken.getValue() == KeywordType.BOOLEAN ||
               (allowVoid && typeKeywordToken.getValue() == KeywordType.VOID)
            ) {
                typeName = typeKeywordToken.getValue().toString().toLowerCase();
            }
            else {
                throw new IllegalSyntaxException(
                        typeToken,
                        "Cannot use keyword " + typeKeywordToken.getValue().toString() + " as a type"
                );
            }
        }
        else if(typeIdentifierToken != null) {
            typeName = typeIdentifierToken.getValue();
        }
        else {
            throw new IllegalSyntaxException(
                    typeToken,
                    "Illegal type " + typeToken.toString()
            );
        }

        return typeName;
    }

    private void compileClass() {
        // Get tokens
        KeywordToken classToken = getTokenOrDie(KeywordToken.class);
        IdentifierToken className = getTokenOrDie(IdentifierToken.class);
        SymbolToken openCurly = getSymbolOrDie('{');

        if(classToken.getValue() == KeywordType.CLASS) {
            //region GEN Opening class code
            write("<class>");
            indentLevel++;

            write(classToken);
            write(className);
            write(openCurly);
            //endregion

            boolean done = false;
            while(!done && tokenizer.hasMoreTokens()) {
                Token nextToken = tokenizer.peek();
                KeywordToken keyword = safeCast(nextToken, KeywordToken.class);
                SymbolToken symbol = safeCast(nextToken, SymbolToken.class);

                if(symbol != null && symbol.getValue() == '}') {

                    //region GEN Closing class code
                    tokenizer.next();
                    write(nextToken);

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
                    else  {
                        throw new IllegalSyntaxException(nextToken, "Unexpected keyword in class body: " + nextToken);
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
            throw new IllegalSyntaxException(classToken, "Malformed class definition");
        }
    }

    private void compileClassVarDec() {
        // Figure out the locality.
        KeywordToken locality = getTokenOrDie(KeywordToken.class);
        if(locality.getValue() != KeywordType.STATIC && locality.getValue() != KeywordType.FIELD) {
            throw new IllegalSyntaxException(
                    locality,
                    "Class variable declarations must be 'static' or 'field'"
            );
        }

        // Next, figure out the type.
        Token varType = tokenizer.next();
        String typeName = convertTokenToTypeName(varType, false);

        // Get every single variable name.
        List<IdentifierToken> names = compileVarDecList();

        // Alright, we have everything we need now.
        // region GEN Variable declaration code
        write("<classVarDec>");
        indentLevel++;

        write(locality);
        write(varType);
        write(names.get(0));
        for(int i = 1; i < names.size(); i++) {
            write(new SymbolToken(-1, ","));
            write(names.get(i));
        }
        write(new SymbolToken(-1, ";"));

        indentLevel--;
        write("</classVarDec>");
        // endregion

    }

    private void compileSubroutine() {
        KeywordToken routineType = getTokenOrDie(KeywordToken.class);
        if (routineType.getValue() != KeywordType.CONSTRUCTOR &&
            routineType.getValue() != KeywordType.FUNCTION &&
            routineType.getValue() != KeywordType.METHOD
        ) {
            throw new IllegalSyntaxException(routineType, "Invalid function type");
        }

        Token returnType = tokenizer.next();
        String returnTypeName = convertTokenToTypeName(returnType, true);

        IdentifierToken routineName = getTokenOrDie(IdentifierToken.class);

        // region GEN Subroutine open
        write("<subroutineDec>");
        indentLevel++;

        write(routineType);
        write(returnType);
        write(routineName);
        // endregion

        write(getSymbolOrDie('('));
        compileParameterList();
        write(getSymbolOrDie(')'));

        compileSubroutineBody();

        // region GEN subroutine close
        indentLevel--;
        write("</subroutineDec>");
        // endregion
    }

    /**
     * This ends upon seeing ')' but it does not consume it. This also
     * assumes that the opening '(' has already been consumed.
     */
    private void compileParameterList() {

        write("<parameterList>");
        indentLevel++;

        SymbolToken nextTokenSymbol = safeCast(tokenizer.peek(), SymbolToken.class);

        // Loop until we see ')'
        while(nextTokenSymbol == null || nextTokenSymbol.getValue() != ')') {

            // There are two possible scenarios here:
            //  1. paramType paramName,
            //  2. paramType paramName)
            //
            // We want to consume commas and leave parenthesis.
            Token paramType = tokenizer.next();
            String paramTypeName = convertTokenToTypeName(paramType, false);
            IdentifierToken paramName = getTokenOrDie(IdentifierToken.class);

            // region GEN Subroutine parameter
            write(paramType);
            write(paramName);
            // endregion

            // Either way, the next token MUST be a symbol (either ',' or ')').
            nextTokenSymbol = peekTokenOrDie(SymbolToken.class);
            if(nextTokenSymbol.getValue() == ',') {
                tokenizer.next();
                write(nextTokenSymbol); // TODO Take this out later
            }
        }

        indentLevel--;
        write("</parameterList>");
    }

    /**
     * This consumes both the '{' and '}' tokens.
     */
    private void compileSubroutineBody() {

        write("<subroutineBody>");
        indentLevel++;
        write(getSymbolOrDie('{'));

        // First we'll need to handle any possible variable declarations
        KeywordToken nextToken = safeCast(tokenizer.peek(), KeywordToken.class);
        while(nextToken != null && nextToken.getValue() == KeywordType.VAR) {
            compileVarDec();
        }

        // Now that that's done, we can get onto the rest of the subroutine
        compileStatements();

        write(getSymbolOrDie('}'));
        indentLevel--;
        write("</subroutineBody>");
    }

    /**
     * Takes any variable declarations, and continues until a ';' is encountered.
     * This consumes the entire statement, including the ';' at the end.
     */
    private void compileVarDec() {

        // Ensure we have the var keyword.
        KeywordToken varKeyword = getTokenOrDie(KeywordToken.class);
        if(varKeyword.getValue() != KeywordType.VAR) {
            throw new IllegalSyntaxException( varKeyword, "Subroutine variable declarations must be 'var'" );
        }

        // Next, figure out the type.
        Token varType = tokenizer.next();
        String typeName = convertTokenToTypeName(varType, false);

        // Get every single variable name.
        List<IdentifierToken> names = compileVarDecList();

        // Alright, we have everything we need now.
        // region GEN Variable declaration code
        write("<varDec>");
        indentLevel++;

        write(varKeyword);
        write(varType);
        write(names.get(0));
        for(int i = 1; i < names.size(); i++) {
            write(new SymbolToken(-1, ","));
            write(names.get(i));
        }
        write(new SymbolToken(-1, ";"));

        indentLevel--;
        write("</varDec>");
        // endregion
    }

    /**
     * Runs through statements until a '}' is encountered. Note that this
     * does not consume the '}' nor does this expect the '{'.
     */
    private void compileStatements() {
        write("<statements>");
        indentLevel++;

        // Loop until we find a '}'
        Token nextToken = tokenizer.peek();
        SymbolToken nextTokenAsSymbol = safeCast(nextToken, SymbolToken.class);
        while(!(nextTokenAsSymbol != null && nextTokenAsSymbol.getValue() == '}')) {

            write(tokenizer.next().toXML() + " (Unsupported statement)");

            nextToken = tokenizer.peek();
            nextTokenAsSymbol = safeCast(nextToken, SymbolToken.class);
        }

        indentLevel--;
        write("</statements>");
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


    /**
     * This specifically consumes the set of names following a variable
     * declaration. That is, it consumes "name, name, name, ..., name;"
     * and returns a list of each of the names. It assumes that there
     * is at least one name followed by a ';', and throws an exception
     * if this isn't the case.
     */
    private List<IdentifierToken> compileVarDecList() {

        // Now we'll get the variable names. We MUST see at least one
        // name, but we might see more after that. It'll be in the
        // format of "name, name, name, ..., name;"
        //
        // Thus, we read the first name, and then repeat as long
        // as we see commas. If we see anything else, we stop and
        // make sure it's a semicolon.
        List<IdentifierToken> names = new Vector<>();
        names.add(getTokenOrDie(IdentifierToken.class));

        SymbolToken symbolAfterName = getTokenOrDie(SymbolToken.class);
        while(symbolAfterName.getValue() == ',') {
            names.add(getTokenOrDie(IdentifierToken.class));
            symbolAfterName = getTokenOrDie(SymbolToken.class);
        }

        if(symbolAfterName.getValue() != ';') {
            throw new IllegalSyntaxException(
                    symbolAfterName,
                    "Unexpected symbol: " + symbolAfterName.getValue()
            );
        }

        return names;
    }


}
