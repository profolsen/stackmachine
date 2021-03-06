import assembler.Assembler;
import virtualmachine.StackMachine;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

/*
MIT License

Copyright (c) 2017 Paul Olsen

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
public class Footnote {

    private static final int DEFAULT_MEMORY_SIZE = 256;

    static class Options {
        boolean sym;
        boolean lines;
        int memory = DEFAULT_MEMORY_SIZE;
        int message;
        File in;
        PrintStream out;
        boolean assemble = true;
        boolean alsoRun = false;
        String infile;
    }

    private static final int VERSION_REQUEST = 1;
    private static final int NO_MESSAGE = 0;
    private static final int BAD_ARGUMENT = 2;
    private static final String currentVersion = "Footnote version 0.1";

    public static void main(String[] args) {
        Options options = getOptions(args);
        if(options.message == BAD_ARGUMENT) {
            printHelp();
            return;
        } else if(options.message == VERSION_REQUEST) {
            System.out.println(currentVersion);
        } else {//no message
            if(options.assemble) {
                Assembler assembler = null;
                try {
                    assembler = new Assembler(options.in);
                } catch (FileNotFoundException e) {
                    System.out.println("Could not open file: " + options.in);
                }
                assembler.assemble();
                for(String i : assembler.program()) {
                    options.out.println(i);
                }
                if(options.sym) {
                    try {
                        PrintStream ps = new PrintStream(new FileOutputStream(new File("symbols.txt")));
                        for(String symbol : assembler.symbolTable().keySet()) {
                            ps.println(symbol + ", " + assembler.symbolTable().get(symbol));
                        }
                    } catch (FileNotFoundException e) {
                        System.out.println("Failed to store symbol table.");
                    }
                }
                if(options.lines) {
                    try {
                        PrintStream ps = new PrintStream(new FileOutputStream(new File("linemap.txt")));
                        for(int pc : assembler.lineMap().keySet()) {
                            ps.println(pc + ", " + assembler.lineMap().get(pc));
                        }
                    } catch (FileNotFoundException e) {
                        System.out.println("Failed to store symbol table.");
                    }
                }
            }
            if((!options.assemble) || options.alsoRun) {
                if(options.assemble) {
                    options.in = new File(dir(options.infile).getPath() + "/" + file(options.infile) + ".i");
                }
                StackMachine m = new StackMachine(options.memory);
                try {
                    m.load(new Scanner(options.in));
                } catch (FileNotFoundException e) {
                    System.out.println("Could not open " + options.in);
                }
                m.run();
                return;
            }
        }
    }

    private static void printHelp() {
        System.out.println("Usage: Footnote [options] infile [outfile]");
        System.out.println("Options: ");
        System.out.println("\t-version .... prints a version message and quits.\n\t  All other arguments are ignored.");
        System.out.println("\t-sym .... saves symbol table information to a file symbols.txt.");
        System.out.println("\t-lines .... saves a map from instructions in the assembled file to lines of code in the unassembled file.\n" +
                "\t This information to a file symbols.txt.");
        System.out.println("\t-memory amount .... sets the amount of memory.  amount is the number of 32-bit integers available to the virtual machine.\n" +
                "\tIf this option is not given, the amount of memory is 256.");
    }

    private static Options getOptions(String[] args) {
        String infilename = null;
        Options answer = new Options();
        answer.message = NO_MESSAGE;
        answer.memory = DEFAULT_MEMORY_SIZE;
        boolean infile_set = false;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-version")) {
                answer.message = VERSION_REQUEST;
                return answer;
            } else if(args[i].equals("-sym")) {
                answer.sym = true;
            } else if(args[i].equals("-lines")) {
                answer.lines = true;
            } else if(args[i].equals("-memory")) {
                if(i+1 == args.length) {
                    System.out.println("-memory: no amount specified.");
                    answer.message = BAD_ARGUMENT;
                    return answer;
                } else {
                    try {
                        answer.memory = Integer.parseInt(args[++i]);
                        if(answer.memory < 0) {
                            System.out.println("Memory amount must be positive.");
                            answer.message = BAD_ARGUMENT;
                            return answer;
                        }
                    } catch(NumberFormatException nfe) {
                        System.out.println("-memory: cannot construct virtual machine with memory limit of " + args[i]);
                        answer.message = BAD_ARGUMENT;
                        return answer;
                    }
                }
            } else if(!infile_set) {
                File[] files = getPossibleInputs(file(args[i]), dir(args[i]).listFiles());
                setAll(answer, files);
                infilename = args[i];
                answer.infile = args[i];
                infile_set = true;
            } else {  //it has to be the outfile.
                try {
                    answer.out = new PrintStream(new FileOutputStream(new File(dir(args[i]).getPath() + "/" + file(args[i]) + ".i")));
                } catch (FileNotFoundException e) {
                    System.out.println("Could not open file" + args[i]);
                    answer.message = BAD_ARGUMENT;
                    return answer;
                }
            }
        }
        if((answer.sym || answer.lines) && !answer.assemble) {
            System.out.println("-sym and -lines can only be used during assembly.");
        }
        if(answer.in == null) {
            System.out.println("No input file specified.");
            answer.message = BAD_ARGUMENT;
            return answer;
        }
        if(answer.out == null && answer.assemble) {
            try {
                answer.out = new PrintStream(new FileOutputStream(new File(file(infilename) + ".i")));
            } catch (FileNotFoundException e) {
                System.out.println("Could not open " + infilename + ".i");
                answer.message = BAD_ARGUMENT;
            }
        }
        return answer;
    }

    private static String file(String arg) {
        if(arg.contains("/")) {
            arg = arg.substring(arg.lastIndexOf('/') + 1);
        }
        return arg;
    }

    private static File dir(String arg) {
        if(arg.contains("/")) {
            if(arg.startsWith("/")) return new File(arg.substring(0, arg.lastIndexOf('/') + 1));
            arg = "/" + arg;
            arg = System.getProperty("user.dir") + arg.substring(0, arg.lastIndexOf('/') + 1);
            return new File(arg);
        }
        return new File(System.getProperty("user.dir"));
    }

    private static void setAll(Options answer, File[] files) {
        if(files[0] != null) {
            answer.in = files[0];
            if (files[1] != null) {
                answer.alsoRun = true;
                try {
                    answer.out = new PrintStream(new FileOutputStream(files[1]));
                } catch (FileNotFoundException e) {
                    System.out.println("Could not open file: " + files[1]);
                    answer.message = BAD_ARGUMENT;
                }
            }
        } else if(files[1] != null) {
            answer.assemble = false;  //this is the running a program case.
            answer.in = files[1];
        }
    }

    private static File[] getPossibleInputs(String arg, File[] files) {
        File[] answer;
        File footnote = null;
        File i = null;
        for(File f : files) {
            if(f.getName().equals(arg + ".ftnt")) {
                footnote = f;
            } else if(f.getName().equals(arg + ".i")) {
                i = f;
            }
        }
        answer = new File[2];
        if(footnote == null) {
            answer[1] = i;
        } else if(i == null) {
            answer[0] = footnote;
        } else {
            answer[0] = footnote;
            answer[1] = i;
        }
        return answer;
    }
}
