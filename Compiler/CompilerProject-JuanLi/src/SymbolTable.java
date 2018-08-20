import java.util.HashMap;

/**
 */
public class SymbolTable {

    private HashMap<String, Symbol> classSymbols;
    private HashMap<String, Symbol> subroutineSymbols;
    private HashMap<Kind, Integer> indices;

    public enum Kind {
        STATIC, FIELD, ARG, VAR
    }

    // inner Symbol class;
    private class Symbol {

        private String type;
        private Kind kind;
        private int index;

        public Symbol(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    public SymbolTable() {

        classSymbols = new HashMap<>();
        subroutineSymbols = new HashMap<>();
        indices = new HashMap<>();
        indices.put(Kind.ARG, 0);
        indices.put(Kind.FIELD, 0);
        indices.put(Kind.STATIC, 0);
        indices.put(Kind.VAR, 0);
    }

    public void startSubroutine() {

        subroutineSymbols.clear();
        indices.put(Kind.VAR, 0);
        indices.put(Kind.ARG, 0);
    }

    public void define(String name, String type, Kind kind) {

        Symbol mSymbol = new Symbol(type, kind, indices.get(kind));
        indices.put(kind, indices.get(kind) + 1);
        if (kind == Kind.ARG || kind == Kind.VAR) {
            subroutineSymbols.put(name, mSymbol);
        }
        // kind == Kind.STATIC || kind == Kind.FIELD
        else {
            classSymbols.put(name, mSymbol);
        }
    }

    public int varCount(Kind kind) {
        return indices.get(kind);
    }

    public Kind kindOf(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name).kind;
        }
        else if (classSymbols.containsKey(name)){
            return classSymbols.get(name).kind;
        }
        else {
            return null;
        }
    }

    public String typeOf(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name).type;
        }
        else if (classSymbols.containsKey(name)){
            return classSymbols.get(name).type;
        }
        else {
            return null;
        }
    }

    public int indexOf(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name).index;
        }
        else if (classSymbols.containsKey(name)){
            return classSymbols.get(name).index;
        }
        else {
            return -1;
        }
    }
}
