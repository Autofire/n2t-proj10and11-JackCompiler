package CodeGeneration;

import CodeGeneration.VM.ArithmeticCommand;
import CodeGeneration.VM.Segment;
import CodeGeneration.VM.VMWriter;
import CodeGeneration.VariableTracking.Variable;
import CodeGeneration.VariableTracking.VariableKind;
import CodeGeneration.VariableTracking.VariableTable;
import SyntaxAnalysis.JackTokenizer;
import SyntaxAnalysis.KeywordType;
import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private VMWriter vmWriter;
    private PrintStream xmlWriter;
    private VariableTable symbolTable;

    private String className;

    public CompilationEngine(BufferedReader reader, PrintStream vmWriter, PrintStream xmlWriter) {
        //this.reader = reader;
        tokenizer = new JackTokenizer(reader);
        this.vmWriter = new VMWriter(vmWriter);
        this.xmlWriter = xmlWriter;
        symbolTable = new VariableTable();
    }

    // region XML
    private final static String INDENT = "  ";
    private int indentLevel = 0;

    /**
     * Writes XML to the file. This is indented based indentLevel.
     * @param value String to write.
     */
    private void writeXML(String value) {
        if(xmlWriter != null) {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < indentLevel; i++) {
                sb.append(INDENT);
            }

            xmlWriter.print(sb.toString());
            xmlWriter.println(value);
        }
    }

    /**
     * Writes XML to the file. This is indented based indentLevel.
     * @param t Token to write. t.toXML is called.
     */
    private void writeXML(Token t) {
        writeXML(t.toXML());
    }
    // endregion

    // region Casting and requiring
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
    // endregion

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

    // region Class compilation
    private void compileClass() {
        writeXML("<class>");
        indentLevel++;

        // Get tokens
        writeXML(getKeywordOrDie(KeywordType.CLASS));

        IdentifierToken classNameToken = getTokenOrDie(IdentifierToken.class);
        writeXML(classNameToken);

        writeXML(getSymbolOrDie('{'));

        className = classNameToken.getValue();

        boolean done = false;
        while(!done && tokenizer.hasMoreTokens()) {
            Token nextToken = tokenizer.peek();
            KeywordToken keyword = safeCast(nextToken, KeywordToken.class);
            SymbolToken symbol = safeCast(nextToken, SymbolToken.class);

            if(symbol != null && symbol.getValue() == '}') {

                tokenizer.next();
                writeXML(nextToken);

                indentLevel--;
                writeXML("</class>");

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
                writeXML(nextToken.toXML() + "(Unsupported)");
                tokenizer.next();   // Normally the child function consume this
            }
        }

        if(!done && !tokenizer.hasMoreTokens()) {
            throw new IllegalSyntaxException(tokenizer.getLineNumber(), "Unexpected end of file");
        }

    }
    // endregion

    // region Subroutine compilation
    private void compileSubroutine() {
        symbolTable.startSubroutine();

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

        writeXML("<subroutineDec>");
        indentLevel++;

        writeXML(routineType);
        writeXML(returnType);
        writeXML(routineName);

        writeXML(getSymbolOrDie('('));
        compileParameterList();
        writeXML(getSymbolOrDie(')'));

        // At this point, the symbol table knows the number of parameters we have.
        // Thus, we can go ask it when we load construct the VM function definition.
        vmWriter.writeFunction(
                className + "." + routineName.getValue(),
                symbolTable.varCount(VariableKind.ARG)
        );

        compileSubroutineBody();

        indentLevel--;
        writeXML("</subroutineDec>");
    }


    /**
     * This consumes both the '{' and '}' tokens.
     */
    private void compileSubroutineBody() {

        writeXML("<subroutineBody>");
        indentLevel++;
        writeXML(getSymbolOrDie('{'));

        // First we'll need to handle any possible variable declarations
        KeywordToken nextToken = safeCast(tokenizer.peek(), KeywordToken.class);
        while(nextToken != null && nextToken.getValue() == KeywordType.VAR) {
            compileVarDec();
            nextToken = safeCast(tokenizer.peek(), KeywordToken.class);
        }

        // Now that that's done, we can get onto the rest of the subroutine
        compileStatements();

        writeXML(getSymbolOrDie('}'));
        indentLevel--;
        writeXML("</subroutineBody>");
    }
    // endregion

    // region Statement compilation
    /**
     * Runs through statements until a '}' is encountered. Note that this
     * does not consume the '}' nor does this expect the '{'.
     */
    private void compileStatements() {
        writeXML("<statements>");
        indentLevel++;

        // Loop until we find a '}'
        Token nextToken = tokenizer.peek();
        SymbolToken nextTokenAsSymbol = safeCast(nextToken, SymbolToken.class);
        while(!(nextTokenAsSymbol != null && nextTokenAsSymbol.getValue() == '}')) {

            //writeXML(tokenizer.next().toXML() + " (Unsupported statement)");
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
        writeXML("</statements>");
    }

    private void compileDo() {
        writeXML("<doStatement>");
        indentLevel++;

        writeXML(getKeywordOrDie(KeywordType.DO));

        IdentifierToken firstToken = getTokenOrDie(IdentifierToken.class);
        if(peekTokenOrDie(SymbolToken.class).getValue() == '.') {
            tokenizer.next(); // Skip the '.'
            compileSubroutineCall(firstToken, getTokenOrDie(IdentifierToken.class));
        }
        else {
            compileSubroutineCall(firstToken);
        }

        // All functions return something, even void functions. However,
        // we discard anything that comes out of a 'Do' call.
        vmWriter.writePop(Segment.TEMP, 0);

        writeXML(getSymbolOrDie(';'));

        indentLevel--;
        writeXML("</doStatement>");
    }

    private void compileLet() {
        writeXML("<letStatement>");
        indentLevel++;

        writeXML(getKeywordOrDie(KeywordType.LET));

        IdentifierToken varName = getTokenOrDie(IdentifierToken.class);
        //writeXML(varName);
        Variable var = symbolTable.get(varName);
        writeXML(var.toXML("(ref)"));

        SymbolToken symbol = peekTokenOrDie(SymbolToken.class);
        if(symbol.getValue() == '[') {
            writeXML(getSymbolOrDie('['));
            compileExpression();
            writeXML(getSymbolOrDie(']'));
        }
        writeXML(getSymbolOrDie('='));

        compileExpression();

        writeXML(getSymbolOrDie(';'));

        indentLevel--;
        writeXML("</letStatement>");
    }

    private void compileWhile() {

        writeXML("<whileStatement>");
        indentLevel++;

        writeXML(getKeywordOrDie(KeywordType.WHILE));

        writeXML(getSymbolOrDie('('));
        compileExpression();
        writeXML(getSymbolOrDie(')'));

        writeXML(getSymbolOrDie('{'));
        compileStatements();
        writeXML(getSymbolOrDie('}'));

        indentLevel--;
        writeXML("</whileStatement>");
    }

    private void compileIf() {

        writeXML("<ifStatement>");
        indentLevel++;

        writeXML(getKeywordOrDie(KeywordType.IF));

        writeXML(getSymbolOrDie('('));
        compileExpression();
        writeXML(getSymbolOrDie(')'));

        writeXML(getSymbolOrDie('{'));
        compileStatements();
        writeXML(getSymbolOrDie('}'));

        KeywordToken elseToken = safeCast(tokenizer.peek(), KeywordToken.class);
        if(elseToken != null && elseToken.getValue() == KeywordType.ELSE) {
            writeXML(tokenizer.next());

            writeXML(getSymbolOrDie('{'));
            compileStatements();
            writeXML(getSymbolOrDie('}'));
        }

        indentLevel--;
        writeXML("</ifStatement>");
    }

    private void compileReturn() {

        writeXML("<returnStatement>");
        indentLevel++;

        writeXML(getKeywordOrDie(KeywordType.RETURN));

        SymbolToken symbol = safeCast(tokenizer.peek(), SymbolToken.class);
        if(!(symbol != null && symbol.getValue() == ';')) {
            // This will leave the appropriate return value sitting
            // on the stack.
            compileExpression();
        }
        else {
            // Even for void functions, we return a 0.
            vmWriter.writePush(Segment.CONSTANT, 0);
        }
        vmWriter.writeReturn();

        writeXML(getSymbolOrDie(';'));

        indentLevel--;
        writeXML("</returnStatement>");
    }
    // endregion

    // region Expression and term compilation
    /**
     * This continues through terms until it encounters something
     * which is NOT a binary operator symbol. It will not consume
     * the token which causes this function to stop.
     */
    private void compileExpression() {

        // Note that unary operands are treated as a part of the term,
        // so we don't need to worry about those.

        writeXML("<expression>");
        indentLevel++;

        // We're making a hard assumption that there's at least one
        // term, because whenever an expression is expected, there's
        // gotta be at least one of these.
        compileTerm();

        SymbolToken symbolAfterToken = safeCast(tokenizer.peek(), SymbolToken.class);
        while(symbolAfterToken != null && symbolAfterToken.isBinaryOp() ) {
            //writeXML(tokenizer.next());

            // First, we'll yoink (and write XML for) the binary operator
            // that we've detected. However, we cannot act on it until we
            // determine the result of the term on the other side.
            SymbolToken operatorToken = getTokenOrDie(SymbolToken.class);
            writeXML(operatorToken);

            // So now that we have the symbol out of the way, let's figure
            // out what the other operand is. This will leave it sitting
            // on our stack, ready to be operated on.
            compileTerm();

            // At this point, we've handled two terms.
            // Both of their results should be on the stack, so now
            // we can go forward and perform the desired operation.
            ArithmeticCommand cmd = ArithmeticCommand.fromBinarySymbol(operatorToken);
            if(cmd == ArithmeticCommand.MULTIPLY || cmd == ArithmeticCommand.DIVIDE) {
                // Both of these commands are implemented as part of the OS, rather
                // than part of the base language. Thus, we'll just insert the
                // function call here.
                vmWriter.writeCall("Math." + cmd.toString().toLowerCase(), 2);
            }
            else {
                vmWriter.writeArithmetic(cmd);
            }

            symbolAfterToken = safeCast(tokenizer.peek(), SymbolToken.class);
        }

        indentLevel--;
        writeXML("</expression>");
    }

    private void compileTerm() {
        writeXML("<term>");
        indentLevel++;

        Token termToken = tokenizer.next();

        IntLiteralToken    intLiteral       = safeCast(termToken, IntLiteralToken.class);
        StringLiteralToken stringLiteral    = safeCast(termToken, StringLiteralToken.class);
        KeywordToken       keyword          = safeCast(termToken, KeywordToken.class);
        SymbolToken        symbol           = safeCast(termToken, SymbolToken.class);
        IdentifierToken    identifier       = safeCast(termToken, IdentifierToken.class);

        if(intLiteral != null) {
            writeXML(intLiteral);
            vmWriter.writePush(Segment.CONSTANT, intLiteral.getValue());
        }
        else if(stringLiteral != null) {
            writeXML(stringLiteral);
        }
        else if(keyword != null && keyword.isConst()) {
            writeXML(keyword);
        }
        else if(symbol != null) {
            // A symbol means one of two things:
            //  1. UNARY_OP term
            //  2. (expression)
            if(symbol.isUnaryOp()) {
                writeXML(symbol);
                compileTerm();
            }
            else if(symbol.getValue() == '(') {
                writeXML(symbol);
                compileExpression();
                writeXML(getSymbolOrDie(')'));
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
                        //writeXML(identifier);
                        Variable var = symbolTable.get(identifier);
                        writeXML(var.toXML("(ref)"));
                        writeXML(getSymbolOrDie('['));
                        compileExpression();
                        writeXML(getSymbolOrDie(']'));
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
                //writeXML(identifier);
                Variable var = symbolTable.get(identifier);
                writeXML(var.toXML("(ref)"));
            }
        }
        else {
            throw new IllegalSyntaxException(termToken, "Unexpected token in term: " + termToken);
        }

        indentLevel--;
        writeXML("</term>");
    }
    // endregion

    // region Subroutine call compilation
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
     * @param objToken Name of the object. Can be null.
     * @param subroutineToken Name of the subroutine. SHOULD NOT BE NULL.
     */
    private void compileSubroutineCall(IdentifierToken objToken, IdentifierToken subroutineToken) {

        // We need to push things onto the stack, and then we can do the
        // call. However, we generate the XML first, so the order is a
        // little iffy.
        String subroutineName;

        // Note that, in our XML format, there is no "subroutineCall" block,
        // so we can just spew our code out.
        if(objToken != null) {
            // TODO The first term could very well be an object, not a class. CHECK FOR THIS
            writeXML(objToken.toXML(" (class)"));
            writeXML(new SymbolToken(objToken.getLineNumber(), "."));
            writeXML(subroutineToken.toXML(" (subroutine)"));

            subroutineName = objToken.getValue() + "." + subroutineToken.getValue();
        }
        else {
            writeXML(subroutineToken.toXML(" (subroutine)"));

            subroutineName = subroutineToken.getValue();
        }

        writeXML(getSymbolOrDie('('));
        int argCount = compileExpressionList();
        writeXML(getSymbolOrDie(')'));

        vmWriter.writeCall(subroutineName, argCount);
    }

    /**
     * This continues to consume expressions until a ')' is found.
     * (In every situation with an expression list, they are always bound
     * by parenthesis so we're exploiting that assumption here.)
     *
     * @return Number of expressions in list.
     */
    private int compileExpressionList() {

        int expressionCount = 0;

        writeXML("<expressionList>");
        indentLevel++;

        SymbolToken symbol = safeCast(tokenizer.peek(), SymbolToken.class);
        if(symbol == null || symbol.getValue() != ')') {
            // We have at LEAST one expression, maybe more. After each expression,
            // we can be sure that we will always have a symbol.
            compileExpression();
            expressionCount++;

            // So now we either have a comma followed by another expression
            // (and this can repeat many times), or a close parenthesis.
            symbol = peekTokenOrDie(SymbolToken.class);
            while(symbol.getValue() == ',') {
                writeXML(tokenizer.next());    // Write the ','
                compileExpression();
                expressionCount++;

                symbol = peekTokenOrDie(SymbolToken.class);
            }
        }

        indentLevel--;
        writeXML("</expressionList>");

        return expressionCount;
    }
    // endregion

    // region Variable and parameter declaration
    // TODO Make all of this maybe do stuff?

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
        writeXML("<classVarDec>");
        indentLevel++;

        writeXML(locality);
        writeXML(varType);
        //writeXML(names.get(0));
        for(int i = 0; i < names.size(); i++) {
            //writeXML(names.get(i));
            Variable newVar = symbolTable.define(
                    names.get(i),
                    typeName,
                    VariableKind.fromKeyword(locality.getValue())
            );
            writeXML(newVar.toXML("(def)"));

            if(i < names.size()-1) {
                writeXML(new SymbolToken(-1, ","));
            }
        }
        writeXML(new SymbolToken(-1, ";"));

        indentLevel--;
        writeXML("</classVarDec>");
        // endregion

    }


    /**
     * This ends upon seeing ')' but it does not consume it. This also
     * assumes that the opening '(' has already been consumed.
     *
     * Note that these are PARAMETERS, not ARGUMENTS. This gets used
     * when defining methods/functions, not when calling them.
     */
    private void compileParameterList() {

        writeXML("<parameterList>");
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

            writeXML(paramType);
            //writeXML(paramName);
            Variable newVar = symbolTable.define(paramName, paramTypeName, VariableKind.ARG);
            writeXML(newVar.toXML("(def)"));

            // Either way, the next token MUST be a symbol (either ',' or ')').
            nextTokenSymbol = peekTokenOrDie(SymbolToken.class);
            if(nextTokenSymbol.getValue() == ',') {
                tokenizer.next();
                writeXML(nextTokenSymbol);
            }
        }

        indentLevel--;
        writeXML("</parameterList>");
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
        writeXML("<varDec>");
        indentLevel++;

        writeXML(varKeyword);
        writeXML(varType);
        for(int i = 0; i < names.size(); i++) {
            Variable newVar = symbolTable.define(names.get(i).getValue(), typeName, VariableKind.VAR);
            writeXML(newVar.toXML("(def)"));

            if(i < names.size()-1) {
                writeXML(new SymbolToken(-1, ","));
            }
        }
        writeXML(new SymbolToken(-1, ";"));

        indentLevel--;
        writeXML("</varDec>");
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
    // endregion


}
