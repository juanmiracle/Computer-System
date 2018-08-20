import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * 
 * This is the driver for the CompilationEngine, which takes an input file or directory and parse
 * the .jack files into .xml.
 */
public class JackCompiler {

    private static CompilationEngine mCompilationEngine;
    private static ArrayList<File> files = new ArrayList<>();
    private static String outFileName = "";

    // Gets the input file and initialize the corresponding CompilationEngine
    public static void main(String[] args) throws FileNotFoundException {
        String inFileName = args[0];
        File inFile = new File(inFileName);

        if (inFile.isFile()) {
            files.add(inFile);
        }
        else if (inFile.isDirectory()){
            for (File file : inFile.listFiles()) {
                if (file.getName().endsWith(".jack")) {
                    files.add(file);
                }
            }
        }

        for (File file : files) {
            outFileName = file.getPath().substring(0, file.getPath().length() - 5) + ".vm";
            mCompilationEngine = new CompilationEngine(file, new File(outFileName));
            mCompilationEngine.compileClass();
        }
    }
}
