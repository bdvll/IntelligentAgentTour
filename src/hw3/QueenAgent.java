package hw3;

import java.awt.MultipleGradientPaint.CycleMethod;
import java.io.IOException;
import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class QueenAgent extends Agent {
	int boardSize = 0;
	int n;
	//ordningen i queen-följden ( 0 = först )
	AID successor = null;
	AID predecessor = null;
	//list containing occupied positions. Index = x, valueAtIndex = y
	SmarterArrayList<Integer> occupiedPositions = new SmarterArrayList<Integer>();
	
	
	public void setup(){
		Object[] args = getArguments();
        this.n = Integer.parseInt(args[0].toString()); // this returns the String "1"
        this.boardSize = Integer.parseInt(args[1].toString()); // this returns the String "arg2"
		System.out.println("Queen"+n+" started, on board with size:"+boardSize+"x"+boardSize);
		//regga som queen (ordning?)
		register();
		successor = lookupQueen(n+1);
		SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();
		OneShotBehaviour oneShot = sendToQueen(successor, true, true);
		sequentialBehaviour.addSubBehaviour(oneShot);
		//om först
		if(n == 0){
			//välj en koordinat
			int y = chooseCoordinate();
			if(y != -1) occupiedPositions.add(n, y);
			OneShotBehaviour firstAction = sendToQueen(successor, true, false);
			sequentialBehaviour.addSubBehaviour(firstAction);
			//skicka till nästa queen (one-shot)
			//lyssna på meddelanden tillbaka (cyclic)
		}
		
		CyclicBehaviour cyclicBehaviour = listenForReplies();
		sequentialBehaviour.addSubBehaviour(cyclicBehaviour);
		//försök välja position
		addBehaviour(sequentialBehaviour);
			
		
	
	}
	private int chooseCoordinate(){
		int currentPos = -1;
		int pos = 0;
		if(occupiedPositions.get(n) != null){
			currentPos = occupiedPositions.get(n);
			pos = currentPos+1;
		}
		for(int i = 0; i < occupiedPositions.size(); i++){
			System.out.println("Queen"+n+": pos: "+pos+"| occupiedPos: "+occupiedPositions.get(i)+"| i: "+i);
			if(pos == occupiedPositions.get(i)){
				pos +=1;
				i = -1;
			}else if(((n-i)*(n-i)) == ((pos-occupiedPositions.get(i))*(pos - occupiedPositions.get(i)))){
				pos +=1;
				i = -1;
			}
			else if (pos >= boardSize){
				return -1;
			}
		}
		System.out.println("Queen"+n+" chose position: "+pos);
		return pos;	
	}
	private OneShotBehaviour sendToQueen(AID queen, boolean isSuccessor, boolean firstAnnouncement){
		OneShotBehaviour oneShotBehaviour = new OneShotBehaviour() {
			
			@Override
			public void action() {
				System.out.println(myAgent.getLocalName()+" trying to send: "+occupiedPositions.toString());
				if(queen == null) return;
				
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setOntology("Queen"+n);
				msg.addReceiver(queen);
				
				try {
					if(isSuccessor && !(firstAnnouncement)){
						msg.setOntology("NEXT STEP");
						System.out.println("Queen"+n+": Sending position message to Queen "+queen.getLocalName());				
					}else if(!(isSuccessor) && !(firstAnnouncement)){
						msg.setOntology("FAILURE");
						System.out.println("size: "+occupiedPositions.size()+" n: "+n);
						if(occupiedPositions.size() == n+1)	occupiedPositions.remove(n);
						System.out.println("Queen"+n+": Sending failure message to predecessor: "+queen.getLocalName());
					}else if(firstAnnouncement){
						msg.setOntology("ANNOUNCEMENT");
						System.out.println(myAgent.getLocalName()+" sent announcement to "+queen.getLocalName());
					}
					msg.setContentObject(occupiedPositions);
				} catch (IOException e) {
					e.printStackTrace();
				}
				send(msg);
			}
		};
		return oneShotBehaviour;	
	}
	
	private CyclicBehaviour listenForReplies(){
		CyclicBehaviour cyclicBehaviour = new CyclicBehaviour() {
			@Override
			public void action() {
				ACLMessage msg = myAgent.receive();
				if(msg != null){
					System.out.println("Queen"+n+" received message from: "+msg.getSender().getLocalName());
					try {
						System.out.println(myAgent.getLocalName()+": Received Msg: "+msg.getContentObject().toString());
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//Announcement from predecessor
					if(msg.getOntology().equals("ANNOUNCEMENT")){
						predecessor = msg.getSender();
					//Find position
					}else{
						if(n == 0){
							if(occupiedPositions.get(n) == boardSize-1){
								System.out.println("No solutions! :(");
								return;
							}
						}
						try {
							occupiedPositions = (SmarterArrayList<Integer>) msg.getContentObject();
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
						int y = chooseCoordinate();
						OneShotBehaviour oneShot = null;
						if(y != -1){
							if(occupiedPositions.size() == n-1){
								occupiedPositions.set(n, y);
							}else{
								occupiedPositions.add(n, y);
							}
							
							if(successor == null){
								System.out.println("Solution found! : "+occupiedPositions.toString());
							}else{
								oneShot = sendToQueen(successor, true, false);
							}
						}else{
							oneShot = sendToQueen(predecessor, false, false);
						}
						if(oneShot != null)addBehaviour(oneShot);
					}
				}
			}
		};
		return cyclicBehaviour;
	}
	public AID lookupQueen(int searchQueenN){
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		String queryString = "Queen"+(searchQueenN);
		serviceDesc.setType(queryString);
		desc.addServices(serviceDesc);
		try {
			AID foundQueen = DFService.search(this, desc)[0].getName();
			return foundQueen;
		} catch (Exception e) {
			return null;
		}
	}
	private void register(){
		DFAgentDescription desc = new DFAgentDescription();
		desc.setName(getAID());
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("Queen"+n);
		serviceDesc.setName(getName());
		desc.addServices(serviceDesc);
		
		try {
			DFService.register(this,  desc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}


}