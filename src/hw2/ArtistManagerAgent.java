package hw2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArtistManagerAgent extends Agent{
	AuctionItem auctionItem = null;
	HashMap<Long, Artifact> artHash = new HashMap<Long, Artifact>();
	@Override
	protected void setup() {
		super.setup();
		System.out.println("ArtistManagerAgent started, name: " + getLocalName());
		
		//Create Artifacts
		TickerBehaviour tickerBehaviour = new TickerBehaviour(this, 10000){

			@Override
			protected void onTick() {
				System.out.println("-tick-");
				if(auctionItem == null) {
					System.out.println("ArtistManager: No current auctions; Starting one...");
					startNewAuction();
				}else if(auctionItem.getPrice() > auctionItem.getLowestPrice()+auctionItem.getPriceReductionAmount()){
						auctionItem.setPrice(auctionItem.getPrice() - auctionItem.getPriceReductionAmount());
						System.out.println("ArtistManager: Active auction: lowering price to: "+auctionItem.getPrice());
						sendNewBid();
				}else{
					System.out.println("ArtistManager: Price too low, ending auction");
					System.out.println();
					endAuction();
				}
				
				
				
			}
		};
		
		CyclicBehaviour cyclicBehaviour = new CyclicBehaviour() {
			
			@Override
			public void action() {
				//MessageTemplate template = MessageTemplate.MatchOntology("BID_RESPONSE");
				ACLMessage msg = myAgent.receive();
				if(msg != null){
					switch(msg.getPerformative()){
		
						case ACLMessage.ACCEPT_PROPOSAL: 
							System.out.println("ArtistManager: Received ACCEPT_PROPOSAL");
							try {
								AuctionItem acceptedAI = (AuctionItem)msg.getContentObject();
								//Bid is on current round
								if(auctionItem != null){
									if(acceptedAI.getPrice() == auctionItem.getPrice()){
										deliverArtifact(msg);
									}
								}else{
									System.out.println("ArtistManager: FAILURE: no more artifacts left, too bad Profiler");
									ACLMessage errorMsg = msg.createReply();
									errorMsg.setPerformative(ACLMessage.FAILURE);
									errorMsg.setOntology("ARTIFACT_DELIVERY");
									send(errorMsg);
								}
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
						break;
						case ACLMessage.REJECT_PROPOSAL: 
							System.out.println("ArtistManager: Received REJECT_PROPOSAL");
						break;
					}
				}
			}
		};
		
		//Add behaviours
		ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
		parallelBehaviour.addSubBehaviour(tickerBehaviour);
		parallelBehaviour.addSubBehaviour(cyclicBehaviour);		
		addBehaviour(parallelBehaviour);
	}
	
	private void startNewAuction(){
		System.out.println("");
		System.out.println("ArtistMAnager: Sending start of auction message");
		//Add list of new artifact to hashmap of artifacts
		List<Long> listArt = genArt();
		//artHash.put(listArt.get(0).getName(), listArt);
		
		//Create auction item & auction message
		auctionItem = new AuctionItem(artHash.get(listArt.get(0)).getName(), artHash.get(listArt.get(0)).getValue()*2, artHash.get(listArt.get(0)).getValue(), artHash.get(listArt.get(0)).getGenre());
		auctionItem.setArtifactList(listArt);
		auctionItem.setStatus("active");
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setOntology("START_AUCTION");
		
		sendToCurators(msg, auctionItem);

	}

	private void sendNewBid(){
		System.out.println("ArtistMAnager: Sending new bid message");
		ACLMessage msg = new ACLMessage(ACLMessage.CFP);
		msg.setOntology("NEW_BID_AUCTION");
		
		sendToCurators(msg, auctionItem);
	}
	
	private void endAuction(){
		System.out.println("ArtistMAnager: Sending end of auction message");
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setOntology("END_AUCTION");
		sendToCurators(msg, auctionItem);
		
		auctionItem = null;
	}
	
	private void deliverArtifact(ACLMessage curatorMsg){
		List<Long> artifactList = auctionItem.getArtifactList();
		if(artifactList.size() >= 1){
			Artifact artifact = artHash.remove(artifactList.remove(artifactList.size()-1));
			ACLMessage msg = curatorMsg.createReply();//new ACLMessage(ACLMessage.CONFIRM);
			msg.setOntology("ARTIFACT_DELIVERY");
			try {
				msg.setContentObject(artifact);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//msg.addReceiver(curatorMsg.getSender());
			send(msg);
			if(artifactList.size() == 0){
				System.out.println("ArtistManager: This was the last artifact, ending auction");
				endAuction();
			}
		}	
		else{
			System.out.println("ArtistManager: FAILURE: no more artifacts left, too bad Profiler");
			ACLMessage msg = curatorMsg.createReply();
			msg.setPerformative(ACLMessage.FAILURE);
			msg.setOntology("ARTIFACT_DELIVERY");
			send(msg);
		}
	}
	
	private void sendToCurators(ACLMessage msg, AuctionItem auctionItem){
		//add auction item to auction message
		try {
			msg.setContentObject(auctionItem);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Get curators and add as receivers
		List<AID> curators = lookUpCurators();
		for(AID a : curators){
			msg.addReceiver(a);
		}
		send(msg);
	}
	
	private List<Long> genArt(){
		List<Long> artCollection = new ArrayList();
		Artifact artifact = new Artifact();
		artHash.put(artifact.getID(), artifact);
		artCollection.add(artifact.getID());
		int copiesAmount = (int) (Math.random()*3);
		
		for(int i=0; i<copiesAmount; i++){
			Artifact copy = artifact.genCopy();
			artHash.put(copy.getID(), copy);
			artCollection.add(copy.getID());
		}
		
		return artCollection;
	}

	//Look up curator using DF
	private List<AID> lookUpCurators(){
		List<AID> curators = new ArrayList<AID>();
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("Curator");
		desc.addServices(serviceDesc);
		try {
			DFAgentDescription[] foundCurators = DFService.search(this, desc);
			for(DFAgentDescription c : foundCurators){
				curators.add(c.getName());
			}
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return curators;
	}
	
	//Class to handle messages coming from Curator requesting artifact
		class HandleCuratorDelivery extends SimpleAchieveREResponder{
			public HandleCuratorDelivery(Agent agent, MessageTemplate messageTemplate){
				super(agent, messageTemplate);
			}
			//Define how to handle incoming BuildTour message
			@Override
			protected ACLMessage prepareResponse(ACLMessage request){
				System.out.println("ArtistManager: received REmessage from Curator");
				List<Long> artifactList = auctionItem.getArtifactList();
				ACLMessage reply = null;
				if(artifactList.size() >= 1){
					Artifact artifact = artHash.remove(artifactList.remove(artifactList.size()-1));
					reply = request.createReply();//new ACLMessage(ACLMessage.CONFIRM);
					reply.setPerformative(ACLMessage.INFORM);
					try {
						reply.setContentObject(artifact);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(artifactList.size() == 0){
						System.out.println("ArtistManager: This was the last artifact, ending auction");
						//endAuction();
					}
				}	
	/*		else{
					System.out.println("ArtistManager: FAILURE: no more artifacts left, too bad Profiler");
					ACLMessage msg = new ACLMessage(ACLMessage.FAILURE);
					msg.setOntology("ARTIFACT_DELIVERY");
					send(msg);
				}
	*/
				return reply;
			}
		}

	
}
