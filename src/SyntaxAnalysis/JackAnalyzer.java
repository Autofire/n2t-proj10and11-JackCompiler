package SyntaxAnalysis;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/***
 * Takes multiple Jack files and converts them into more usable data.
 * At the moment, that means just turning it all into tokens and then
 * all into XML, but more useful things will come later.
 */
public class JackAnalyzer {
    private List<File> inputFiles = new LinkedList<>();

    public JackAnalyzer() {

    }

    public void add(File target) {
        if(target.exists()) {
            if(target.isFile() && target.getName().endsWith(".jack")) {
                inputFiles.add(target);
            }
            else if(target.isDirectory()) {
                throw new IllegalArgumentException("Can't handle directories yet");
            }
            else {
                throw new IllegalArgumentException("Can only handle directories and jack files.");
            }
        }
        else {
            throw new IllegalArgumentException("Can only read files which exist. (Duh!)");
        }
    }

    public void analyze() throws IOException {
        for(File inputFile : inputFiles) {
            File outputFile = new File(inputFile.getPath().replace(".jack", ".C.xml"));

            System.out.println("Opening " + inputFile);
            System.out.println("Writing " + outputFile);

            try(BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                PrintStream writer = new PrintStream(outputFile)) {

                JackTokenizer tokenizer = new JackTokenizer(reader);

                writer.println("<tokens>");
                while(tokenizer.hasMoreTokens()) {
                    writer.println(tokenizer.nextToken().toXML());
                }
                writer.println("</tokens>");
            }

        }
    }
}
