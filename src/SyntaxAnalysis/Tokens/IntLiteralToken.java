package SyntaxAnalysis.Tokens;

public class IntLiteralToken implements Token {

    private static final int LOWER_BOUND = 0;
    private static final int UPPER_BOUND = 32767;
    public static boolean isValid(String value) {
        boolean valid = false;

        if(value.matches("^[0-9]*$")) {
            int i = Integer.parseInt(value);

            valid = (i >= LOWER_BOUND && i <= UPPER_BOUND);
        }

        return valid;
    }

    private int value;
    public IntLiteralToken(String value) {
        if(isValid(value)) {
            this.value = Integer.parseInt(value);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toXML() {
        return "<integerConstant> " + value + " </integerConstant>";
    }
}
