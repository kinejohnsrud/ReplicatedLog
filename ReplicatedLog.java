package homework1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ReplicatedLog {
	
	int replicaID = 0;
	ArrayList<Transmission> buffer = new ArrayList<Transmission>();
	int transmissionID = 0;
	Replica replica1 = new Replica();
	Replica replica2 = new Replica();
	Replica replica3 = new Replica();
	Replica[] replicas = {replica1, replica2, replica3};

	public static void main(String[] args) {
		ReplicatedLog run = new ReplicatedLog();

		while(true){		//here is the part that takes input from the console, parses it, and sends it to the right method
			System.out.println("Enter a command: ");
			Scanner input = new Scanner(System.in);
			String operation = input.nextLine();
			
			String[] inputElements = operation.toString().split("[\\(\\),]");	//String-array of the 2 or 3 elements of the input
			int input1 = 0;
			
			if(inputElements.length>=2){	//the methods with only one input
				operation = inputElements[0].toLowerCase();
				input1 = Integer.parseInt(inputElements[1].replaceAll(" ", "").replaceAll("\"", ""));
				if(operation.equals("printstate")){
					run.printState(input1);
				}
				else if(operation.equals("receivelog")){
					run.receiveLog(input1);
				}
			}
			else{
				System.out.println("Error");
			}
			
			if(inputElements.length==3){	//the method with two inputs
				String input2 = inputElements[2].replaceAll(" ", "").replaceAll("\"", "");
				if(operation.equals("increment")){
					run.increment(input1, input2, true);
				}
				else if(operation.equals("decrement")){
					run.decrement(input1, input2, true);
				}
				else if(operation.equals("getvalue")){
					run.getValue(input1, input2);
				}
				else if(operation.equals("sendlog")){
					run.sendLog(input1, Integer.parseInt(input2));
				}
			}
		}
	}
	
	public void increment(int repID, String key, boolean realInput){
		if(!replicas[repID-1].hashmap.containsKey(key)){		//makes a hashmap containing the keys and the corresponding value
			replicas[repID-1].hashmap.put(key, 1);				//creates the key if it does not exist, and sets it to 1
		}
		else{													
			replicas[repID-1].hashmap.put(key, replicas[repID-1].hashmap.get(key) + 1); //adds 1 to existing keys
		}
		if(realInput){	//needed in case it is just a receiving replica updating it's own log. Then clock should not be incremented. See receiveLog-method.
			replicas[repID-1].timeTable[repID-1][repID-1] += 1;
			int time = replicas[repID-1].timeTable[repID-1][repID-1];
			Event event = new Event(time,  repID, "Increment(" + key + ")");
			replicas[repID-1].log.add(event);
		}
	}
	
	public void decrement(int repID, String key, boolean realInput){ //equal to increment-method, but with -1 instead.
		if(!replicas[repID-1].hashmap.containsKey(key)){
			replicas[repID-1].hashmap.put(key, -1);
		}
		else{
			replicas[repID-1].hashmap.put(key, replicas[repID-1].hashmap.get(key) - 1);
		}
		if(realInput){
			replicas[repID-1].timeTable[repID-1][repID-1] += 1;
			int time = replicas[repID-1].timeTable[repID-1][repID-1];
			Event event = new Event(time,  repID, "Decrement(" + key + ")");
			replicas[repID-1].log.add(event);	
		}
	}
	
	public void getValue(int replicaID, String key){			//prints the current value of a given key of a given replica
		if(replicas[replicaID-1].hashmap.containsKey(key)){
			System.out.println(replicas[replicaID-1].hashmap.get(key));
		}
		else{
			System.out.println("null");
		}
	}
	
	public void printState(int replicaID){			//printing Log and Timetable of given replica
		System.out.print("Log: {");
		for (int i = 0; i < replicas[replicaID-1].log.size(); i++) {
			System.out.print(replicas[replicaID-1].log.get(i).operation);
			if (i!=replicas[replicaID-1].log.size()-1) System.out.print(", ");
		}
		System.out.println("}");
		System.out.println("Timetable: ");
		System.out.println(replicas[replicaID-1].timeTable[0][0] + " " + replicas[replicaID-1].timeTable[0][1] +" "+ replicas[replicaID-1].timeTable[0][2]);
		System.out.println(replicas[replicaID-1].timeTable[1][0] + " " + replicas[replicaID-1].timeTable[1][1] +" "+ replicas[replicaID-1].timeTable[1][2]);
		System.out.println(replicas[replicaID-1].timeTable[2][0] + " " + replicas[replicaID-1].timeTable[2][1] +" "+ replicas[replicaID-1].timeTable[2][2]);
	}
	
	public int sendLog(int sourceReplicaID, int destReplicaID){
		Replica replica = replicas[sourceReplicaID-1];
		transmissionID += 1;
		//make subset of log to transmit
		ArrayList<Event> log = new ArrayList<Event>();
		for(Event event : replica.log){
			if(!hasrec(replica.timeTable, event, destReplicaID)){	//uses hasrec-method as lectured
				log.add(event);
			}
		}
		int[][] timeTableCopy = deepCopy(replica.timeTable);		//made a deep-copy for arrays to prevent shallow copy where they only get different pointers to same object
		
		Transmission trans = new Transmission(sourceReplicaID, destReplicaID, timeTableCopy, log, transmissionID); //makes a transmission-object to add to buffer/network
		buffer.add(trans);
		System.out.println("Transmission number: " + transmissionID);
		return transmissionID;
	}
	
	public void receiveLog(int transmissionNumber){
		Transmission trans = null;
		//fetches the correct transmission from the buffer based on transmissionNumber
		for (Transmission transmission : buffer) {
			if(transmission.transmissionID == transmissionNumber){
				trans = transmission;
			}	
		}
		Replica replica = replicas[trans.destID-1];		//the receiving replica
		//Checks hasrec, adds unrecorded events to local log, and executes them in executeEvent
		for (Event event : trans.log){
			if(!hasrec(replica.timeTable, event, trans.destID)){
				replica.log.add(event);
				executeEvent(trans.destID, event);
			}
		}
		//updates the local timetable.
		for (int i = 0; i < trans.timeTable.length; i++) {
			replica.timeTable[trans.destID-1][i] = Math.max(replica.timeTable[trans.destID-1][i], trans.timeTable[trans.srcID-1][i]);
			for (int j = 0; j < trans.timeTable[0].length; j++) {
				replica.timeTable[i][j] = Math.max(replica.timeTable[i][j], trans.timeTable[i][j]);
			}
		}
		//Garbage Collection
		ArrayList<Event> garbageEvents = new ArrayList<Event>();
		for (Event event : replica.log){
			int counter = 0;
			for (int i = 1; i <= 3; i++) {	//uses 3 statically since we have three replicas in this assignment
				if(hasrec(replica.timeTable, event, i)){
					counter++;					
				}
			}
			if (counter >= 3){
				garbageEvents.add(event);
			}
		}
		for (Event event : garbageEvents){
			System.out.println("Garbage: " + event.operation);
			replica.log.remove(event);
		}
	}
	
	public void executeEvent(int replicaID, Event event){		//this method is for executing the increment and decrement events at a receiving replica, 
		String[] operationString = event.operation.split("\\("); //and entering "realInput"= false to avoid updating your local clock for these events.
		String operation = operationString[0];
		String key = operationString[1].substring(0, operationString[1].length()-1);
		if(operation.equals("Increment")){
			increment(replicaID, key, false);
		}
		else if(operation.equals("Decrement")){
			decrement(replicaID, key, false);
		}
	}
	
	public boolean hasrec(int[][] timeTable, Event event, int destReplicaID){	//the hasrec-method as lectured
		if(timeTable[destReplicaID-1][event.replicaID-1] >= event.time){
			return true;
		}
		return false;
	}

	//A method made to deep copy the two dimensional time table.
	public static int[][] deepCopy(int[][] original) {
	    if (original == null) {
	        return null;
	    }
	
	    final int[][] result = new int[original.length][];
	    for (int i = 0; i < original.length; i++) {
	        result[i] = Arrays.copyOf(original[i], original[i].length);
	    }
	    return result;
	}
}
