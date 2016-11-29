package hw3p2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;

public class ParticipantMobileAgent extends MobileAgent{
	
	int maxPrice;
	int payedPrice = -1;
	
	@Override
	protected void setup() {
		register();
		System.out.println("Setting up Participant agent");
		super.setup();
	}

	@Override
	void init() {
		super.init();
		myGui.setInfo("Participant Agent Initialized");
	}

	@Override
	protected void beforeMove() {
		myGui.setInfo("Preparing for move...");
		super.beforeMove();
	}

	@Override
	protected void afterMove() {
		if(payedPrice > 0){
			System.out.println("-------------------------");
			System.out.println(this.getLocalName()+": Moved home with the bids:");
			System.out.println("payed price: "+payedPrice);
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
		myGui.setInfo("Preparing for clone...");
		super.beforeClone();
	}

	@Override
	protected void afterClone() {
        
		Location home = findHome();
		System.out.println("ParticipantAgent: "+this.getLocalName()+" cloned successfully, running afterClone");
		System.out.println(this.getLocalName()+" registered.");
		register();
		//wait for auction
		CyclicBehaviour cyclicBehaviour = new CyclicBehaviour() {
			@Override
			public void action() {
				ACLMessage msg = myAgent.receive();
				if(msg != null){
					AuctionItem auctionItem = null;
					switch(msg.getOntology()){

						case "START_AUCTION":
							System.out.println("Curator ("+myAgent.getLocalName() + "): recieved start of auction");
							try {
								auctionItem = (AuctionItem) msg.getContentObject();
								maxPrice = (int)(auctionItem.getLowestPrice()*1.5);
								System.out.println(myAgent.getLocalName()+" set its max price to: "+maxPrice);
							} catch (UnreadableException e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							}
							break;
						case "NEW_BID_AUCTION":
							System.out.println("Curator ("+myAgent.getLocalName() + "): recieved new bid proposal");
							try {
								auctionItem = (AuctionItem) msg.getContentObject();
								
								if(auctionItem.getPrice() <= maxPrice){
									System.out.println(myAgent.getLocalName()+" price is below maxprice (my max price: "+maxPrice+", price: "+auctionItem.getPrice()+")");
									ACLMessage accMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
									accMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
									accMsg.setOntology("GET_ARTIFACT");
									accMsg.addReceiver(msg.getSender());
									try {
										accMsg.setContentObject(msg.getContentObject());
									} catch (IOException e) {
										e.printStackTrace();
									};
									//msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
									acceptBid(auctionItem, accMsg);
								}
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
							break;
						case "END_AUCTION": 
							System.out.println("Curator ("+myAgent.getLocalName() + "): recieved end of auction");
							break;
						case "ARTIFACT_DELIVERY": 
							System.out.println("Curator ("+myAgent.getLocalName() + "): recieved artifact in cyclic");
							if(msg.getPerformative() == ACLMessage.CONFIRM){
								try {
									int price = (Integer)msg.getContentObject();
									System.out.println(myAgent.getLocalName()+": received confirmation. Bought art for: "+price);
									payedPrice = price;
									System.out.println(myAgent.getLocalName()+": auction completed, moving home.");
									System.out.println(myAgent.getLocalName()+": thinks its parent is: "+lookupParent().getLocalName());
									System.out.println(myAgent.getLocalName()+": thinks its home is: "+findHome().getName());
									myAgent.doMove(findHome());
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}else{
								System.out.println("Curator ("+myAgent.getLocalName() + "): Could not get artifact");
							}
							break;
						case "PROFILER_MAX_PRICE": 
							try {
								System.out.println("Curator ("+myAgent.getLocalName() + "): received max acceptable price ("+String.valueOf(msg.getContentObject())+") for Profiler: "+ msg.getSender().getName());
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
		//participate in auction
		//move() back to container
		super.afterClone();
	}
	//-------- FROM CURATOR -----------
	
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
	private void acceptBid(AuctionItem auctionItem, ACLMessage msg){
		System.out.println(this.getLocalName()+": trying to accept offer");
		SimpleAchieveREInitiator msgInitiator = new SimpleAchieveREInitiator(this, msg){
			protected void handleInform(ACLMessage inform){
				super.handleInform(inform);
				System.out.println("Curator ("+myAgent.getLocalName() + "): received bid response in REInitiator");
				//generera Art collection (One shot behaviour)
				System.out.println(myAgent.getLocalName()+" Moving back to home.");
				//move back to home container
				//myAgent.doMove(arg0);
				
			}

			@Override
			protected void handleFailure(ACLMessage msg) {
				super.handleFailure(msg);
				System.out.println("Recieved refuse on bid from: ? ");
			}

			@Override
			//Should always be running, if it ends, reboot it
			public int onEnd() {
				System.out.println("Curator ("+myAgent.getLocalName() + "): REInitiator end");
				return super.onEnd();
			}
			
		};
		addBehaviour(msgInitiator);
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
	private AID lookupParent(){
		if((this.getLocalName().equals("MuseoGalileo")) || this.getLocalName().equals("HeritageMalta")) return null;
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
	

}
