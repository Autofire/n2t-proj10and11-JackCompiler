//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

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

    }
}
