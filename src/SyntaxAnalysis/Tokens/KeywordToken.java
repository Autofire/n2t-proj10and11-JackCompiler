package SyntaxAnalysis.Tokens;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class KeywordToken implements Token {

    //region Statics and enums
    public enum Keyword {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR,
        INT, BOOLEAN, CHAR, VOID,
        VAR, STATIC, FIELD, LET, DO,
        IF, ELSE, WHILE, RETURN,
        TRUE, FALSE, NULL, THIS
    }
    private static final Map<String, Keyword> nameIndex =
            new HashMap<String, Keyword>(Keyword.values().length);
    static {
        for (Keyword suit : Keyword.values()) {
            nameIndex.put(suit.name(), suit);
        }
    }
    public static Keyword lookupByName(String name) {
        return nameIndex.get(name);
    }

    public static boolean isValid(String word) {
        return lookupByName(word.toUpperCase()) != null;
    }
    //endregion

    private Keyword kw;

    public KeywordToken(String word) {
        kw = Keyword.valueOf(word.toUpperCase());
    }

    @Override
    public String toXML() {
        return "<keyword> " + kw.name().toLowerCase() + " </keyword>";
    }

    @Override
    public String toString() {
        return kw.toString();
    }
}
