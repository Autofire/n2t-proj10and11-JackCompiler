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
            String line = "test */ /* */ */ test /* test */ // more blah // yet more blah /* /* blah;";

            System.out.println(line = line.replaceFirst("^.*?\\*/", ""));
            System.out.println(line = line.replaceAll("/\\*.*?\\*/", ""));
            System.out.println(line = line.replaceFirst("/\\*.*$", ""));
            System.out.println(line = line.replaceFirst("//.*$", ""));

            System.out.println("  ".strip().split("\\s+")[0].isEmpty() );

            String line2 = "blah; // test // test2";
            System.out.println(line2.replaceFirst("//.*", ""));

            return;
        }

        String rawTargetArg = args[0];

        String targetFileName;

        if(rawTargetArg.endsWith(".jack")) {
            targetFileName = rawTargetArg;
        }
        else {
            // TODO
            throw new IllegalArgumentException("Directories not supported");
        }

        System.out.println("Opening " + targetFileName);

        try(BufferedReader reader = new BufferedReader(new FileReader(targetFileName))) {
            JackTokenizer tokenizer = new JackTokenizer(reader);

            while(tokenizer.ready()) {
                tokenizer.nextToken();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
