package homework1;
import java.util.ArrayList;


public class Transmission {
	int srcID;
	int destID;
	int transmissionID;
	int[][] timeTable;
	ArrayList<Event> log;
	
	//A transmission is the object sent to the buffer and received from the buffer. The buffer is an ArrayList with Transmission

	public Transmission(int srcID, int destID, int[][] timeTable, ArrayList<Event> log, int transmissionID) {
		this.srcID = srcID;
		this.destID = destID;
		this.timeTable = timeTable;
		this.log = log;
		this.transmissionID = transmissionID;
	}
}
