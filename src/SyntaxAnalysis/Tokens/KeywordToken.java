package SyntaxAnalysis.Tokens;

import SyntaxAnalysis.KeywordType;

import java.util.HashMap;
import java.util.Map;

public class KeywordToken extends AbstractToken {

    private static final Map<String, KeywordType> nameIndex =
            new HashMap<String, KeywordType>(KeywordType.values().length);
    static {
        for (KeywordType suit : KeywordType.values()) {
            nameIndex.put(suit.name(), suit);
        }
    }
    public static KeywordType lookupByName(String name) {
        return nameIndex.get(name);
    }

    public static boolean isValid(String word) {
        return lookupByName(word.toUpperCase()) != null;
    }
    //endregion

    private KeywordType kw;

    public KeywordToken(int line, String word) {
        super(line);

        kw = KeywordType.valueOf(word.toUpperCase());
    }

    public KeywordType getValue() {
        return kw;
    }

    @Override
    public String toString() {
        return kw.toString();
    }

    @Override
    public String toXML() {
        return "<keyword> " + kw.name().toLowerCase() + " </keyword>";
    }

}
