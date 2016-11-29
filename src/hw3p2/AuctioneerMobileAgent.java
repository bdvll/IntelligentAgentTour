package hw3p2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hw2.Artifact;
import hw2.AuctionItem;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class AuctioneerMobileAgent extends MobileAgent{
	int numOfArtifacts;
	ArrayList<AID> participants = null;
	HashMap<Long, Artifact> artHash = new HashMap<Long, Artifact>();
	AuctionItem auctionItem = null;
	ArrayList<String> acceptedBids = new ArrayList<String>();
	boolean auctionCompleted = false;
	
	@Override
	protected void setup() {
		register();
		System.out.println("Setting up Auctioneer agent");
		super.setup();
	}

	@Override
	void init() {
		super.init();
		myGui.setInfo("Auctioneer Agent Initialized");
	}

	@Override
	protected void beforeMove() {
		System.out.println("AuctioneerAgent preparing to move...");
		super.beforeMove();
	}

	@Override
	protected void afterMove() {
		if(acceptedBids.size() > 0){
			System.out.println("----------------------");
			System.out.println("Moved home with the bids:");
			System.out.println(acceptedBids.toString());
			System.out.println(this.getLocalName()+" is done, deleting itself...");
			try{
				Thread.sleep(5000);
			}catch(Exception e){
				
			}
			this.doDelete();
		}
		super.afterMove();
	}

	@Override
	protected void beforeClone() {
		System.out.println("AuctioneerAgent preparing to clone...");
		super.beforeClone();
	}

	@Override
	protected void afterClone() {
		System.out.println("AuctioneerAgent: "+this.getLocalName()+" cloned successfully, running afterClone");
		System.out.println(this.getLocalName()+" looking up auction participants");
		participants = lookupCurators();
		System.out.println(this.getLocalName()+" found: "+participants.size()+" participants!");
		//----- begin auction ------
		
		TickerBehaviour tickerBehaviour = new TickerBehaviour(this, 10000){
			@Override
			protected void onTick() {
				System.out.println("-tick-");
				System.out.println("Auctioneer has "+artHash.size()+" items to sell");
				if(auctionItem == null) {
					System.out.println(myAgent.getLocalName()+": No current auctions; Starting one...");
					startNewAuction();
				}else if(auctionItem.getPrice() > auctionItem.getLowestPrice()+auctionItem.getPriceReductionAmount()){
						auctionItem.setPrice(auctionItem.getPrice() - auctionItem.getPriceReductionAmount());
						System.out.println(myAgent.getLocalName()+": Active auction: lowering price to: "+auctionItem.getPrice());
						sendNewBid();
				}else{
					System.out.println(myAgent.getLocalName()+": Price too low, ending auction");
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
							System.out.println(myAgent.getLocalName()+": Received ACCEPT_PROPOSAL");
							try {
								AuctionItem acceptedAI = (AuctionItem)msg.getContentObject();
								//Bid is on current round
								if(auctionItem != null){
									if(acceptedAI.getPrice() == auctionItem.getPrice()){
										System.out.println(myAgent.getLocalName()+" putting accepted bids in bid-list");
										acceptedBids.add(String.valueOf(acceptedAI.getPrice()));
										ACLMessage confirm = msg.createReply();
										confirm.setContentObject(acceptedAI.getPrice());
										confirm.setPerformative(ACLMessage.CONFIRM);
										confirm.setOntology("ARTIFACT_DELIVERY");
										send(confirm);
										
										numOfArtifacts --;
										
										if(numOfArtifacts <= 0){
											auctionItem = null;
											System.out.println("---- NO MORE ITEMS-----");
										}
										if(acceptedBids.size() == participants.size()){
											//System.out.println(myAgent.getLocalName()+" Moving back to home.");
											myAgent.doMove(findHome());
										}
									}
								}else{
									System.out.println(myAgent.getLocalName()+": FAILURE: no more artifacts left, too bad Buyer");
									ACLMessage errorMsg = msg.createReply();
									errorMsg.setPerformative(ACLMessage.FAILURE);
									errorMsg.setOntology("ARTIFACT_DELIVERY");
									send(errorMsg);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						break;
						case ACLMessage.REJECT_PROPOSAL: 
							System.out.println(myAgent.getLocalName()+": Received REJECT_PROPOSAL");
						break;
					}
				}
			}
		};
		//Add behaviours
		ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
		System.out.println(this.getLocalName()+" Adding behaviours and starting auction");
		parallelBehaviour.addSubBehaviour(tickerBehaviour);
		parallelBehaviour.addSubBehaviour(cyclicBehaviour);		
		addBehaviour(parallelBehaviour);
		
		//put accepted bids in acceptedBids
		//when done move() back to container
		super.afterClone();
	}
	

	// ---------- FROM ARTIST MANAGER -----------------
	private void endAuction(){
		System.out.println(this.getLocalName()+": Sending end of auction message");
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setOntology("END_AUCTION");
		sendToCurators(msg, auctionItem);
		
		auctionItem = null;
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
		List<AID> curators = lookupCurators();
		for(AID a : curators){
			msg.addReceiver(a);
		}
		send(msg);
	}
	private void startNewAuction(){
		System.out.println("");
		System.out.println(this.getLocalName()+": Sending start of auction message");
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
	private List<Long> genArt(){
		List<Long> artCollection = new ArrayList();
		Artifact artifact = new Artifact();
		artHash.put(artifact.getID(), artifact);
		artCollection.add(artifact.getID());
		//int copiesAmount = (int) (Math.random()*1);
		int copiesAmount = 1;
		
		for(int i=0; i<copiesAmount; i++){
			Artifact copy = artifact.genCopy();
			artHash.put(copy.getID(), copy);
			artCollection.add(copy.getID());
		}
		numOfArtifacts = artHash.size();
		return artCollection;
	}
	private void sendNewBid(){
		System.out.println(this.getLocalName()+": Sending new bid message");
		ACLMessage msg = new ACLMessage(ACLMessage.CFP);
		msg.setOntology("NEW_BID_AUCTION");
		
		sendToCurators(msg, auctionItem);
	}
	
	public ArrayList<AID> lookupCurators(){
		ArrayList<AID> curators = new ArrayList<AID>();
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		String myName = this.getLocalName();
		String[] nameString = myName.split("-");
		String cloneInfo;
		if(nameString.length > 1){
			cloneInfo = "-"+nameString[1];
		}else{
			cloneInfo = "";
		}
		
		serviceDesc.setType("MuseoGalileo"+cloneInfo);
		desc.addServices(serviceDesc);
		
		try {
			while(curators.size() < 2){
				Thread.sleep(500);
				DFAgentDescription[] foundGalileo = DFService.search(this, desc);
					for(int i = 0; i < foundGalileo.length; ++i){
						String agentName = foundGalileo[i].getName().getLocalName();
						System.out.println("found participant "+agentName);
						if(!(agentName.contains("-"))) continue;
						serviceDesc.setType("HeritageMalta"+cloneInfo);
						curators.add(foundGalileo[i].getName());
					}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Lookup curs found: "+curators.toString());
		return curators;
	}
	
	private AID lookupParent(){
		if((this.getLocalName().equals("Auctioneer"))) return null;
		AID parent = null;
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		String myName = this.getLocalName();
		String[] splitName = myName.split("-");
		String parentName = splitName[0];
		//System.out.println("in lookup parent: looking up parent with name: "+parentName);
		serviceDesc.setType(parentName);
		desc.addServices(serviceDesc);
		try {
			while(parent==null){
				Thread.sleep(500);
				//System.out.println("in lookup parent: looking up parent with name: "+parentName);
				//System.out.println(this.getLocalName()+"in while loop");
				DFAgentDescription[] foundParents = DFService.search(this, desc);
				for(int i = 0; i < foundParents.length; ++i){
					parent = foundParents[i].getName();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return parent;
	}
	void sendRequest(Action action) {
		// ---------------------------------
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setLanguage(new SLCodec().getName());
		request.setOntology(MobilityOntology.getInstance().getName());
		try {
			getContentManager().fillContent(request, action);
			request.addReceiver(action.getActor());
			send(request);
		}
		catch (Exception ex) { ex.printStackTrace(); }
   }
	private Location findHome(){
		// Find way back home
		AID parent = lookupParent();
		//System.out.println("in find home: "+this.getLocalName()+" thinks "+parent.getLocalName()+" is his parent.");
		WhereIsAgentAction whereIsAgent = new WhereIsAgentAction();
		whereIsAgent.setAgentIdentifier(parent);
		sendRequest(new Action(getAMS(), whereIsAgent));

	     //Receive response from AMS
        MessageTemplate mt = MessageTemplate.and(
			                  MessageTemplate.MatchSender(getAMS()),
			                  MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage resp = blockingReceive(mt);
        ContentElement ce;
		try {
			ce = getContentManager().extractContent(resp);
			Result result = (Result) ce;
	        jade.util.leap.Iterator it = result.getItems().iterator();
	        while (it.hasNext()) {
	           return (Location)it.next();
			 }
		} catch (CodecException | OntologyException e) {
			e.printStackTrace();
		}
        return null;
	}
	
	private void register(){
		DFAgentDescription desc = new DFAgentDescription();
		desc.setName(getAID());
		ServiceDescription serviceDesc = new ServiceDescription();
		System.out.println(this.getLocalName()+": registering as "+this.getLocalName());
		serviceDesc.setType(this.getLocalName());
		serviceDesc.setName(getName());
		desc.addServices(serviceDesc);
		
		try {
			DFService.register(this,  desc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	

}
