package SyntaxAnalysis.Tokens;

public class SymbolToken implements Token {

    public static final String ALL_SYMBOLS = "{}()[].,;+-*/&|<>=~";
    public static boolean isValid(char value) {
        return ALL_SYMBOLS.contains(Character.toString(value));
    }
    public static boolean isValid(String value) {
        return value.length() == 1 && isValid(value.charAt(0));
    }

    private char symbol;

    public SymbolToken(String value) {
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
