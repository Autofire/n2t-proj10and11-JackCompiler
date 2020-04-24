package SyntaxAnalysis.Tokens;

public class StringLiteralToken implements Token {

    public static boolean isValid(String value) {
        return value.matches("^\".*\"$") && !value.endsWith("\\\"");
    }

    private String value;
    public StringLiteralToken(String value) {
        if(isValid(value)) {
            this.value = value.substring(1, value.length()-1);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toXML() {
        return "<stringConstant> " + value + " </stringConstant>";
    }
}
