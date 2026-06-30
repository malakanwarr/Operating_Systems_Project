package OS;

import java.util.ArrayList;
import java.util.List;

public class OS {

    Memory      memory;
    Disk        disk;
    Scheduler   scheduler;
    Interpreter interpreter;
    Mutex       userInput;
    Mutex       userOutput;
    Mutex       file;

    List<Process> allProcesses;

    // tracked for GUI display
    String lastExecutedInstruction = "—";
    int    lastRunningPID          = -1;
    int displayClock = 0;
    public Runnable onBeforeExecute = null;

    int[]     arrivalTimes = {0, 1, 4};
    String[]  programFiles = {"Program_1.txt", "Program_2.txt", "Program_3.txt"};
    boolean[] arrived      = {false, false, false};

    public OS()  {
        memory      = new Memory();
        disk        = new Disk();
        scheduler   = new Scheduler(memory, "RR", 2);
        interpreter = new Interpreter(this, memory, scheduler);
        userInput   = new Mutex(Resource.userInput);
        userOutput  = new Mutex(Resource.userOutput);
        file        = new Mutex(Resource.file);
        allProcesses = new ArrayList<>();
    }
    
    public void setSchedulerType(String algorithm) {
        scheduler = new Scheduler(memory, algorithm, 2);
        interpreter = new Interpreter(this, memory, scheduler);
    }
    
    

    // called when a process arrives at its arrival time
    public void handleArrival(String programFile, int processID, int arrivalTime) {
        List<String> lines       = interpreter.readProgramFile(programFile);
        int          slotsNeeded = Process.calculateSlotsNeeded(lines);
        
        while (memory.allocate(slotsNeeded) == -1) {
            Process victim = scheduler.pickProcessToSwap();
            if (victim == null) {
                System.out.println("[OS] ERROR: Cannot free enough memory for Process " + processID);
                return;
            }
            disk.swapToDisk(victim, memory);
            System.out.println("[OS] Process " + victim.processID + " swapped OUT to make room.");
        }

        // allocate once after enough space guaranteed
        int memStart = memory.allocate(slotsNeeded);
        int memEnd   = memStart + slotsNeeded - 1;

        Process process = new Process(processID, lines, arrivalTime, memStart, memEnd);
        process.loadIntoMemory(memory);
        allProcesses.add(process);
        scheduler.addProcess(process);

        System.out.println("[OS] Process " + processID +
                           " created and loaded at slots " + memStart + "-" + memEnd);
    }

    // called when a swapped out process needs to run
    public void handleSwapIn(Process process) {
        int slotsNeeded = Process.calculateSlotsNeeded(process.programLines);

        while (memory.allocate(slotsNeeded) == -1) {
            Process victim = scheduler.pickProcessToSwap();
            if (victim == null) {
                System.out.println("[OS] ERROR: Cannot swap in Process " + process.processID);
                return;
            }
            disk.swapToDisk(victim, memory);
            System.out.println("[OS] Process " + victim.processID + " swapped OUT to make room.");
        }

        disk.swapFromDisk(process, memory);
        System.out.println("[OS] Process " + process.processID + " swapped IN.");
    }

    // one clock cycle returns false when simulation is done
    public boolean step() {
    	displayClock = scheduler.getClock();
        int clock = scheduler.getClock();
        System.out.println("\n======= CLOCK " + clock + " =======");

        // 1. handle arrivals
        for (int i = 0; i < 3; i++) {
            if (!arrived[i] && clock == arrivalTimes[i]) {
                handleArrival(programFiles[i], i + 1, arrivalTimes[i]);
                arrived[i] = true;
            }
        }

        // 2. now pick
        if (scheduler.getCurrentProcess() == null) {
            scheduler.pickNext();
        }

        // 3. swap in from disk if needed
        Process running = scheduler.getCurrentProcess();
        if (running != null && running.isOnDisk) {
            handleSwapIn(running);
        }

        // 4. execute one instruction
        running = scheduler.getCurrentProcess();
        if (running != null) {
            // capture BEFORE executing so GUI can display it
            lastExecutedInstruction = running.getCurrentLine();
            lastRunningPID          = running.processID;
            if (onBeforeExecute != null) onBeforeExecute.run();

            System.out.println("[CPU] Process " + running.processID +
                               " executing: " + lastExecutedInstruction);

            interpreter.execute(lastExecutedInstruction, running);
            running.incrementPC();
            running.updatePCInMemory(memory);

            if (running.isFinished()) {
                scheduler.handleFinish(running);
            } else {
                scheduler.afterInstruction();
            }
        }

        // 5. tick clock
        scheduler.tick();

        // 6. check done AFTER tick
        boolean allArrived = arrived[0] && arrived[1] && arrived[2];
        if (allArrived && scheduler.allDone()) {
            System.out.println("[OS] All processes finished.");
            return false;
        }

        return true;
    }
}