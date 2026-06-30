package OS;

import java.io.*;

public class Disk {

    // all swap files will be saved in a folder called "disk" , so process 1 becomes in disk/process_1.txt
    private static final String DISK_FOLDER = "disk/";

    // creates the disk folder if it doesn't exist
    public Disk() {
        File folder = new File(DISK_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
            System.out.println("[DISK] Disk folder created.");
        }
    }


    // saves all of a process's memory slots to a .txt file , then frees those slots in memory
    public void swapToDisk(Process process, Memory memory) {
    	
        String filename = DISK_FOLDER + "process_" + process.processID + ".txt";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            for (int i = process.memStart; i <= process.memEnd; i++) {
                MemoryWord slot = memory.getSlot(i);
                // format is: key=value
                writer.write(slot.key + "=" + slot.value);
                writer.newLine();
            }

            writer.close();
            memory.free(process.memStart, process.memEnd);
            process.isOnDisk = true;
         

            System.out.println("[DISK] Process " + process.processID +
                               " swapped OUT and saved to " + filename);

        } catch (IOException e) {
            System.out.println("ERROR: Could not swap process " +
                               process.processID + " to disk.");
        }
    }

    // reads the .txt file back into memory slots, updates the process's memory boundaries ,deletes the file after loading
    public void swapFromDisk(Process process, Memory memory) {
        String filename = DISK_FOLDER + "process_" + process.processID + ".txt";

        try {
            // first count how many lines are in the file
            // so we know how many slots to allocate
            BufferedReader counter = new BufferedReader(new FileReader(filename));
            int numSlots = 0;
            while (counter.readLine() != null) numSlots++;
            counter.close();

            // find space in memory for this process
            int newMemStart = memory.allocate(numSlots);
            if (newMemStart == -1) {
                System.out.println("[DISK] Not enough memory to swap in Process " +
                                   process.processID);
                return;
            }

            int newMemEnd = newMemStart + numSlots - 1;

            // read the file and write each line back into memory
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            int slotIndex = newMemStart;

            while ((line = reader.readLine()) != null) {
                // split by first = sign only
                int separatorIndex = line.indexOf("=");
                String key   = line.substring(0, separatorIndex);
                String value = line.substring(separatorIndex + 1);

                // --- FIX INJECTED HERE ---
                // Override the stale disk data with the live data from the Process object
                if (key.equals("P" + process.processID + "_state")) {
                    value = process.state; 
                } else if (key.equals("P" + process.processID + "_pc")) {
                    value = String.valueOf(process.programCounter); 
                }
                // -------------------------

                memory.write(slotIndex, key, value);
                slotIndex++;
            }

            reader.close();

            // update process memory boundaries
            process.memStart = newMemStart;
            process.memEnd   = newMemEnd;
            process.isOnDisk = false;

            // sync new boundaries to memory
            memory.update("P" + process.processID + "_memStart",
                          String.valueOf(newMemStart));
            memory.update("P" + process.processID + "_memEnd",
                          String.valueOf(newMemEnd));

            // delete the file now that its back in memory
            new File(filename).delete();

            System.out.println("[DISK] Process " + process.processID +
                               " swapped IN and loaded at slots " +
                               newMemStart + "-" + newMemEnd);

        } catch (IOException e) {
            System.out.println("ERROR: Could not swap process " +
                               process.processID + " from disk.");
        }
    }

    // checks if a swap file exists for this process
    public boolean isOnDisk(int processID) {
        File file = new File(DISK_FOLDER + "process_" + processID + ".txt");
        return file.exists();
    }
}
	

	
	

	


