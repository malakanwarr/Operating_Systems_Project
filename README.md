# CSEN 602 — Operating Systems Simulator
**German University in Cairo · Faculty of Media Engineering and Technology**

A Java-based simulation of a real operating system built for the CSEN 602 Operating Systems course. The simulator models process lifecycle management, memory allocation, CPU scheduling, mutual exclusion, and disk swapping — all visualized through an interactive JavaFX GUI.

---

## Features

- **Process Interpreter** — reads `.txt` program files and executes a custom instruction set including `assign`, `print`, `printFromTo`, `readFile`, `writeFile`, `semWait`, and `semSignal`
- **Main Memory** — 40-word fixed-size memory divided into words, each storing a key-value pair. Holds process instructions, variables, and PCBs
- **Process Control Block (PCB)** — each process stores its ID, state, program counter, and memory boundaries directly in memory
- **CPU Scheduling** — three algorithms implemented:
  - **Round Robin (RR)** — preemptive, configurable time slice (default: 2 instructions)
  - **Highest Response Ratio Next (HRRN)** — non-preemptive, selects process with highest `(waitingTime + burstTime) / burstTime`
  - **Multi-Level Feedback Queue (MLFQ)** — 4 priority queues with quantum formula `2^i`, demotes on quantum expiry, last queue uses RR
- **Mutual Exclusion** — three mutexes for `userInput`, `userOutput`, and `file` resources using `semWait` / `semSignal`
- **Disk Swapping** — when memory is full, processes are swapped out to disk in `key=value` format and swapped back in when scheduled, potentially at a new memory location
- **JavaFX GUI** — step-by-step clock cycle execution with live visualization of all system components

---

## Programs

Three programs are included, modelling real OS workloads:

|
 Program 
|
 Description 
|
|
---
|
---
|
|
`Program_1.txt`
|
 Takes two numbers as input and prints all integers between them 
|
|
`Program_2.txt`
|
 Takes a filename and data as input and writes the data to that file 
|
|
`Program_3.txt`
|
 Takes a filename as input and prints the contents of that file 
|

> Program 3 depends on Program 2 having written the file first. Ensure Program 2 arrives before Program 3 executes its `readFile` instruction.

---

## Architecture

```
OS/
├── Main.java          Entry point
├── OS.java            Core loop — handles arrivals, scheduling, execution per clock cycle
├── Process.java       Process object + PCB management
├── Memory.java        40-word memory array with allocate, read, write, update, free
├── MemoryWord.java    Single memory slot (key + value)
├── Disk.java          Swap in/out to disk/process_N.txt files
├── Scheduler.java     RR, HRRN, and MLFQ scheduling algorithms + queue management
├── Interpreter.java   Parses and executes program instructions
├── SystemCalls.java   Implements assign, print, readFile, writeFile, printFromTo
├── Mutex.java         Semaphore-based mutual exclusion with blocked queue per resource
├── Resource.java      Enum for userInput, userOutput, file
└── OSGUI.java         JavaFX GUI
```

---

## GUI Overview

The GUI provides a full real-time view of the simulator state after every clock cycle:

- **Main Memory panel** — all 40 slots color-coded by process (P1 blue, P2 orange, P3 purple)
- **CPU panel** — currently executing process and instruction
- **Process state badges** — live READY / RUNNING / BLOCKED / FINISHED indicators
- **Ready Queue** — shows all waiting processes with their current PC
- **Blocked Queue** — processes waiting for a mutex resource
- **Disk panel** — processes currently swapped out
- **Process Output** — what the programs actually print to screen
- **System Log** — all OS and scheduler events
- **STEP button** — advance one clock cycle at a time
- **RESET button** — restart the simulation from scratch

---

## How to Run

**Requirements:** Java 17+ and JavaFX SDK

1. Clone the repository
2. Place `Program_1.txt`, `Program_2.txt`, `Program_3.txt` in the **project root** (same level as `src/`)
3. Open in Eclipse (or any Java IDE) and run `OSGUI.java` for the GUI, or `Main.java` for terminal output

**To change the scheduling algorithm**, edit the constructor in `OS.java`:
```java
scheduler = new Scheduler(memory, "RR",   2);   // Round Robin, time slice = 2
scheduler = new Scheduler(memory, "HRRN", 2);   // Highest Response Ratio Next
scheduler = new Scheduler(memory, "MLFQ", 2);   // Multi-Level Feedback Queue
```

**To change arrival times or program order**, edit in `OS.java`:
```java
int[]    arrivalTimes = {0, 1, 4};
String[] programFiles = {"Program_1.txt", "Program_2.txt", "Program_3.txt"};
int[]    processIDs   = {1, 2, 3};
```

---

## Process Instruction Set

|
 Instruction 
|
 Syntax 
|
 Description 
|
|
---
|
---
|
---
|
|
`assign`
|
`assign x 5`
 or 
`assign x input`
|
 Assigns a value to a variable. If 
`input`
, prompts the user 
|
|
`print`
|
`print x`
|
 Prints the value of a variable 
|
|
`printFromTo`
|
`printFromTo x y`
|
 Prints all integers from x to y 
|
|
`readFile`
|
`readFile x`
|
 Reads and returns the contents of file x 
|
|
`writeFile`
|
`writeFile x y`
|
 Writes value y to file named x 
|
|
`semWait`
|
`semWait userInput`
|
 Acquires a mutex. Blocks the process if already held 
|
|
`semSignal`
|
`semSignal userInput`
|
 Releases a mutex and unblocks the next waiting process 
|

---

## Team

Built as part of the CSEN 602 Operating Systems course, Spring 2026.

<img width="1276" height="802" alt="Screenshot 2026-06-30 at 11 01 58 PM" src="https://github.com/user-attachments/assets/4a816db6-d46b-41a4-aa9a-ed88cfbf46d9" />
<img width="1274" height="804" alt="Screenshot 2026-06-30 at 11 02 31 PM" src="https://github.com/user-attachments/assets/3a8fae6d-4f90-4cdc-9c5f-878d673445c3" />
<img width="1275" height="806" alt="Screenshot 2026-06-30 at 11 03 59 PM" src="https://github.com/user-attachments/assets/57b539ef-dcba-4cdc-8ea8-ab7333a0ae9a" />

