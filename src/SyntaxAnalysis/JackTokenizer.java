package SyntaxAnalysis;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Serves as a wrapper for a .jack file. This converts the input stream
 * into tokens, one token at a time. It has no concept of what the tokens
 * actually *mean*, it just breaks them up.
 */
public class JackTokenizer {
    private BufferedReader reader;
    private List<String> currentLine;
    private boolean unclosedBlockComment = false;
    private Token nextToken;

    /**
     * This matches whatever line we are actually on in the file.
     */
    private int lineNumber = 0;


    public JackTokenizer(BufferedReader in) {
        this.reader = in;
    }

    /**
     * Gets the next token's line number. Note that this will advance
     * every time you go past the end of a line.
     * @return The line number of the next token.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Checks if there are more tokens. This may cause a read
     * from the file to occur.
     * @return True if there is at least unhandled 1 token.
     * @throws IOException if a read error occurs
     */
    public boolean hasMoreTokens() {
        try {
            if (nextToken == null) {
                nextToken = readNextToken();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return nextToken != null;
    }

    /**
     * Reads the next token. This "discards" it from the file,
     * so the next read will result in a new token.
     * @return The token, or null if hasMoreTokens is false.
     * @throws IOException if a read error occurs
     */
    public Token next() {
        Token currentToken = null;

        try {
            if (hasMoreTokens()) {
                currentToken = nextToken;
                nextToken = readNextToken();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return currentToken;
    }

    public Token peek() {
        if(hasMoreTokens()) {
            return nextToken;
        }
        else {
            return null;
        }
    }

    /**
     * Attempts to grab the next token. If there are no tokens left,
     * then null is returned.
     * @return The next token or null.
     * @throws IOException if a read error occurs
     */
    private Token readNextToken() throws IOException {

        Token result = null;

        // Only get another line if we've either parsed this one
        // or we don't have any line at all yet.
        if(currentLine == null || currentLine.size() == 0) {
            getNextLine();
        }

        if(currentLine != null) {

            // Ok, here's where we actually pull the token out of the line.
            // Most tokens will have been correctly broken into their own
            // elements of the array. There's some exceptions however:
            //  1. Some array elements are empty strings.
            //  2. String literals are also broken up.
            //
            // The first case can be solved just by ignoring empty strings.
            // Note that these never occur at the end of the line, so we can
            // safely loop through the line until we find something interesting.
            //
            // The second needs some special handling. Fortunately, double quotes
            // also get split into their own array elements. Thus, we can stitch
            // elements together until we find another double quote. AFAIK, a string
            // literal cannot span multiple lines. However, some level of checking
            // should occur so we know when it occurs.

            // So, as I just said, we need to strip out any empty strings.
            while(currentLine.get(0).isEmpty()) {
                currentLine.remove(0);
            }

            // Now we can actually start to figure out what the token is
            // that we're looking at. According to the Jack specs, these are
            // the possibilities (and the order we'll check for them):
            //  1. Symbols
            //  2. Keywords
            //  3. Integer literals
            //  4. String literals
            //  5. Identifiers
            //
            // The first three are trivial. The 4th, as previously mentioned,
            // is easy to spot but a little harder to stitch together.
            // We save Identifiers for last because we could mix things up
            // otherwise. That being said, we still make sure it's a valid
            // identifier. If it is not, then we know there's a syntax error
            // so we'll give up immediately.

            String tStr = currentLine.get(0);
            currentLine.remove(0);

            if(SymbolToken.isValid(tStr)) {
                result = new SymbolToken(lineNumber, tStr);
            }
            else if(KeywordToken.isValid(tStr)) {
                result = new KeywordToken(lineNumber, tStr);
            }
            else if(IntLiteralToken.isValid(tStr)) {
                result = new IntLiteralToken(lineNumber, tStr);
            }
            else if(tStr.equals("\"")) {
                // Ok, so it *looks* like a string literal.
                StringBuilder rawStr = new StringBuilder();
                rawStr.append(tStr);

                do {
                    tStr = currentLine.get(0);
                    currentLine.remove(0);
                    rawStr.append(tStr);
                } while(!tStr.equals("\""));

                String strLiteral = rawStr.toString();
                if(StringLiteralToken.isValid(strLiteral)) {
                    result = new StringLiteralToken(lineNumber, strLiteral);
                }
                else {
                    throw new RuntimeException("Malformed string literal");
                }

            }
            else if(IdentifierToken.isValid(tStr)) {
                result = new IdentifierToken(lineNumber, tStr);
            }
            else {
                throw new RuntimeException("Malformed input");
            }

        }

        return result;
    }

    /**
     * Gets the next line, breaks it up according to spaces, and
     * stores it in currentLine.
     * @throws IOException if a read error occurs
     */
    private void getNextLine() throws IOException {

        // When reading, we can have these possibilities:
        //  1. The line has no usable code (it's empty or has comments)
        //  2. The line has just usable code
        //  3. The line has some of each
        //
        // In the case of double slash, it's easy; just strip everything that
        // follows. However, with /* */, it's a little harder. If we have a
        // line of code has an unclosed /*, we would then want to keep going
        // through lines until we find a matching */. Note that more code
        // may follow after the */!
        //
        // HOWEVER, this gets harder if we have both code and an unclosed
        // /* in the line. Then we need to remember that we're looking for */
        // but not until we're done with the current line. Then we can
        // resume as normal.

        // We need to invalidate the array.
        currentLine = null;

        // We will keep going until we get a non-empty line.
        while(currentLine == null && reader.ready()) {
            String line = reader.readLine();
            lineNumber++;

            // First, we'll check if we're in a block. If we are,
            // we gotta delete until the next */. If that DOESN'T
            // exist, then we gotta just delete it all.
            if (unclosedBlockComment) {
                int oldLength = line.length();
                line = line.replaceFirst("^.*?\\*/", "");

                // If no deletion occurred, we want to wipe out the
                // entire line.
                if(line.length() == oldLength) {
                    line = "";
                }
                else {
                    unclosedBlockComment = false;
                }
            }

            // At this point, the line might just be empty...

            if(!line.isEmpty()) {
                // First, we'll nuke all contained instances of /* ... */
                line = line.replaceAll("/\\*.*?\\*/", "");

                // Now we'll nuke the first instance of /* ...
                // Note that if we DO make a deletion, it means we have an
                // unclosed comment block! Hence, we'll track the length.
                // If the length changes, then we know there's an unclosed
                // block comment.
                int oldLength = line.length();
                line = line.replaceFirst("/\\*.*$", "");
                unclosedBlockComment = (line.length() != oldLength);

                // We'll delete the first instance of // ...
                line = line.replaceFirst("//.*$", "");

                // Ok, FINALLY... we can drop any whitespace.
                line = line.strip();

                // If, after all of that, there's more to the line, we should
                // be able to extract some meaningful code from it.
                if(!line.isEmpty()) {

                    // TODO Handle escaped quotes

                    // See https://stackoverflow.com/a/11503678
                    // See https://stackoverflow.com/a/2206432
                    String regex = String.format(
                        "\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)|" +  // Match spaces outside of quotes, or...
                        "((?<=%1$s)|(?=%1$s))",                    // Match symbols but split around them
                        "[\\Q" + "{}()[].,;+-*/&|<>=~\"" + "\\E]"  // The list of symbols to detect
                    );

                    currentLine = new Vector<>(
                            Arrays.asList(line.split(regex))
                    );

                    /*
                    for (String word : currentLine) {
                        System.out.print(word + "_");
                    }
                    System.out.println();
                     */
                }

            }
        }


    }


}
