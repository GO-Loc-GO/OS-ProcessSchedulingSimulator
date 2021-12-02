package utils;

// Run() is called from Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

import java.util.*;
import java.io.*;

public class SchedulingAlgorithm {

  public static Results Run(int runtime, Vector<sProcess> processVector, Results result, int timeSlice) {

    String resultsFile = "Summary-Processes";

    try {
      PrintStream out = new PrintStream(new FileOutputStream(resultsFile));
      roundRobin(runtime, timeSlice, processVector, result, out);
      //FIFO(runtime, processVector, result, out);
      out.close();
    } catch (IOException e) { /* Handle exceptions */
      e.printStackTrace();
    }
    return result;
  }
  
  // The algorithm is implemented with the use of a timer counter which counts up with the passing of each time
  // unit. The algorithm checks at the beginning of each time unit:
  //	(1) first, whether a process has finished;
  //	(2) whether it needs to be blocked for I/O;
  //    (3) whether the time slice allocated for the current process is used up. 
  // If any of these conditions are true, scheduling is done for a new process to enter the CPU.
  // Scheduling is implemented using i mod the total number of processes in a while loop to traverse the ready 
  // queue in a round robin order, where i starts from the index of the current process in the processVector. 
  // The loop breaks when a process that has not been completed is found which becomes the new process 
  // for execution.
  private static void roundRobin(int runtime, int timeSlice, Vector<sProcess> processVector, Results result, PrintStream out){
	  int i = 0;
	  int comptime = 0;
	  int slicedone = 0;
	  int currentProcess = 0;
	  int size = processVector.size();
	  int completed = 0;
	  
	  // Round-robin is a preemptive scheduling algorithm used in interactive systems
	  result.schedulingType = "Interactive (Preemptive)"; 
	  result.schedulingName = "Round-Robin";
	  
	  try {
		  sProcess process = (sProcess) processVector.elementAt(currentProcess);
	      printRegistered(out, currentProcess, process, comptime);
	      while (comptime < runtime) {
	    	// Check completion of the process, schedule the next process when a process is completed
	    	if (process.cpudone == process.cputime) {
	    		completed++;
	    		slicedone = 0;
	    	    printCompleted(out, currentProcess, process, comptime);
	    	    if (completed == size) {
	    	    	result.compuTime = comptime;
	    	        return;
	    	    }
	    	    // scheduling the next process
	    	    // Keep traversing processes in round robin manner
	            // until a unfinished process is found, the traversing starts from the process following the
	    	    // current process
	    	    i = currentProcess + 1;
	    	    while (true) {
	    	    	process = (sProcess) processVector.elementAt(i%size);
	    	        if (process.cpudone < process.cputime) {
	    	        	currentProcess = i%size;
	    	        	break;
	    	        }
	    	        i++;
	    	    }
	    	    process = (sProcess) processVector.elementAt(currentProcess);
	    	    printRegistered(out, currentProcess, process, comptime);
	    	}
	    	
	    	// Checking for blocking time, schedule the next process when blocking time is reached
	        if (process.ioblocking == process.ionext) {
	          printIOBlocked(out, currentProcess, process, comptime);
	          process.numblocked++;
	          //reset ionext of the process and the slice time elapsed
	          process.ionext = 0;
	          slicedone = 0;
	          
	          // scheduling the next process, same as scheduling for completion
	          i = currentProcess + 1;
	    	    while (true) {
	    	    	process = (sProcess) processVector.elementAt(i%size);
	    	        if (process.cpudone < process.cputime) {
	    	        	currentProcess = i%size;
	    	        	break;
	    	        }
	    	        i++;
	    	    }
	    	    process = (sProcess) processVector.elementAt(currentProcess);
	    	    printRegistered(out, currentProcess, process, comptime);
	        }
	    	
	        // Checking for slice time, schedule the next process when a time slice is used up
	    	if(slicedone == timeSlice) {
	    		//reset time slice elapsed
	    		slicedone = 0;
	    		
	    		// scheduling the next process, same as scheduling for previous scenarios
	    		i = currentProcess + 1;
	    	    while (true) {
	    	    	process = (sProcess) processVector.elementAt(i%size);
	    	        if (process.cpudone < process.cputime) {
	    	        	currentProcess = i%size;
	    	        	break;
	    	        }
	    	        i++;
	    	    }
	    	    process = (sProcess) processVector.elementAt(currentProcess);
	    	    printRegistered(out, currentProcess, process, comptime);
	    	}
	    	
	    	// increment timer counters, including timers for cpu time completed and next I/O of each
	    	// process and timers for slice time and total simulation time controlling the whole simulation
	        process.cpudone++;
	        if (process.ioblocking > 0) {
	          process.ionext++;
	        }
	    	slicedone++;
	    	comptime++;
	      }
	      
	      //
	      result.compuTime = comptime;
	      out.println("Not enough time to complete all processes!");
	  }catch(Exception e) {
		  throw(e);
	  }
  }


