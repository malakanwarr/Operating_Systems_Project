package OS;

import java.util.LinkedList;

public class Scheduler {

    // MLFQ: 4 priority queues
    LinkedList<Process>[] mlfqQueues;
    LinkedList<Process> readyQueue;   // used for RR and HRRN
    LinkedList<Process> blockedQueue;
    Process runningProcess;

    // clock and scheduling
    int clock;
    int instructionsThisCycle;
    int timeSlice;
    String algorithm;
    Memory memory;

    // current process queue level (for MLFQ)
    int currentQueueLevel;

    // algorithm constants
    public static final String RR   = "RR";
    public static final String HRRN = "HRRN";
    public static final String MLFQ = "MLFQ";

    // number of MLFQ queues
    private static final int NUM_QUEUES = 4;

   
    @SuppressWarnings("unchecked")
    public Scheduler(Memory memory, String algorithm, int timeSlice) {
        this.memory                = memory;
        this.algorithm             = algorithm;
        this.timeSlice             = timeSlice;
        this.readyQueue            = new LinkedList<>();
        this.blockedQueue          = new LinkedList<>();
        this.runningProcess        = null;
        this.clock                 = 0;
        this.instructionsThisCycle = 0;
        this.currentQueueLevel     = 0;

        // initialize 4 MLFQ queues
        mlfqQueues = new LinkedList[NUM_QUEUES];
        for (int i = 0; i < NUM_QUEUES; i++) {
            mlfqQueues[i] = new LinkedList<>();
        }
    }


    // called by OS when a new process arrives
    public void addProcess(Process process) {
        process.state = Process.READY;
        memory.update("P" + process.processID + "_state", Process.READY);

        if (algorithm.equals(MLFQ)) {
            // new processes always start at highest priority queue
            process.queueLevel = 0;
            mlfqQueues[0].add(process);
            System.out.println("[SCHEDULER] Process " + process.processID +
                               " added to MLFQ queue 0.");
        } else {
            readyQueue.add(process);
            System.out.println("[SCHEDULER] Process " + process.processID +
                               " added to ready queue.");
        }
        printQueues();
    }

    
    // picks next process based on algorithm
    public void pickNext() {
        Process next = null;

        if (algorithm.equals(RR)) {
            if (readyQueue.isEmpty()) return;
            next = pickNextRR();
        } else if (algorithm.equals(HRRN)) {
            if (readyQueue.isEmpty()) return;
            next = pickNextHRRN();
        } else if (algorithm.equals(MLFQ)) {
            next = pickNextMLFQ();
        }

        if (next != null) {
            // remove from whichever queue it was in
            if (algorithm.equals(MLFQ)) {
                mlfqQueues[next.queueLevel].remove(next);
                currentQueueLevel = next.queueLevel;
            } else {
                readyQueue.remove(next);
            }

            runningProcess = next;
            runningProcess.state = Process.RUNNING;
            memory.update("P" + next.processID + "_state", Process.RUNNING);
            instructionsThisCycle = 0;

            System.out.println("[SCHEDULER] Process " + next.processID +
                               " selected to run from queue " +
                               (algorithm.equals(MLFQ) ? next.queueLevel : "ready") + ".");
            printQueues();
        }
    }

    // just takes first process from ready queue
    private Process pickNextRR() {
        return readyQueue.peekFirst();
    }

    // calculates response ratio for each, picks highest
    private Process pickNextHRRN() {
        Process best      = null;
        double  bestRatio = -1;

        for (Process p : readyQueue) {
            double ratio = (double)(p.waitingTime + p.burstTime) / p.burstTime;

            System.out.println("[HRRN] Process " + p.processID +
                               " ratio = (" + p.waitingTime + " + " +
                               p.burstTime + ") / " + p.burstTime +
                               " = " + String.format("%.2f", ratio));

            if (ratio > bestRatio) {
                bestRatio = ratio;
                best      = p;
            }
        }
        return best;
    }

    // picks from highest priority non empty MLFQ queue
    private Process pickNextMLFQ() {
        for (int i = 0; i < NUM_QUEUES; i++) {
            if (!mlfqQueues[i].isEmpty()) {
                return mlfqQueues[i].peekFirst();
            }
        }
        return null;
    }

   
    // GET MLFQ QUANTUM FOR A QUEUE LEVEL ,  formula: 2^i
    private int getQuantum(int level) {
        return (int) Math.pow(2, level);
    }

   
   
