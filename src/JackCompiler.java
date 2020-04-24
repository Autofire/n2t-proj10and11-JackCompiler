import SyntaxAnalysis.JackAnalyzer;
import SyntaxAnalysis.JackTokenizer;
import SyntaxAnalysis.Tokens.*;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class JackCompiler {

    public static void main(String[] args) {

        // NOTE: This uses some early return statements
        //       to avoid nesting everything in a big 'ol if-else statement
        if(args.length != 1) {
            System.out.println("Incorrect usage");
            return;
        }

        String rawTargetArg = args[0];

        JackAnalyzer analyzer = new JackAnalyzer();
        analyzer.add(new File(rawTargetArg));
        try {
            analyzer.analyze();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        String targetFileName;
        String outputFileName;

        if(rawTargetArg.endsWith(".jack")) {
            targetFileName = rawTargetArg;
            outputFileName = rawTargetArg.replace(".jack", "C.xml");
        }
        else {
            // TODO
            throw new IllegalArgumentException("Directories not supported");
        }

        System.out.println("Opening " + targetFileName);
        System.out.println("Writing to " + outputFileName);

        try(BufferedReader reader = new BufferedReader(new FileReader(targetFileName))) {
            JackTokenizer tokenizer = new JackTokenizer(reader);

            while(tokenizer.hasMoreTokens()) {
                System.out.println(tokenizer.nextToken().toXML());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
         */

    }
}
