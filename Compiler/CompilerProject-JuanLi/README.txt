1. How to compile the code
The program is written in Java. To compile the code using the terminal, get to the src directory, type in "javac JackCompiler.java JackTokenizer.java CompilationEngine.java SymbolTable.java VMWriter.java”, hit enter. The corresponding .class file will show up in the directory. That's the executable.

code usage:  javac JackCompiler.java


2. How to run the code
In the terminal, type in "java JackCompiler filename” to get the output. filename can be either the the path name of the .jack file, absolute or relative, or the directory that contains the .jack files. If it is in the same directory as the JackCompiler.class file, can just use the file name.



3. A description of the code
This program can take a .jack file, or a directory of .jack files. The output will be the same number of .vm files in the same directory.