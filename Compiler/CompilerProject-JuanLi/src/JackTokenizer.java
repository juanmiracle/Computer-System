import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * This class is to tokenize the jack language and get the type of it.
 */
public class JackTokenizer {

    private ArrayList<String> content;
    private int curr;
    public String currToken;

    public enum tokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    }

    public enum jackKeyWord {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID, VAR, STATIC,
        FIELD, LET, DO, IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS
    }

    private static HashMap<String, jackKeyWord> keyWordMap = new HashMap<>();
    private static Pattern tokenPattern;
    private static String keyWordPattern;
    private static String symbolPattern = "[\\&\\*\\+\\(\\)\\.\\/\\,\\-\\]\\;\\~\\}\\|\\{\\>\\=\\[\\<]";
    private static String intPattern = "[0-9]+";
    private static String strPattern = "\"[^\"\n]*\"";
    private static String idPattern = "[\\w_]+";

    static {
        keyWordMap.put("class", jackKeyWord.CLASS);
        keyWordMap.put("method", jackKeyWord.METHOD);
        keyWordMap.put("function", jackKeyWord.FUNCTION);
        keyWordMap.put("constructor", jackKeyWord.CONSTRUCTOR);
        keyWordMap.put("int", jackKeyWord.INT);
        keyWordMap.put("boolean", jackKeyWord.BOOLEAN);
        keyWordMap.put("char", jackKeyWord.CHAR);
        keyWordMap.put("void", jackKeyWord.VOID);
        keyWordMap.put("var", jackKeyWord.VAR);
        keyWordMap.put("static", jackKeyWord.STATIC);
        keyWordMap.put("field", jackKeyWord.FIELD);
        keyWordMap.put("let", jackKeyWord.LET);
        keyWordMap.put("do", jackKeyWord.DO);
        keyWordMap.put("if", jackKeyWord.IF);
        keyWordMap.put("else", jackKeyWord.ELSE);
        keyWordMap.put("while", jackKeyWord.WHILE);
        keyWordMap.put("return", jackKeyWord.RETURN);
        keyWordMap.put("true", jackKeyWord.TRUE);
        keyWordMap.put("false", jackKeyWord.FALSE);
        keyWordMap.put("null", jackKeyWord.NULL);
        keyWordMap.put("this", jackKeyWord.THIS);

        for (String key: keyWordMap.keySet()) {
            keyWordPattern += key + "|";
        }
        tokenPattern = Pattern.compile(idPattern + "|" + keyWordPattern + symbolPattern + "|" + intPattern + "|" + strPattern);
    }

    // Opens the input file/stream and gets ready to tokenize it
    public JackTokenizer(File inFile) throws FileNotFoundException {
        content = new ArrayList<>();
        curr = -1;
        StringBuilder contentString = new StringBuilder();
        Scanner in = new Scanner(inFile);

        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (!line.trim().equals("//")) {
                String noComments = line.split("//")[0];
                if (noComments.trim().length() > 0) {
                    contentString.append(noComments + "\n");
                }
            }
        }
        String input = trimAPIComments(contentString.toString());
        in.close();

        Matcher matcher = tokenPattern.matcher(input);
        while (matcher.find()) {
            content.add(matcher.group());
        }
    }

    // helper functio to trim the API comments
    private String trimAPIComments(String contentString) {
        int start = contentString.indexOf("/*");
        if (start == - 1) {
            return contentString;
        }

        int end = contentString.indexOf("*/");
        String result = contentString;

        while (start != -1) {

            result = result.substring(0, start) + result.substring(end + 2);
            start = result.indexOf("/*");
            end = result.indexOf("*/");
        }

        return result;

    }

    // Do we have more tokens in the input?
    public boolean hasMoreTokens() {
        return curr < content.size() - 1;
    }

    // Gets the next token
    public void advance() {
        if (hasMoreTokens()) {
            curr++;
            currToken = content.get(curr);
        }
    }

    // Returns the type of the current token
    public tokenType tokenType() {
        if (keyWordMap.keySet().contains(currToken)) {
            return tokenType.KEYWORD;
        }
        else if (currToken.matches(symbolPattern)) {
            return tokenType.SYMBOL;
        }
        else if (currToken.matches(intPattern)) {
            return tokenType.INT_CONST;
        }
        else if (currToken.matches(strPattern)) {
            return tokenType.STRING_CONST;
        }
        // if (currToken.matches(idPattern))
        else {
            return tokenType.IDENTIFIER;
        }
    }

    // Returns the keyword if it's the type
    public jackKeyWord keyWord() {
        return keyWordMap.get(currToken);
    }

    // Returns the keyword string
    public String keyWordToken() {
        return currToken;
    }

    // Returns the symbol if it's the type
    public char symbol() {
        return currToken.charAt(0);
    }

    // Returns the identifier if it's the type
    public String identifier() {
        return currToken;
    }

    // Returns the integer constant if it's the type
    public int intVal() {
        return Integer.parseInt(currToken);
    }

    // Returns the string constant if it's the type
    public String stringVal() {
        return currToken.substring(1, currToken.length() - 1);
    }

    // Go back one line in the token input
    public void goBack() {
        curr--;
        currToken = content.get(curr);
    }
}
