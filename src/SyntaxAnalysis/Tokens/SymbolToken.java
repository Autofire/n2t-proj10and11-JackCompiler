package SyntaxAnalysis.Tokens;

public class SymbolToken extends AbstractToken {

    public static final String ALL_SYMBOLS = "{}()[].,;+-*/&|<>=~";
    public static boolean isValid(char value) {
        return ALL_SYMBOLS.contains(Character.toString(value));
    }
    public static boolean isValid(String value) {
        return value.length() == 1 && isValid(value.charAt(0));
    }

    private char symbol;

    public SymbolToken(int line, String value) {
        super(line);

        if(isValid(value)) {
            symbol = value.charAt(0);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public char getValue() {
        return symbol;
    }

    @Override
    public String toString() {
        return Character.toString(symbol);
    }

    @Override
    public String toXML() {
        String xmlOutput;

        switch(symbol) {
            case '<':
                xmlOutput = "&lt";
                break;
            case '>':
                xmlOutput = "&gt";
                break;
            case '&':
                xmlOutput = "&amp";
                break;
            default:
                xmlOutput = symbol + "";
        }

        return "<symbol> " + xmlOutput + " </symbol>";
    }
}
