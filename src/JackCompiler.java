import SyntaxAnalysis.Tokens.*;

public class JackCompiler {

    public static void main(String[] args) {
        System.out.println(StringLiteralToken.isValid("123"));
        System.out.println(StringLiteralToken.isValid("\"123\""));
        System.out.println(StringLiteralToken.isValid("\"\""));
        System.out.println(StringLiteralToken.isValid(""));
        System.out.println(StringLiteralToken.isValid("\"adsf\\\"\""));
        System.out.println(new StringLiteralToken("\"100\"").toXML());
        System.out.println(new StringLiteralToken("\"\"").toXML());
    }
}
