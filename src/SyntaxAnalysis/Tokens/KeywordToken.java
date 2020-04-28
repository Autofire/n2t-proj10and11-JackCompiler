package SyntaxAnalysis.Tokens;
//  Author: Daniel Edwards
//   Class: CS 3650 (Section 1)
// Project: 10 & 11
//     Due: 04/29/2020

import SyntaxAnalysis.KeywordType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeywordToken extends AbstractToken {

    public static final List<KeywordType> KEYWORD_CONSTS = Arrays.asList(
        new KeywordType[]{
            KeywordType.TRUE, KeywordType.FALSE, KeywordType.NULL, KeywordType.THIS
        }
    );

    // region String conversion and parsing
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
    private String originalWord;

    public KeywordToken(int line, String word) {
        super(line);

        originalWord = word;
        kw = KeywordType.valueOf(word.toUpperCase());
    }

    public boolean isConst() {
        return KEYWORD_CONSTS.contains(kw);
    }

    public KeywordType getValue() {
        return kw;
    }

    @Override
    public String toString() {
        return originalWord;
    }

    @Override
    public String toXML() {
        return "<keyword> " + originalWord + " </keyword>";
    }

}
