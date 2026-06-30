package OS;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Interpreter {

    OS os;
    Memory memory;
    Scheduler scheduler;
    SystemCalls systemCalls;

    public Interpreter(OS os, Memory memory, Scheduler scheduler) {
        this.os          = os;
        this.memory      = memory;
        this.scheduler   = scheduler;
        this.systemCalls = new SystemCalls(memory, scheduler);
    }

    // reads a .txt file and returns its lines as a list
    public List<String> readProgramFile(String filename) {
        List<String> lines = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().equals("")) {
                    lines.add(line.trim());
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("ERROR: Could not read file " + filename);
        }
        return lines;
    }

  
    // takes one line and the current running process
    // figures out the command and executes it
    public void execute(String line, Process process) {
        String[] parts  = line.split(" ");
        String command  = parts[0];

        switch (command) {

            case "assign":
                // parts[1] = variable name
                // parts[2] = value OR "input" OR "readFile"
                String varName = parts[1];

                if (parts[2].equals("readFile")) {
                    // special case: assign b readFile a
                    // get filename from variable a
                    String filename = process.getVariable(parts[3], memory);
                    // read the file
                    String data = systemCalls.readFile(filename);
                    // store result in variable b
                    process.addVariable(varName, data, memory);

                } else {
                    // normal case: assign x 5 OR assign x input
                    // systemCalls.assign handles "input" internally
                    systemCalls.assign(varName, parts[2]);
                }
                break;

            case "print":
                // get value of variable from memory
                // system call 5 → read from memory
                String value = process.getVariable(parts[1], memory);
                // system call 3 → print to screen
                systemCalls.print(value);
                break;

            case "printFromTo":
                // get both variable values from memory
                String xVal = process.getVariable(parts[1], memory);
                String yVal = process.getVariable(parts[2], memory);
                systemCalls.PrintFromTo(
                    Integer.parseInt(xVal),
                    Integer.parseInt(yVal)
                );
                break;

            case "writeFile":
                // get filename and data from memory
                String wFilename = process.getVariable(parts[1], memory);
                String wData     = process.getVariable(parts[2], memory);
                systemCalls.writeFile(wFilename, wData);
                break;

            case "readFile":
                // get filename from memory
                String rFilename = process.getVariable(parts[1], memory);
                String rData     = systemCalls.readFile(rFilename);
                systemCalls.print(rData);
                break;

            case "semWait":
                if (parts[1].equals("userInput")) {
                    os.userInput.semWait(process, scheduler, memory);
                } else if (parts[1].equals("userOutput")) {
                    os.userOutput.semWait(process, scheduler, memory);
                } else if (parts[1].equals("file")) {
                    os.file.semWait(process, scheduler, memory);
                }
                break;

            case "semSignal":
                if (parts[1].equals("userInput")) {
                    os.userInput.semSignal(scheduler, memory);
                } else if (parts[1].equals("userOutput")) {
                    os.userOutput.semSignal(scheduler, memory);
                } else if (parts[1].equals("file")) {
                    os.file.semSignal(scheduler, memory);
                }
                break;

            default:
                System.out.println("ERROR: Unknown command " + command);
        }
    }
}