  //Identifiable design decisions made:
  //(1) in the algorithm, the time duration of I/O blocking of each process is assumed to be 0;
  //(2) in the algorithm, scheduling happens every time completion of a process or blocking of a process happens;
  //(3) scheduling is done by traversing the process queue processVector from the last to the first element, 
  // by checking at each step whether the process that is being traversed has finished, the index of the front most element in 
  // the queue that has not been completed is captured in the end and dispatched for execution;
  //(4) a timer counter is used in the algorithm. It counts up with the passing of each time unit, after the
  // counts up every time, the algorithm checks for I/O blocking or process completion;
  
  //Limitation:
  //(1)The algorithm written here is not suitable for simulating scenarios in which blocking time is very long 
  // (way larger than 0) for each process.
  //(2) it seems that the code "result.compuTime = comptime;" in the catch block should be moved up to the try
  // block after the while loop, such that the total simulation time can be assigned correctly.
  private static void FIFO(int runtime, Vector<sProcess> processVector, Results result, PrintStream out){
    int i = 0;
    int comptime = 0;
    int currentProcess = 0;
    int previousProcess = 0;
    int size = processVector.size();
    int completed = 0;

    result.schedulingType = "Batch (Nonpreemptive)";
    result.schedulingName = "First-Come First-Served";

    try {
      sProcess process = (sProcess) processVector.elementAt(currentProcess);
      printRegistered(out, currentProcess, process, comptime);
      while (comptime < runtime) {

        // Check completion of the process
        if (process.cpudone == process.cputime) {
          completed++;
          printCompleted(out, currentProcess, process, comptime);
          if (completed == size) {
            result.compuTime = comptime;
            return;
          }
          // scheduling the next process
          for (i = size - 1; i >= 0; i--) {
            process = (sProcess) processVector.elementAt(i);
            if (process.cpudone < process.cputime) {
              currentProcess = i;
            }
          }
          process = (sProcess) processVector.elementAt(currentProcess);
          printRegistered(out, currentProcess, process, comptime);

        }
        // Checking for blocking time
        if (process.ioblocking == process.ionext) {
          printIOBlocked(out, currentProcess, process, comptime);
          process.numblocked++;
          process.ionext = 0;
          
          // scheduling the next process
          previousProcess = currentProcess;
          for (i = size - 1; i >= 0; i--) {
            process = (sProcess) processVector.elementAt(i);
            if (process.cpudone < process.cputime && previousProcess != i) {
              currentProcess = i;
            }
          }
          process = (sProcess) processVector.elementAt(currentProcess);
          printRegistered(out, currentProcess, process, comptime);
        }
        // increment timer counters
        process.cpudone++;
        if (process.ioblocking > 0) {
          process.ionext++;
        }
        comptime++;
      }
      out.println("Not enough time to complete all processes!");
    } catch (Exception e) {
      result.compuTime = comptime;
      throw(e);
    }
  }

  private static void printRegistered(PrintStream out, int currentProcess, sProcess process, int comptime){
    out.println("Process: " + currentProcess + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + comptime + ")");
  }

  private static void printCompleted(PrintStream out, int currentProcess, sProcess process, int comptime){
    out.println("Process: " + currentProcess + " completed... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + comptime + ")");
  }

  private static void printIOBlocked(PrintStream out, int currentProcess, sProcess process, int comptime){
    out.println("Process: " + currentProcess + " I/O blocked... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + comptime + ")");
  }
}
