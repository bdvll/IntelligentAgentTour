package hw1;

import java.io.IOException;
import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.SubscriptionInitiator;

public class ProfilerAgent extends Agent {
	//create user with 'preferences'
	User user;
	long max_subs = 1;
	
	ArrayList<String> artifactIDs;
	ArrayList<Artifact> artifacts;
	ArrayList<AID> tourGuides;
	
	public void setup(){
		System.out.println("Hey HEY OH! ProfilerAgent started! ID: "+this.getName());
		user = new User();
		System.out.println("User interested in artifacts of type: "+user.getGenreInterest());
		System.out.println("User interested in artifacts created: "+(user.getYearsOfInterest()-50)+" - "+(user.getYearsOfInterest()+50));
		//lookup tour guides
		tourGuides = lookupTourGuides();
		//Subscribe to guides
		subscribeToServices();
		//Get tour
		getTour();
		
	}
	
	public void getTour(){
		//Get tour from TourGuide based on user preferences
		ACLMessage tourRequest = new ACLMessage(ACLMessage.REQUEST);
		tourRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		tourRequest.addReceiver(tourGuides.get(0));
		
		try {
			tourRequest.setContentObject(user);
		} catch (IOException e) {
			e.printStackTrace();
		}
		tourRequest.setOntology("TourRequest");
		TourRequestInit requestInit = new TourRequestInit(this, tourRequest);
		
		//artifact details from curator
		ACLMessage artifactDetailsRequest = new ACLMessage(ACLMessage.REQUEST);
		artifactDetailsRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		artifactDetailsRequest.addReceiver(lookupCurators().get(0));
		artifactDetailsRequest.setOntology("ArtifactDetails");
		ArtifactDetailsInit artifactDetailsInit = new ArtifactDetailsInit(this, artifactDetailsRequest);
		
		SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();
		sequentialBehaviour.addSubBehaviour(requestInit);
		sequentialBehaviour.addSubBehaviour(artifactDetailsInit);
		addBehaviour(sequentialBehaviour);
	}
	
	public void subscribeToServices(){
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("TourGuide");
		SearchConstraints constraints = new SearchConstraints();
		constraints.setMaxResults(max_subs);
		SubscriptionInitiator subscriptionInit = new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), desc, constraints)){

			@Override
			protected void handleInform(ACLMessage inform) {
				super.handleInform(inform);
				DFAgentDescription[] foundAgents;
				try {
					foundAgents = DFService.decodeNotification(inform.getContent());
					if(foundAgents.length == 0) return;
					System.out.println("Profiler received sub message from DF");
					tourGuides = lookupTourGuides();
				} catch (FIPAException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		//add subscription behaviour
		addBehaviour(subscriptionInit);
	}
	
	public ArrayList<AID> lookupTourGuides(){
		ArrayList<AID> tourGuides = new ArrayList<AID>();
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("TourGuide");
		desc.addServices(serviceDesc);
		
		try {
			DFAgentDescription[] foundTourGuides = DFService.search(this, desc);
				for(int i = 0; i < foundTourGuides.length; ++i){
					tourGuides.add(foundTourGuides[i].getName());
				}
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tourGuides;
	}
	
	public ArrayList<AID> lookupCurators(){
		ArrayList<AID> curators = new ArrayList<AID>();
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("Curator");
		desc.addServices(serviceDesc);
		
		try {
			DFAgentDescription[] foundCurators = DFService.search(this, desc);
				for(int i = 0; i < foundCurators.length; ++i){
					curators.add(foundCurators[i].getName());
				}
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return curators;
	}
	
	//Initiator for requesting tour from TourGuideAgent
	class TourRequestInit extends SimpleAchieveREInitiator{
		
		public TourRequestInit(Agent agent, ACLMessage msg){
			super(agent, msg);
		}

		@Override
		protected void handleInform(ACLMessage msg) {
			super.handleInform(msg);
			System.out.println("Profiler received tour response from TourGuide");

			try {
				artifactIDs = (ArrayList<String>) msg.getContentObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	//Initiator for requesting artifact details from Curator
	class ArtifactDetailsInit extends SimpleAchieveREInitiator{
		
		public ArtifactDetailsInit(Agent agent, ACLMessage msg){
			super(agent, msg);
		}

		
		@Override
		protected ACLMessage prepareRequest(ACLMessage msg) {
			try {
				msg.setContentObject(artifactIDs);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return super.prepareRequest(msg);
		}


		@Override
		protected void handleInform(ACLMessage msg) {
			super.handleInform(msg);
			System.out.println("Profiler received artifact details response from Curator");

			try {
				//!!!!!!!!!ARTIFACT PRINT!!!!!!!!!!!
				artifacts = (ArrayList<Artifact>) msg.getContentObject();
				System.out.println("!!! ARTIFACT SIZE: "+artifacts.size()+" !!!");
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("---- Profiler received detailed tour ----");
			for(Artifact artifact : artifacts){
				System.out.println("Artifact - Genre: "+artifact.getGenre()+" Created: "+artifact.getYearOfCreation());
			}
		}
	}

}
