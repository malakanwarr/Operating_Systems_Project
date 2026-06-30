package OS;

import java.util.Queue;
 
public class Mutex {
	boolean locked;
    Process heldBy;
    Queue<Process> blockedQueue;
    Resource resource;

    public Mutex(Resource resource) {
        this.resource = resource;
        this.locked = false;
        this.heldBy = null;
        this.blockedQueue = new java.util.LinkedList<>();
    }

	
    void semWait(Process p, Scheduler scheduler, Memory memory) {
        if (!locked) {
            locked = true;
            heldBy = p;
        } else {
            blockedQueue.add(p);
            scheduler.handleBlock(p);  
        }
    }

    void semSignal(Scheduler scheduler, Memory memory) {
        if (blockedQueue.isEmpty()) {
            locked = false;
            heldBy = null;
        } else {
            Process next = blockedQueue.poll();
            scheduler.handleUnblock(next); 
            heldBy = next;
        }
    }
	

	

}