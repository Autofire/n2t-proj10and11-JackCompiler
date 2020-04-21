import SyntaxAnalysis.Tokens.*;

public class JackCompiler {

    public static void main(String[] args) {
        System.out.println(new KeywordToken("Class").toXML());
        System.out.println(new KeywordToken("Class").toString());
        System.out.println(new KeywordToken("Do").toXML());
        System.out.println(KeywordToken.isValid("Do"));
        System.out.println(KeywordToken.isValid("Don"));
        System.out.println(new KeywordToken("if").toXML());
    }
}
