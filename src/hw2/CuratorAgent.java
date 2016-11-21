package hw2;

import hw1.Artifact;
import hw2.TourGuideAgent.BuildTourInit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.proto.SimpleAchieveREInitiator;
//import jade.domain.introspection.ACLMessage;
import jade.proto.SimpleAchieveREResponder;
import jade.proto.states.MsgReceiver;

public class CuratorAgent extends Agent {
	
	public HashMap<Long, Artifact> artCollection;
	int numberOfArtifacts = 100;
	
	public void setup(){
		System.out.println("CuratorAgent started! ID: "+this.getName());
		//Register curator to DF
		register();
		HashMap<AID, Integer> maxPrice = new HashMap<AID, Integer>();
		
		CyclicBehaviour cyclicBehaviour = new CyclicBehaviour() {
			
			@Override
			public void action() {
				ACLMessage msg = myAgent.receive();
				if(msg != null){
					System.out.println("Curator: Ontology = "+msg.getOntology());
					switch(msg.getOntology()){

						case "START_AUCTION":
							System.out.println("Curator recieved start of auction");
							informProfilers(msg);
							break;
						case "NEW_BID_AUCTION":
							System.out.println("Curator recieved new bid proposal");
							AuctionItem auctionItem;
							try {
								auctionItem = (AuctionItem) msg.getContentObject();
								for(Map.Entry<AID, Integer> profilers: maxPrice.entrySet()){
									AID aid = profilers.getKey();
									Integer price = profilers.getValue();
									if(auctionItem.getPrice() <= price){
										acceptBid(aid, auctionItem, msg);
									}
								}
							} catch (UnreadableException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						case "END_AUCTION": 
							System.out.println("Curator recieved end of auction");
							break;
						case "ARTIFACT_DELIVERY": 
							System.out.println("Curator recieved artifact in cyclic");
							break;
						case "PROFILER_MAX_PRICE": 
							try {
								System.out.println("Curator received max acceptable price ("+String.valueOf(msg.getContentObject())+") for Profiler: "+ msg.getSender().getName());
								maxPrice.put(msg.getSender(), (Integer) msg.getContentObject());
							} catch (UnreadableException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						default: 
							System.out.println("other");
							break;
					}
				}
			}
		};
		addBehaviour(cyclicBehaviour);
	}	
	
	private void informProfilers(ACLMessage artifactInfoMsg){
		List<AID> profilers = lookUpProfilers();
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setOntology("NEW_ARTIFACT");
		for(AID profiler : profilers){
			msg.addReceiver(profiler);
		}
		try {
			msg.setContentObject((AuctionItem)artifactInfoMsg.getContentObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
		send(msg);
	}
	
	
	private void acceptBid(AID profiler, AuctionItem auctionItem, ACLMessage msg){
		System.out.println("Curator: trying to accept offer");
		SimpleAchieveREInitiator msgInitiator = new SimpleAchieveREInitiator(this, msg){
			protected void handleInform(ACLMessage inform){
				super.handleInform(inform);
				System.out.println("Curator received bid response in REInitiator");
				//generera Art collection (One shot behaviour)
				OneShotBehaviour generateArt = new OneShotBehaviour() {
					
					@Override
					public void action() {
						try {
							sendArtifactToProfiler(profiler, ((Artifact) inform.getContentObject()));
						} catch (UnreadableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
			}

			@Override
			protected void handleFailure(ACLMessage msg) {
				super.handleFailure(msg);
				System.out.println("Recieved refuse on bid from: "+ profiler.getName());
			}

			@Override
			//Should always be running, if it ends, reboot it
			public int onEnd() {
				return super.onEnd();
			}
			
		};
	}
	
	private void sendArtifactToProfiler(AID profiler, Artifact artifact){
		ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
		msg.setOntology("ARTIFACT");
		msg.addReceiver(profiler);
		try {
			msg.setContentObject(artifact);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		send(msg);
	}
		
	
	private void register(){
		DFAgentDescription desc = new DFAgentDescription();
		desc.setName(getAID());
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("Curator");
		serviceDesc.setName(getName());
		desc.addServices(serviceDesc);
		
		try {
			DFService.register(this,  desc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	/*
	 *-------LAB 2------------
	 */
	
	//Look up profilers using DF
		private List<AID> lookUpProfilers(){
			List<AID> profilers = new ArrayList<AID>();
			DFAgentDescription desc = new DFAgentDescription();
			ServiceDescription serviceDesc = new ServiceDescription();
			serviceDesc.setType("Profiler");
			desc.addServices(serviceDesc);
			try {
				DFAgentDescription[] foundCurators = DFService.search(this, desc);
				for(DFAgentDescription c : foundCurators){
					profilers.add(c.getName());
				}
			} catch (FIPAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return profilers;
		}
	
	//Initiate tour build at curator
	class GetArtifactFromAMA extends SimpleAchieveREInitiator{
		ACLMessage original;
		
		public GetArtifactFromAMA(Agent agent, ACLMessage msg, ACLMessage profilerMsg){
			super(agent, msg);
			this.original = profilerMsg;
		}

		@Override
		protected void handleInform(ACLMessage msg) {
			super.handleInform(msg);
			System.out.println("Tour guide receive ack on build from Curator");
			
			ACLMessage profilerReply = original.createReply();
			profilerReply.setPerformative(ACLMessage.INFORM);
			
			try {
				profilerReply.setContentObject(msg.getContentObject());
			} catch (Exception e) {
				e.printStackTrace();
			}
			send(profilerReply);
		}	
	}
	
}
