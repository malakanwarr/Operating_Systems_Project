package OS;

public class Memory {
    private MemoryWord[] slots;
    
    public Memory() {
        slots = new MemoryWord[40];
        for (int i = 0; i < 40; i++) {
            slots[i] = new MemoryWord("EMPTY", "");
        }
    }

    
    // returns the start index, or -1 if not enough space
    public int allocate(int slotsNeeded) {
        int consecutive = 0;
        int startIndex  = -1;

        for (int i = 0; i < 40; i++) {
            if (slots[i].key.equals("EMPTY")) {
                if (consecutive == 0) 
                	startIndex = i;
                consecutive++;
                if (consecutive == slotsNeeded)
                	return startIndex;
            } else {
                consecutive = 0;
                startIndex  = -1;
            }
        }
        return -1; 
    }

    public void write(int index, String key, String value) {
        if (index < 0 || index >= 40) {
            System.out.println("ERROR: slot " + index + " out of bounds.");
            return;
        }
        slots[index] = new MemoryWord(key, value);
        
    }

  
    // finds a slot by key and returns its value
    public String read(String key) {
        for (int i = 0; i < 40; i++) {
            if (slots[i].key.equals(key)) {
                return slots[i].value;
            }
        }
        System.out.println("ERROR: key '" + key + "' not found.");
        return null;
    }

    // finds a slot by key and updates its value
    public void update(String key, String newValue) {
        for (int i = 0; i < 40; i++) {
            if (slots[i].key.equals(key)) {
                slots[i].value = newValue;
                return;
            }
        }
        System.out.println("ERROR: key '" + key + "' not found to update.");
    }

 
    // clears all slots belonging to a process, called when swapping a process out
    public void free(int memStart, int memEnd) {
        for (int i = memStart; i <= memEnd; i++) {
            slots[i] = new MemoryWord("EMPTY", "");
        }
    }

    public int countFreeSlots() {
        int count = 0;
        for (int i = 0; i < 40; i++) {
            if (slots[i].key.equals("EMPTY")) count++;
        }
        return count;
    }

    
    // needed by Disk class when saving to file
    public MemoryWord getSlot(int index) {
        return slots[index];
    }

    
    // shows all 40 slots in readable format, use just for now until we do the gui 
    public void printMemory() {
        System.out.println("\n========== MEMORY STATE ==========");
        for (int i = 0; i < 40; i++) {
        	System.out.println("Slot " + i + " | " + slots[i].toString());
        }
        System.out.println("Free slots: " + countFreeSlots());
        System.out.println("==================================\n");
    }
}