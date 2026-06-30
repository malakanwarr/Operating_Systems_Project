package OS;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

public class SystemCalls {
	private Memory memory;
    private Scheduler scheduler;

    public SystemCalls(Memory memory, Scheduler scheduler) {
        this.memory = memory;
        this.scheduler = scheduler;
    }
	private static final Scanner consoleScanner = new Scanner(System.in);
	
	public void print(String x) {
		System.out.println(x);
	}
	
	public void assign(String x, int y) {
		Process currentProcess = scheduler.getCurrentProcess();
        currentProcess.addVariable(x,String.valueOf(y),memory);
	}
	
	public void assign(String x, String y) {
        String valueToAssign = y;
        
        if (y.equals("input")) {
            // We are already on the JavaFX thread from the button click, 
            // so we can show the dialog directly without deadlocking.
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("User Input Required");
            dialog.setHeaderText("Process " + scheduler.getCurrentProcess().processID + " is asking for input.");
            dialog.setContentText("Please enter a value for variable '" + x + "':");
            
            // This natively pauses the execution until the user clicks OK or Cancel
            Optional<String> dialogResult = dialog.showAndWait();
            valueToAssign = dialogResult.orElse("0"); // defaults to 0 if you close the window
        }
        
        try {
            int numericValue = Integer.parseInt(valueToAssign);
            assign(x, numericValue); 
            
        } catch (NumberFormatException e) {
            Process currentProcess = scheduler.getCurrentProcess();
            currentProcess.addVariable(x, valueToAssign, memory);
        }
    }
	
	public void writeFile(String FileName, String Data) {
		//memory.update(FileName, Data);
		String path = "disk/" + FileName; 
        
        try (FileWriter writer = new FileWriter(path, false)) {
            writer.write(Data);
        } catch (IOException e) {
            System.out.println("System Call Error: Could not write to disk.");
        }
		
	}
	
	public String readFile(String FileName) {
		//return memory.read(FileName);
		//from memory or disk or both??
		String path = "disk/" + FileName;
        StringBuilder content = new StringBuilder();
        
        try {
            File file = new File(path);
            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                content.append(fileScanner.nextLine()).append("\n");
            }
            fileScanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("System Call Error: File '" + FileName + "' not found on disk.");
        }
        return content.toString().trim();
	}
	
	public void PrintFromTo(int x, int y) {
		int start = Math.min(x, y);
        int end = Math.max(x, y);
        
        for (int i = start; i <= end; i++) {
            System.out.println(i);
        }
	}
	

}