    // called after every instruction executes
    public void afterInstruction() {
        instructionsThisCycle++;

        // increment waiting time for all ready processes
        if (algorithm.equals(MLFQ)) {
            for (int i = 0; i < NUM_QUEUES; i++) {
                for (Process p : mlfqQueues[i]) {
                    p.waitingTime++;
                }
            }
        } else {
            for (Process p : readyQueue) {
                p.waitingTime++;
            }
        }

        if (algorithm.equals(RR)) {
            // check if RR time slice expired
            if (instructionsThisCycle >= timeSlice) {
                if (runningProcess != null) {
                    runningProcess.state = Process.READY;
                    memory.update("P" + runningProcess.processID + "_state", Process.READY);
                    readyQueue.addLast(runningProcess);

                    System.out.println("[SCHEDULER] Process " + runningProcess.processID +
                                       " time slice expired → back of ready queue.");
                    runningProcess        = null;
                    instructionsThisCycle = 0;
                    printQueues();
                }
            }

        } else if (algorithm.equals(MLFQ)) {
            // check if MLFQ quantum expired
            int quantum = getQuantum(currentQueueLevel);

            if (instructionsThisCycle >= quantum) {
                if (runningProcess != null) {
                    // last queue uses RR → stays in same queue
                    if (currentQueueLevel == NUM_QUEUES - 1) {
                        runningProcess.state = Process.READY;
                        memory.update("P" + runningProcess.processID + "_state", Process.READY);
                        mlfqQueues[currentQueueLevel].addLast(runningProcess);

                        System.out.println("[SCHEDULER] Process " + runningProcess.processID +
                                           " quantum expired in last queue → stays in queue " +
                                           currentQueueLevel);

                    } else {
                        // demote to next lower priority queue
                        int nextLevel = currentQueueLevel + 1;
                        runningProcess.queueLevel = nextLevel;
                        runningProcess.state = Process.READY;
                        memory.update("P" + runningProcess.processID + "_state", Process.READY);
                        mlfqQueues[nextLevel].addLast(runningProcess);

                        System.out.println("[SCHEDULER] Process " + runningProcess.processID +
                                           " quantum expired → demoted to queue " + nextLevel);
                    }

                    runningProcess        = null;
                    instructionsThisCycle = 0;
                    printQueues();
                }
            }
        }
    }

    // called when a process completes all instructions
    public void handleFinish(Process process) {
        process.state  = Process.FINISHED;
        runningProcess = null;
        instructionsThisCycle = 0;
        memory.free(process.memStart, process.memEnd);

        System.out.println("[SCHEDULER] Process " + process.processID +
                           " FINISHED and removed from memory.");
        printQueues();
    }

    // called by mutex when semWait fails
    public void handleBlock(Process process) {
        process.state  = Process.BLOCKED;
        runningProcess = null;
        instructionsThisCycle = 0;
        blockedQueue.add(process);
        memory.update("P" + process.processID + "_state", Process.BLOCKED);

        System.out.println("[SCHEDULER] Process " + process.processID +
                           " BLOCKED → added to blocked queue.");
        printQueues();
    }

    // called by mutex when semSignal frees a process
    public void handleUnblock(Process process) {
        process.state = Process.READY;
        blockedQueue.remove(process);

        if (algorithm.equals(MLFQ)) {
            // return to same queue it was in before blocking
            mlfqQueues[process.queueLevel].addLast(process);
            System.out.println("[SCHEDULER] Process " + process.processID +
                               " UNBLOCKED → returned to queue " + process.queueLevel);
        } else {
            readyQueue.addLast(process);
            System.out.println("[SCHEDULER] Process " + process.processID +
                               " UNBLOCKED → moved to ready queue.");
        }

        if (!process.isOnDisk) {
            memory.update("P" + process.processID + "_state", Process.READY);
        }
        printQueues();
    }

    // finds a process to kick out of memory
    public Process pickProcessToSwap() {
        if (algorithm.equals(MLFQ)) {
            for (int i = NUM_QUEUES - 1; i >= 0; i--) {
                for (Process p : mlfqQueues[i]) {
                    if (!p.isOnDisk) return p;  
                }
            }
        } else {
            for (Process p : readyQueue) {
                if (!p.isOnDisk) return p;      
            }
        }
        for (Process p : blockedQueue) {
            if (!p.isOnDisk) return p;         
        }
        return null;
    }
    // checks if every process is finished
    public boolean allDone() {
        if (blockedQueue.isEmpty() && runningProcess == null) {
            if (algorithm.equals(MLFQ)) {
                for (int i = 0; i < NUM_QUEUES; i++) {
                    if (!mlfqQueues[i].isEmpty()) return false;
                }
                return true;
            } else {
                return readyQueue.isEmpty();
            }
        }
        return false;
    }

    // called after every scheduling event
 // called after every scheduling event
    public void printQueues() {
        System.out.println("------------------------------------");
        System.out.println("  Clock: " + clock);

        String running = (runningProcess == null) ? "none" : "P" + runningProcess.processID;
        System.out.println("  Running : " + running);

        if (algorithm.equals(MLFQ)) {
            for (int i = 0; i < NUM_QUEUES; i++) {
                String q = "";
                for (Process p : mlfqQueues[i]) q += "P" + p.processID + " ";
                System.out.println("  Queue " + i + " (quantum=" + getQuantum(i) + "): [" + q.trim() + "]");
            }
        } else {
            String ready = "";
            for (Process p : readyQueue) ready += "P" + p.processID + " ";
            System.out.println("  Ready   : [" + ready.trim() + "]");
        }

        String blocked = "";
        for (Process p : blockedQueue) blocked += "P" + p.processID + " ";
        System.out.println("  Blocked : [" + blocked.trim() + "]");
        System.out.println("------------------------------------");
    }

    public int     getClock()             { return clock; }
    public Process getCurrentProcess()    { return runningProcess; }
    public void    tick()                 { clock++; }
    public void    setAlgorithm(String a) { algorithm = a; }
    public void    setTimeSlice(int t)    { timeSlice = t; }
}