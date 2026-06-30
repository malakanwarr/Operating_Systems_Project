package OS;

import java.util.List;

public class Process {
    int processID;
    String state;
    int programCounter;
    int memStart;
    int memEnd;
    List<String> programLines;
    int arrivalTime; //required to know at which clock cycle does the process arrive(given in description) 
    boolean isOnDisk;
    int waitingTime;  // required for HRRN
    int burstTime;    // required for HRRN
    int variableCount = 0;
    int queueLevel = 0; //required for MLFQ


    public static final String READY    = "ready";
    public static final String RUNNING  = "running";
    public static final String BLOCKED  = "blocked";
    public static final String FINISHED = "finished";  // we do it like this instead of enum because state gets written to memory as a string. 

 
    public Process(int processID, List<String> programLines,
                   int arrivalTime, int memStart, int memEnd) {
        this.processID      = processID;
        this.programLines   = programLines;
        this.arrivalTime    = arrivalTime;
        this.memStart       = memStart;
        this.memEnd         = memEnd;
        this.state          = READY;
        this.programCounter = 0;
        this.isOnDisk       = false;
        this.waitingTime    = 0;
        this.burstTime      = programLines.size();
    }

  
    // returns the line the program counter points to
    public String getCurrentLine() {
        if (programCounter < programLines.size()) {
            return programLines.get(programCounter);
        }
        return null;
    }

   
    public boolean isFinished() {
        return programCounter >= programLines.size();
    }

   
    // call this after every instruction executes
    public void incrementPC() {
        programCounter++;
        burstTime--;
    }


    // called once when process is first created where we writes PCB fields, code lines and variable slots in memory
    public void loadIntoMemory(Memory memory) {
        String p = "P" + processID + "_";
        // write PCB fields (5 slots)
        memory.write(memStart,     p + "pid",      String.valueOf(processID));
        memory.write(memStart + 1, p + "state",    state);
        memory.write(memStart + 2, p + "pc",       String.valueOf(programCounter));
        memory.write(memStart + 3, p + "memStart", String.valueOf(memStart));
        memory.write(memStart + 4, p + "memEnd",   String.valueOf(memEnd));
        // write code lines (after PCB)
        int codeStart = memStart + 5;
        for (int i = 0; i < programLines.size(); i++) {
            memory.write(codeStart + i, p + "line" + i, programLines.get(i));
        }

        // reserve 3 variable slots (after code)
        int varStart = codeStart + programLines.size();
        for (int i = 0; i < 3; i++) {
            memory.write(varStart + i, p + "var_slot" + i, "");
        }
    }

    // call whenever state changes
    public void updateStateInMemory(Memory memory, String newState) {
        state = newState;
        memory.update("P" + processID + "_state", newState);
    }

    
    // call after every instruction executes
    public void updatePCInMemory(Memory memory) {
        memory.update("P" + processID + "_pc", String.valueOf(programCounter));
    }

    public void addVariable(String name, String value, Memory memory) {
        int varStart = memStart + 5 + programLines.size();

        // Check if variable already exists → update it instead of adding new slot
        for (int i = 0; i < variableCount; i++) {
            MemoryWord slot = memory.getSlot(varStart + i);
            if (slot.key.equals("P" + processID + "_var_" + name)) {
                memory.write(varStart + i, slot.key, value);  // overwrite value, same slot
                return;  // done, don't increment variableCount
            }
        }
        if (variableCount >= 3) {
            System.out.println("ERROR: Process " + processID + " has no free variable slots.");
            return;
        }
        memory.write(varStart + variableCount, "P" + processID + "_var_" + name, value);
        variableCount++;
    }
    // called when interpreter needs to resolve a variable
    public String getVariable(String name, Memory memory) {
        return memory.read("P" + processID + "_var_" + name);
    }

    // called when interpreter reassigns a variable
    public void updateVariable(String name, String value, Memory memory) {
        memory.update("P" + processID + "_var_" + name, value);
    }

    public static int calculateSlotsNeeded(List<String> lines) {
        return 5 + lines.size() + 3;
    }
    //just for now and i think it will help us in gui . 
    @Override
    public String toString() {
        return "Process " + processID +
               " [State=" + state +
               " | PC=" + programCounter +
               " | Mem=" + memStart + "-" + memEnd +
               " | Waiting=" + waitingTime +
               " | Burst=" + burstTime + "]";
    }
}