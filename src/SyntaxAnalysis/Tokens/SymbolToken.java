package SyntaxAnalysis.Tokens;

public class SymbolToken implements Token {

    public static final String ALL_SYMBOLS = "{}()[].,;+-*/&|<>=~";
    public static boolean isValid(char value) {
        return ALL_SYMBOLS.contains(Character.toString(value));
    }

    private char symbol;

    public SymbolToken(char value) {
        if(isValid(value)) {
            symbol = value;
        }
        else {
            throw new IllegalArgumentException();
        }
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
