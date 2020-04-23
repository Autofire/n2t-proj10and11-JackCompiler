package SyntaxAnalysis;

import SyntaxAnalysis.Tokens.*;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Serves as a wrapper for a .jack file. This converts the input stream
 * into tokens, one token at a time. It has no concept of what the tokens
 * actually *mean*, it just breaks them up.
 */
public class JackTokenizer {
    private BufferedReader in;

    /**
     * Don't reference directly; call getCurrentLine. Yes, that's
     * private too, but we want to always make sure this has the next
     * batch of data.
     */
    private StringBuilder currentLine;

    public JackTokenizer(BufferedReader in) {
        this.in = in;
    }

    public boolean hasMoreTokens() throws IOException {
        return in.ready();
    }

    public Token nextToken() throws IOException {
        Token result = null;

        // First, we must seek until we find some non-whitespace characters
        // as well as all comments.
        pruneNonTokens();

        if(hasMoreTokens()) {
            // Ok, so there's more to the file AND we aren't done.
            StringBuilder currentToken = new StringBuilder();

        }

        return result;
    }

    /**
     * This fast-forwards through the file until we see something that is NOT
     * a token.
     */
    private void pruneNonTokens() throws IOException {
        Character c = peekNextChar();
        boolean donePruning = false;
        while(c != null && !donePruning) {

            if(c.toString().isBlank()) {
                removeNextChar();
            }
            else if(c == '/') {
                // We might be looking at a comment, either just for the
                // line or for an entire block.
                if(getCurrentLine().toString().startsWith("//")) {
                    discardLine();
                }
                else if(getCurrentLine().toString().startsWith("/*")) {
                    // Ok, we just saw a block comment. This means we'll need
                    // to delete up until the closing block symbol. Thus,
                    // we'll have another internal loop that seeks until
                    // we spot it.

                    // Remove the "/*"
                    removeNextChar();
                    removeNextChar();

                    while(currentLine != null && !currentLine.toString().contains("*/")) {
                        discardLine();
                        getCurrentLine();
                    }

                    if(currentLine != null) {
                        // Strip out everything that comes before the "*/"
                        currentLine = new StringBuilder(
                                currentLine.toString().replaceFirst("^.*?\\*/", "")
                        );
                    }
                }
            }
            else {
                // We've found something which looks like it can be a token.
                donePruning = true;
            }

            c = peekNextChar();
        }


    }

    /**
     * This attempts to grab the next character in the file. Note that this can
     * be called without actually changing where we are in the file.
     * @return The next character, if there is one. Returns null otherwise.
     */
    private Character peekNextChar() {
        StringBuilder l = getCurrentLine();

        if(l != null && l.length() > 0) {
            return l.charAt(0);
        }
        else {
            return null;
        }
    }

    /**
     * Removes the current character we're looking at.
     * @return True if removed, false otherwise.
     */
    private boolean removeNextChar() {
        StringBuilder l = getCurrentLine();

        if(l != null && l.length() > 0) {
            l.deleteCharAt(0);
            return true;
        }
        else {
            return false;
        }

    }

    /**
     * Attempts to grab the current line. This will keep grabbing more lines
     * until it finds one which is not empty. If there's any issue reading the
     * file, null is returned instead.
     * @return
     */
    private StringBuilder getCurrentLine() {
        try {
            while ((currentLine == null || currentLine.length() == 0) && hasMoreTokens()) {
                currentLine = new StringBuilder(in.readLine());
            }
        }
        catch(IOException e) {
            currentLine = null;
        }

        return currentLine;
    }

    /**
     * Call this if we decide that we don't need anything else on the current line
     */
    private void discardLine() {
        currentLine = null;
    }
}
