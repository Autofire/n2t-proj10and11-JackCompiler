import SyntaxAnalysis.Tokens.*;

public class JackCompiler {

    public static void main(String[] args) {
        String line = "*/ */ blah;";
        System.out.println(line.replaceFirst("^.*?\\*/", ""));
    }
}
