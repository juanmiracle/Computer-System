import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;

/**
 * 
 * This class gets the input and output file and prints out the corresponding .xml into the output.
 */

public class CompilationEngine {

    private VMWriter mVMWriter;
    private SymbolTable mSymbolTable;
    private JackTokenizer mJackTokenizer;
    private static HashSet<Character> opSet = new HashSet<>();
    private int counter;
    private String currClass;
    private String currSubroutine;

    static {
        opSet.add('+');
        opSet.add('-');
        opSet.add('*');
        opSet.add('/');
        opSet.add('&');
        opSet.add('|');
        opSet.add('<');
        opSet.add('>');
        opSet.add('=');
    }

    // Creates a new compilation engine with the given input and output.
    public CompilationEngine(File inFile, File outFile) throws FileNotFoundException {

        mJackTokenizer = new JackTokenizer(inFile);
        mVMWriter = new VMWriter(outFile);
        mSymbolTable = new SymbolTable();
        counter = 0;
    }

    // Compiles a complete class
    public void compileClass() {
        mJackTokenizer.advance();

        // class
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.CLASS) {
            mJackTokenizer.advance();
        }

        // className
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER) {
            currClass = mJackTokenizer.identifier();
            mJackTokenizer.advance();
        }

        // {
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL && mJackTokenizer.symbol() == '{') {
            mJackTokenizer.advance();
        }

        // classVarDec
        while (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.STATIC ||
                        mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.FIELD)) {
            compileClassVarDec();
        }

        // subroutineDec
        while (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.CONSTRUCTOR ||
                        mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.FUNCTION ||
                        mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.METHOD)) {
            compileSubroutine();
        }

        // }
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL && mJackTokenizer.symbol() == '}') {
        }

        // end of class
        mVMWriter.close();
    }

    // Compiles a static declaration of a field declaration
    private void compileClassVarDec() {

        SymbolTable.Kind kind;
        if (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.STATIC) {
            kind = SymbolTable.Kind.STATIC;
        }
        // mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.FIELD
        else {
            kind = SymbolTable.Kind.FIELD;
        }
        mJackTokenizer.advance();
        String type = compileType();
        mJackTokenizer.advance();
        compileMultiVarNames(kind, type);
    }

    // Compiles a type
    private String compileType() {
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD && (
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.INT ||
                        mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.CHAR ||
                        mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.BOOLEAN)) {
            return mJackTokenizer.keyWordToken();
        }
        //mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER
        else {
            return mJackTokenizer.identifier();
        }
    }

    // Compiles a complete method, function, or constructor
    private void compileSubroutine() {
        // keyword = constructor / function / method
        JackTokenizer.jackKeyWord keyword = mJackTokenizer.keyWord();
        mJackTokenizer.advance();
        mSymbolTable.startSubroutine();

        if (keyword == JackTokenizer.jackKeyWord.METHOD) {
            mSymbolTable.define("this", currClass, SymbolTable.Kind.ARG);
        }

        // void | type
        String type;
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.VOID) {
            type = "void";
            mJackTokenizer.advance();
        }
        else {
            type = compileType();
            mJackTokenizer.advance();
        }

        // subroutine name

        currSubroutine = mJackTokenizer.identifier();
        mJackTokenizer.advance();

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '(') {
            mJackTokenizer.advance();
        }

        if (mJackTokenizer.tokenType() != JackTokenizer.tokenType.SYMBOL) {
            compileParameterList();
        }

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == ')') {
            mJackTokenizer.advance();
        }

        compileSubroutineBody(keyword);
    }

    // Compiles the subroutine body
    private void compileSubroutineBody(JackTokenizer.jackKeyWord keyword) {

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '{') {
            mJackTokenizer.advance();
        }

        while (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.VAR) {
            compileVarDec();
        }

        mVMWriter.writeFunction(currClass + "." + currSubroutine, mSymbolTable.varCount(SymbolTable.Kind.VAR));

        // load THIS for METHOD and CONSTRUCTOR
        if (keyword == JackTokenizer.jackKeyWord.METHOD) {
            mVMWriter.writePush(VMWriter.Segment.ARG, 0);
            mVMWriter.writePop(VMWriter.Segment.POINTER, 0);
        }
        else if (keyword == JackTokenizer.jackKeyWord.CONSTRUCTOR) {
            mVMWriter.writePush(VMWriter.Segment.CONST, mSymbolTable.varCount(SymbolTable.Kind.FIELD));
            mVMWriter.writeCall("Memory.alloc", 1);
            mVMWriter.writePop(VMWriter.Segment.POINTER, 0);
        }

        while (mJackTokenizer.tokenType() != JackTokenizer.tokenType.SYMBOL) {
            compileStatements();
        }

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '}') {
            mJackTokenizer.advance();
        }
    }

    // Compiles a parameter list, could be empty.
    private void compileParameterList() {
        while (true) {
            String type = compileType();
            mJackTokenizer.advance();

            // varName
            mSymbolTable.define(mJackTokenizer.identifier(), type, SymbolTable.Kind.ARG);
            mJackTokenizer.advance();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ',') {
                mJackTokenizer.advance();
            }
            else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ')') {
                break;
            }
        }

    }

    // Compiles a var declaration
    private void compileVarDec() {

        mJackTokenizer.advance();
        // type
        String type = compileType();
        mJackTokenizer.advance();
        // varName
        compileMultiVarNames(SymbolTable.Kind.VAR,type);
    }

    // Compiles a sequence of varNames
    private void compileMultiVarNames(SymbolTable.Kind kind, String type) {
        while (true) {
            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER) {
                String name = mJackTokenizer.identifier();
                mSymbolTable.define(name, type, kind);
                mJackTokenizer.advance();
            }

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ',') {
                mJackTokenizer.advance();
            }
            else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ';') {
                mJackTokenizer.advance();
                break;
            }
        }
    }

    // Compiles a sequence of statements
    private void compileStatements() {
        while (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD) {
            if (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.LET) {
                compileLet();
            }
            else if (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.IF) {
                compileIf();
            }
            else if (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.WHILE) {
                compileWhile();
            }
            else if (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.DO) {
                compileDo();
            }
            else if (mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.RETURN) {
                compileReturn();
            }
        }
    }

    // Compiles a do statemenet
    private void compileDo() {

        mJackTokenizer.advance();

        compileSubroutineCall();

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == ';') {
            mJackTokenizer.advance();
        }
        mVMWriter.writePop(VMWriter.Segment.TEMP, 0);
    }

    // Compiles a subroutine call
    private void compileSubroutineCall() {
        String name = "";
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER) {
            name = mJackTokenizer.identifier();
            mJackTokenizer.advance();
        }
        int nArgs = 0;
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '(') {
            mVMWriter.writePush(VMWriter.Segment.POINTER, 0);
            mJackTokenizer.advance();

            nArgs = compileExpressionList() + 1;

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ')') {
                mJackTokenizer.advance();
            }
            mVMWriter.writeCall(currClass + "." + name, nArgs);
        }
        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '.') {
            String objName = name;
            mJackTokenizer.advance();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER) {
                name = mJackTokenizer.identifier();
                mJackTokenizer.advance();
            }

            String type = mSymbolTable.typeOf(objName);
            if (type == null) {
                name = objName + "." + name;
            }
            else {
                nArgs = 1;
                mVMWriter.writePush(getSeg(mSymbolTable.kindOf(objName)), mSymbolTable.indexOf(objName));
                name = mSymbolTable.typeOf(objName) + "." + name;
            }

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == '(') {
                mJackTokenizer.advance();
            }

            nArgs += compileExpressionList();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ')') {
                mJackTokenizer.advance();
            }
            mVMWriter.writeCall(name, nArgs);
        }
    }

    private VMWriter.Segment getSeg(SymbolTable.Kind kind) {
        if (kind == SymbolTable.Kind.FIELD) {
            return VMWriter.Segment.THIS;
        }
        else if (kind == SymbolTable.Kind.STATIC) {
            return VMWriter.Segment.STATIC;
        }
        else if (kind == SymbolTable.Kind.VAR) {
            return VMWriter.Segment.LOCAL;
        }
        // kind == SymbolTable.Kind.ARG
        else {
            return VMWriter.Segment.ARG;
        }
    }

    // Compiles a let statement
    private void compileLet() {

        mJackTokenizer.advance();
        String varName = "";
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER) {
            varName = mJackTokenizer.identifier();
            mJackTokenizer.advance();
        }
        boolean flag = false;

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '[') {
            flag = true;
            mVMWriter.writePush(getSeg(mSymbolTable.kindOf(varName)), mSymbolTable.indexOf(varName));
            mJackTokenizer.advance();

            compileExpression();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ']') {
                mJackTokenizer.advance();
            }

            mVMWriter.writeArithmetic(VMWriter.Command.ADD);
        }

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '=') {
            mJackTokenizer.advance();

            compileExpression();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ';') {
                mJackTokenizer.advance();
            }
        }

        // if it's an array
        if (flag) {
            mVMWriter.writePop(VMWriter.Segment.TEMP, 0);
            mVMWriter.writePop(VMWriter.Segment.POINTER, 1);
            mVMWriter.writePush(VMWriter.Segment.TEMP, 0);
            mVMWriter.writePop(VMWriter.Segment.THAT, 0);
        }
        else {
            mVMWriter.writePop(getSeg(mSymbolTable.kindOf(varName)), mSymbolTable.indexOf(varName));
        }
    }

    // Compiles a while statement
    private void compileWhile() {
        String label1 = label();
        String label2 = label();
        mVMWriter.writeLabel(label1);
        mJackTokenizer.advance();

        compileParentExpressionParent();
        mVMWriter.writeArithmetic(VMWriter.Command.NOT);
        mVMWriter.writeIf(label2);

        compileBracketStatementBracket();
        mVMWriter.writeGoto(label1);
        mVMWriter.writeLabel(label2);
    }

    private String label() {
        return "LABEL_" + (counter++);
    }

    // Compiles a return statement
    private void compileReturn() {

        mJackTokenizer.advance();

        if (mJackTokenizer.tokenType() != JackTokenizer.tokenType.SYMBOL) {
            compileExpression();
        }
        else {
            // push 0 to stack
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
        }

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == ';') {
            mJackTokenizer.advance();
        }
        mVMWriter.writeReturn();
    }

    // Compiles an if statement
    private void compileIf() {
        String label1 = label();
        String label2 = label();

        mJackTokenizer.advance();

        compileParentExpressionParent();
        mVMWriter.writeArithmetic(VMWriter.Command.NOT);
        mVMWriter.writeIf(label1);

        compileBracketStatementBracket();
        mVMWriter.writeGoto(label2);
        mVMWriter.writeLabel(label1);

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.ELSE) {
            mJackTokenizer.advance();
            compileBracketStatementBracket();
        }
        mVMWriter.writeLabel(label2);
    }

    // Compiles a statement in a pair of brackets
    private void compileBracketStatementBracket() {
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '{') {
            mJackTokenizer.advance();
            compileStatements();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == '}') {
                mJackTokenizer.advance();
            }
        }
    }

    // Compiles an expression within a pair of parentheses
    private void compileParentExpressionParent() {
        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '(') {
            mJackTokenizer.advance();

            compileExpression();

            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ')') {
                mJackTokenizer.advance();
            }
        }
    }

    // Compiles an expression
    private void compileExpression() {
        compileTerm();

        String op = "";
        while (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                opSet.contains(mJackTokenizer.symbol())) {
            if (mJackTokenizer.symbol() == '>') {
                op = "gt";
            }
            else if (mJackTokenizer.symbol() == '<') {
                op = "lt";
            }
            else if (mJackTokenizer.symbol() == '=') {
                op = "eq";
            }
            else if (mJackTokenizer.symbol() == '&') {
                op = "and";
            }
            else if (mJackTokenizer.symbol() == '|') {
                op = "or";
            }
            else if (mJackTokenizer.symbol() == '+'){
                op = "add";
            }
            else if (mJackTokenizer.symbol() == '-') {
                op = "sub";
            }
            else if (mJackTokenizer.symbol() == '*') {
                op = "call Math.multiply 2";
            }
            //mJackTokenizer.symbol() == '/'
            else {
                op = "call Math.divide 2";
            }
            mJackTokenizer.advance();
            compileTerm();
            mVMWriter.printLine(op);
        }
    }

    // Compiles a term
    // term: integerConstant | stringConstant | keywordConstant | varName | varName [ expression ]
    // subroutineCall | ( expression ) | unaryOp term
    private void compileTerm() {

        if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.INT_CONST) {
            mVMWriter.writePush(VMWriter.Segment.CONST, mJackTokenizer.intVal());
            mJackTokenizer.advance();
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.STRING_CONST) {
            String string = mJackTokenizer.stringVal();
            // string is an array of char's
            mVMWriter.writePush(VMWriter.Segment.CONST, string.length());
            mVMWriter.writeCall("String.new", 1);

            for (int i = 0; i < string.length(); i++) {
                mVMWriter.writePush(VMWriter.Segment.CONST, (int)string.charAt(i));
                mVMWriter.writeCall("String.appendChar", 2);
            }

            mJackTokenizer.advance();
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.TRUE) {
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
            mVMWriter.writeArithmetic(VMWriter.Command.NOT);
            mJackTokenizer.advance();
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD && (
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.FALSE ||
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.NULL)) {
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
            mJackTokenizer.advance();
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.KEYWORD &&
                mJackTokenizer.keyWord() == JackTokenizer.jackKeyWord.THIS) {
            mVMWriter.writePush(VMWriter.Segment.POINTER, 0);
            mJackTokenizer.advance();
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '(') {
            compileParentExpressionParent();
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '-') {
            mJackTokenizer.advance();
            compileTerm();
            mVMWriter.writeArithmetic(VMWriter.Command.NEG);
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                mJackTokenizer.symbol() == '~') {
            mJackTokenizer.advance();
            compileTerm();
            mVMWriter.writeArithmetic(VMWriter.Command.NOT);
        }

        else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.IDENTIFIER) {
            String id = mJackTokenizer.identifier();
            mJackTokenizer.advance();

            // varName [ expression ]
            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == '[') {
                mVMWriter.writePush(getSeg(mSymbolTable.kindOf(id)), mSymbolTable.indexOf(id));
                mJackTokenizer.advance();

                compileExpression();

                if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                        mJackTokenizer.symbol() == ']') {
                    mVMWriter.writeArithmetic(VMWriter.Command.ADD);
                    mVMWriter.writePop(VMWriter.Segment.POINTER, 1);
                    mVMWriter.writePush(VMWriter.Segment.THAT, 0);
                    mJackTokenizer.advance();
                }
            }
            else if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL && (
                    mJackTokenizer.symbol() == '(' ||
                            mJackTokenizer.symbol() == '.')) {
                mJackTokenizer.goBack();
                compileSubroutineCall();
            }
            else {
                mVMWriter.writePush(getSeg(mSymbolTable.kindOf(id)), mSymbolTable.indexOf(id));
            }
        }
    }

    // Compiles a comma-seperated list of expressions
    private int compileExpressionList() {
        int nArgs = 0;

        while (mJackTokenizer.tokenType() != JackTokenizer.tokenType.SYMBOL ||
                mJackTokenizer.symbol() != ')') {
            compileExpression();
            nArgs++;
            if (mJackTokenizer.tokenType() == JackTokenizer.tokenType.SYMBOL &&
                    mJackTokenizer.symbol() == ',') {
                mJackTokenizer.advance();
            }
        }
        return nArgs;
    }
}
