package homework1;


public class Event {
	int time;
	int replicaID;
	String operation;
	
	//The logs of the replicas is an ArrayList containing Event-objects
	
	public Event(int time, int replicaID, String operation){
		this.time = time;
		this.replicaID = replicaID;
		this.operation = operation;
	}
}
