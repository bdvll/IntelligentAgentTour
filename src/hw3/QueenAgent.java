package hw3;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class QueenAgent extends Agent {
	//ordningen i queen-följden ( 0 = först )
	int myOrder = 0;
	Coordinate currentPosition = null;
	ArrayList<Coordinate> bannedPositions = new ArrayList<Coordinate>();
	
	public QueenAgent(){
		
	}
	
	public void setup(){
		
		//regga som queen (ordning?)
		//om först
			//välj en koordinat
			Coordinate pos = chooseCoordinate();
			bannedPositions.add(pos);
			//skicka till nästa queen (one-shot)
			sendToSuccessor(bannedPositions);
			//lyssna på meddelanden tillbaka (cyclic)
			listenForReplies();
		//om inte först
			//lyssna efter meddelanden (cyclic)
			listenForReplies();
				//försök välja position
				chooseCoordinate();
				//om lyckas
					//skicka till nästa queen
					sendToSuccessor(bannedPositions);
				//om misslyckas
					//skicka tillbaka till förra queen
					sendToPredecessor();
	
	}
	private Coordinate chooseCoordinate(){
		return null;	
	}
	private void sendToSuccessor(ArrayList<Coordinate> coords){
		
	}
	private void sendToPredecessor(){
		
	}
	private void listenForReplies(){
		
	}
	public AID lookupSuccessor(){
		AID successor = null;;
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		String queryString = "Queen"+(myOrder+1);
		serviceDesc.setType("queryString");
		desc.addServices(serviceDesc);
		try {
			AID foundQueen = DFService.search(this, desc)[0].getName();
			return foundQueen;
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return null;
	}


}