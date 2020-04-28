package SyntaxAnalysis;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import CodeGeneration.CompilationEngine;

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
                //throw new IllegalArgumentException("Can't handle directories yet");
                for(File file : target.listFiles()) {
                    if(file.getName().endsWith(".jack")) {
                        inputFiles.add(file);
                    }
                }
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
            File outputVMFile = new File(inputFile.getPath().replace(".jack", ".vm"));
            File outputXMLFile = new File(inputFile.getPath().replace(".jack", ".C.xml"));

            System.out.println("Reading jack from " + inputFile);
            System.out.println("Writing VM to     " + outputVMFile);
            System.out.println("Writing XML to    " + outputXMLFile);
            System.out.println();

            try(BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                PrintStream xmlWriter = new PrintStream(outputXMLFile);
                PrintStream vmWriter = new PrintStream(outputVMFile)) {

                /*
                JackTokenizer tokenizer = new JackTokenizer(reader);

                writer.println("<tokens>");
                while(tokenizer.hasMoreTokens()) {
                    writer.println(tokenizer.next().toXML());
                }
                writer.println("</tokens>");
                 */

                CompilationEngine engine = new CompilationEngine(reader, vmWriter, xmlWriter);
                engine.compile();

            }

        }
    }
}
