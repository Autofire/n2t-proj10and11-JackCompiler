package CodeGeneration;

import SyntaxAnalysis.JackTokenizer;
import SyntaxAnalysis.KeywordType;
import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            throw new IllegalSyntaxException(rawToken, "Unexpected token: " + rawToken);
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

    private KeywordToken getKeywordOrDie(KeywordType keyword) {
        KeywordToken token = getTokenOrDie(KeywordToken.class);
        if(token.getValue() != keyword) {
            throw new IllegalSyntaxException(token, "Expected " + keyword + " but got " + token.getValue());
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
        KeywordToken classToken = getKeywordOrDie(KeywordType.CLASS);
        IdentifierToken className = getTokenOrDie(IdentifierToken.class);
        SymbolToken openCurly = getSymbolOrDie('{');

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
        List<IdentifierToken> names = parseVarDecList();

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
            nextToken = safeCast(tokenizer.peek(), KeywordToken.class);
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
        KeywordToken varKeyword = getKeywordOrDie(KeywordType.VAR);

        // Next, figure out the type.
        Token varType = tokenizer.next();
        String typeName = convertTokenToTypeName(varType, false);

        // Get every single variable name.
        List<IdentifierToken> names = parseVarDecList();

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

            //write(tokenizer.next().toXML() + " (Unsupported statement)");
            KeywordToken keyword = peekTokenOrDie(KeywordToken.class);
            switch(keyword.getValue()) {
                case LET:
                    compileLet();
                    break;

                case DO:
                    compileDo();
                    break;

                case IF:    // This consumes the else as well
                    compileIf();
                    break;

                case WHILE:
                    compileWhile();
                    break;

                case RETURN:
                    compileReturn();
                    break;

                default:
                    throw new IllegalSyntaxException(keyword, "Unexpected keyword " + keyword);
            }

            nextToken = tokenizer.peek();
            nextTokenAsSymbol = safeCast(nextToken, SymbolToken.class);
        }

        indentLevel--;
        write("</statements>");
    }

    private void compileDo() {
        write("<doStatement>");
        indentLevel++;

        write(getKeywordOrDie(KeywordType.DO));

        IdentifierToken firstToken = getTokenOrDie(IdentifierToken.class);
        if(peekTokenOrDie(SymbolToken.class).getValue() == '.') {
            tokenizer.next(); // Skip the '.'
            compileSubroutineCall(firstToken, getTokenOrDie(IdentifierToken.class));
        }
        else {
            compileSubroutineCall(firstToken);
        }

        write(getSymbolOrDie(';'));

        indentLevel--;
        write("</doStatement>");
    }

    private void compileLet() {
        write("<letStatement>");
        indentLevel++;

        write(getKeywordOrDie(KeywordType.LET));

        IdentifierToken varName = getTokenOrDie(IdentifierToken.class);
        write(varName);

        SymbolToken symbol = peekTokenOrDie(SymbolToken.class);
        if(symbol.getValue() == '[') {
            write(getSymbolOrDie('['));
            compileExpression();
            write(getSymbolOrDie(']'));
        }
        write(getSymbolOrDie('='));

        compileExpression();

        write(getSymbolOrDie(';'));

        indentLevel--;
        write("</letStatement>");
    }

    private void compileWhile() {

        write("<whileStatement>");
        indentLevel++;

        write(getKeywordOrDie(KeywordType.WHILE));

        write(getSymbolOrDie('('));
        compileExpression();
        write(getSymbolOrDie(')'));

        write(getSymbolOrDie('{'));
        compileStatements();
        write(getSymbolOrDie('}'));

        indentLevel--;
        write("</whileStatement>");
    }

    private void compileIf() {

        write("<ifStatement>");
        indentLevel++;

        write(getKeywordOrDie(KeywordType.IF));

        write(getSymbolOrDie('('));
        compileExpression();
        write(getSymbolOrDie(')'));

        write(getSymbolOrDie('{'));
        compileStatements();
        write(getSymbolOrDie('}'));

        KeywordToken elseToken = safeCast(tokenizer.peek(), KeywordToken.class);
        if(elseToken != null && elseToken.getValue() == KeywordType.ELSE) {
            write(tokenizer.next());

            write(getSymbolOrDie('{'));
            compileStatements();
            write(getSymbolOrDie('}'));
        }

        indentLevel--;
        write("</ifStatement>");
    }

    private void compileReturn() {

        write("<returnStatement>");
        indentLevel++;

        write(getKeywordOrDie(KeywordType.RETURN));

        SymbolToken symbol = safeCast(tokenizer.peek(), SymbolToken.class);
        if(!(symbol != null && symbol.getValue() == ';')) {
            compileExpression();
        }

        write(getSymbolOrDie(';'));

        indentLevel--;
        write("</returnStatement>");
    }


    /**
     * This continues through terms until it encounters something
     * which is NOT a binary operator symbol. It will not consume
     * the token which causes this function to stop.
     */
    private void compileExpression() {

        // Note that unary operands are treated as a part of the term,
        // so we don't need to worry about those.

        write("<expression>");
        indentLevel++;

        compileTerm();

        SymbolToken symbolAfterToken = safeCast(tokenizer.peek(), SymbolToken.class);
        while(symbolAfterToken != null && symbolAfterToken.isBinaryOp() ) {
            write(tokenizer.next());
            compileTerm();

            symbolAfterToken = safeCast(tokenizer.peek(), SymbolToken.class);
        }

        indentLevel--;
        write("</expression>");
    }

    private void compileTerm() {
        write("<term>");
        indentLevel++;

        Token termToken = tokenizer.next();

        IntLiteralToken    intLiteral       = safeCast(termToken, IntLiteralToken.class);
        StringLiteralToken stringLiteral    = safeCast(termToken, StringLiteralToken.class);
        KeywordToken       keyword          = safeCast(termToken, KeywordToken.class);
        SymbolToken        symbol           = safeCast(termToken, SymbolToken.class);
        IdentifierToken    identifier       = safeCast(termToken, IdentifierToken.class);

        if(intLiteral != null) {
            write(intLiteral);
        }
        else if(stringLiteral != null) {
            write(stringLiteral);
        }
        else if(keyword != null && keyword.isConst()) {
            write(keyword);
        }
        else if(symbol != null) {
            // A symbol means one of two things:
            //  1. UNARY_OP term
            //  2. (expression)
            if(symbol.isUnaryOp()) {
                write(symbol);
                compileTerm();
            }
            else if(symbol.getValue() == '(') {
                write(symbol);
                compileExpression();
                write(getSymbolOrDie(')'));
            }
            else {
                throw new IllegalSyntaxException(symbol, "Unexpected symbol: " + symbol);
            }
        }
        else if(identifier != null) {
            // Here's the messy one. Identifiers can mean any one of these things:
            //  1. varName
            //  2. varName[expression]
            //  3. subroutineCall
            //   3a. callName( ... )
            //   3b. obj.callName( ... )
            //
            // For all but the first one, those are determined via symbols. Thus,
            // we can easily check for them all at once.

            SymbolToken nextSymbol = safeCast(tokenizer.peek(), SymbolToken.class);

            // If this stays false, we'll assume it's plain variable.
            boolean handledTerm = false;

            if(nextSymbol != null) {
                // If there's a symbol, we'll assume it's handled.
                // This'll get set back false if the switch below doesn't
                // detect anything to do.
                handledTerm = true;

                switch(nextSymbol.getValue()) {
                    case '[':
                        write(identifier);
                        write(getSymbolOrDie('['));
                        compileExpression();
                        write(getSymbolOrDie(']'));
                        break;

                    case '.':
                        // In this case, the identifier is the class name.
                        tokenizer.next();
                        compileSubroutineCall( identifier, getTokenOrDie(IdentifierToken.class) );
                        break;

                    case '(':
                        compileSubroutineCall( identifier);
                        break;

                    default:
                        handledTerm = false;
                }
            }

            if(!handledTerm) {
                write(identifier);
            }
        }
        else {
            throw new IllegalSyntaxException(termToken, "Unexpected token in term: " + termToken);
        }

        indentLevel--;
        write("</term>");
    }

    /**
     * This continues to consume expressions until a ')' is found.
     * (In every situation with an expression list, they are always bound
     * by parenthesis so we're exploiting that assumption here.)
     */
    private void compileExpressionList() {

        write("<expressionList>");
        indentLevel++;

        SymbolToken symbol = safeCast(tokenizer.peek(), SymbolToken.class);
        if(symbol == null || symbol.getValue() != ')') {
            // We have at LEAST one expression, maybe more. After each expression,
            // we can be sure that we will always have a symbol.
            compileExpression();

            // So now we either have a comma followed by another expression
            // (and this can repeat many times), or a close parenthesis.
            symbol = peekTokenOrDie(SymbolToken.class);
            while(symbol.getValue() == ',') {
                write(tokenizer.next());    // Write the ','
                compileExpression();

                symbol = peekTokenOrDie(SymbolToken.class);
            }
        }

        indentLevel--;
        write("</expressionList>");
    }

    private void compileSubroutineCall(IdentifierToken subroutineName) {
        compileSubroutineCall(null, subroutineName);
    }

    /**
     * Figures out what to do with a subroutine. Subroutines take one
     * of two forms:
     *  1. name(expressionList)
     *  2. obj.name(expressionList)
     *
     * Callers must pass in either the name 'token', or the tokens for
     * 'obj.name'. In the middle of expressions, we can do this because
     * we know that an identifier followed immediately by '(' or '.'
     * must be a subroutine call.
     *
     * This consumes '(' and ')', in addition to the expressionList inside.
     *
     * @param objName Name of the object. Can be null.
     * @param subroutineName Name of the subroutine. SHOULD NOT BE NULL.
     */
    private void compileSubroutineCall(IdentifierToken objName, IdentifierToken subroutineName) {

        // region GEN Initial method call setup
        // Note that, in our XML format, there is no "subroutineCall" block,
        // so we can just spew our code out.
        if(objName != null) {
            write(objName);
            write(new SymbolToken(objName.getLineNumber(), "."));
        }
        write(subroutineName);

        write(getSymbolOrDie('('));
        compileExpressionList();
        write(getSymbolOrDie(')'));
        // endregion
    }

    /**
     * This specifically consumes the set of names following a variable
     * declaration. That is, it consumes "name, name, name, ..., name;"
     * and returns a list of each of the names. It assumes that there
     * is at least one name followed by a ';', and throws an exception
     * if this isn't the case.
     */
    private List<IdentifierToken> parseVarDecList() {

        // Now we'll get the variable names. We MUST see at least one
        // name, but we might see more after that. It'll be in the
        // format of "name, name, name, ..., name;"
        //
        // Thus, we read the first name, and then repeat as long
        // as we see commas. If we see anything else, we stop and
        // make sure it's a semicolon.
        List<IdentifierToken> names = new ArrayList<>();
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
