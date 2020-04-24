package SyntaxAnalysis.Tokens;

public class IdentifierToken extends AbstractToken {

    private static final String VALID_INIT = "[A-Za-z_]";
    private static final String VALID_BODY = "[A-Za-z0-9_]";
    public static boolean isValidChar(char value) {
        return Character.toString(value).matches(VALID_BODY);
    }
    public static boolean isValid(String value) {
        return value.matches("^" + VALID_INIT + VALID_BODY + "*$");
    }

    private String identifier;
    public IdentifierToken(int line, String identifier) {
        super(line);

        if(isValid(identifier)) {
            this.identifier = identifier;
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public String getValue() {
        return identifier;
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public String toXML() {
        return "<identifier> " + identifier + " </identifier>";
    }

}
