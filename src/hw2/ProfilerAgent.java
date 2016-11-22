package hw2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.SubscriptionInitiator;

public class ProfilerAgent extends Agent {
	//create user with 'preferences'
	User user;
	long max_subs = 1;
	
	ArrayList<String> artifactIDs;
	//ArrayList<Artifact> artifacts;
	//Hash <Name(int), Artifact>
	HashMap<Integer, Artifact> artifacts;
	
	public void setup(){
		user = new User();
		artifacts = new HashMap<Integer, Artifact>();
		System.out.println("ProfilerAgent started! ID: "+this.getName());
		System.out.println("User interested in artifacts of type: "+user.getGenreInterest());
		System.out.println("User interested in artifacts created: "+(user.getYearsOfInterest()-50)+" - "+(user.getYearsOfInterest()+50));
		
		register();
		
		CyclicBehaviour cyclicBehaviour= new CyclicBehaviour() {
			
			@Override
			public void action() {
				ACLMessage msg = myAgent.receive();
				if(msg != null){
					switch(msg.getOntology()){
						case "NEW_ARTIFACT": 
							try {
								AuctionItem auctionItem = (AuctionItem) msg.getContentObject();
								System.out.println("Profiler ("+myAgent.getLocalName() + "): recieved information on new auction");
								int maxPrice = 0;
								if(artifacts.get(auctionItem.getItemName()) == null){
									maxPrice = (int) (Math.random()*100000+80000);
									System.out.println("Profiler ("+myAgent.getLocalName()+"): new "+auctionItem.getGenre()+", 'I CAN PAY "+String.valueOf(maxPrice)+"'");
								}else{
									System.out.println("Profiler ("+myAgent.getLocalName()+"): Already got this "+auctionItem.getGenre());
								}
								
								ACLMessage respondMsg = new ACLMessage(ACLMessage.INFORM);
								respondMsg.addReceiver(msg.getSender());
								respondMsg.setContentObject(maxPrice);
								respondMsg.setOntology("PROFILER_MAX_PRICE");
								send(respondMsg);
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							break;
							
						case "ARTIFACT" : 
							try {
								Artifact artifact = (Artifact) msg.getContentObject();
								System.out.println("Profiler ("+myAgent.getLocalName() + "): got new "+artifact.getGenre()+" called: " + artifact.getName());
								artifacts.put(artifact.getName(), artifact);
							} catch (UnreadableException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							break;
					}
				}
			}
		};
		addBehaviour(cyclicBehaviour);
	}
	
	private void register(){
		DFAgentDescription desc = new DFAgentDescription();
		desc.setName(getAID());
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("Profiler");
		serviceDesc.setName(getName());
		desc.addServices(serviceDesc);
		
		try {
			DFService.register(this,  desc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
}
