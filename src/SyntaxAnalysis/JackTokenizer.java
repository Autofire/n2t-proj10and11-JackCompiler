package SyntaxAnalysis;

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

    public JackTokenizer(BufferedReader in) {
        this.reader = in;
    }

    public boolean ready() throws IOException {
        // TODO We need a hasMoreTokens function instead...
        return reader.ready();
    }

    public Token nextToken() throws IOException {
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

        Token result = null;

        /*
        if(ready()) {
            // Ok, so there's more to the file AND we aren't done.
            StringBuilder currentToken = new StringBuilder();

        }
         */

        getNextLine();

        if(currentLine != null) {
            for (String word : currentLine) {
                System.out.print(word + "_");
            }
            System.out.println();
        }
        else {
            System.out.println("// Null line");
        }

        return result;
    }

    /**
     * Gets the next line, breaks it up according to spaces, and
     * stores it in currentLine.
     * @throws IOException
     */
    private void getNextLine() throws IOException {

        currentLine = null;

        // We will keep going until we get a non-empty line.
        while(currentLine == null && ready()) {
            String line = reader.readLine();

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
                    String regex =
                            String.format(
                                    "\\s+|((?<=%1$s)|(?=%1$s))",
                                    "[\\Q" + "{}()[].,;+-*/&|<>=~\"" + "\\E]"
                            );

                    currentLine = new Vector<String>(
                            Arrays.asList( line.split( regex ) )
                    );
                }

            }
        }

    }


}
