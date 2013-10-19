package homework1;


public class Event {
	int time;
	int replicaID;
	String operation;
	
	public Event(int time, int replicaID, String operation){
		this.time = time;
		this.replicaID = replicaID;
		this.operation = operation;
	}
}
