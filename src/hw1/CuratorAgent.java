package hw1;

import java.util.ArrayList;
import java.util.HashMap;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
//import jade.domain.introspection.ACLMessage;
import jade.proto.SimpleAchieveREResponder;

public class CuratorAgent extends Agent {
	
	public HashMap<Long, Artifact> artCollection;
	int numberOfArtifacts = 100;
	
	public void setup(){
		System.out.println("CuratorAgent started! ID: "+this.getName());
		//Register curator to DF
		register();
		
		
		//Add Sequential behaviour:
		SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();
		
		//generera Art collection (One shot behaviour)
		OneShotBehaviour generateArt = new OneShotBehaviour() {
			
			@Override
			public void action() {
				artCollection = new HashMap<Long, Artifact>();
				for(int i = 0; i < numberOfArtifacts; i++ ){
					Artifact art  = new Artifact();
					artCollection.put(art.getID(), art);
				}
				
			}
		};
		//add oneshot behaviour to sequential behaviour
		sequentialBehaviour.addSubBehaviour(generateArt);
		
		//Message from tour guide to build tour
		MessageTemplate buildTourTemplate = MessageTemplate.MatchOntology("BuildTour");
		
		//Message from Profiler for more details on artifacts within tour
		MessageTemplate artifactDetailsTemplate = MessageTemplate.MatchOntology("ArtifactDetails");
		
		//Create a Parallell behaviour to listen for messages from TourGuide and Profiler in parallell
		ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
		parallelBehaviour.addSubBehaviour(new BuildTourResponder(this, buildTourTemplate));
		parallelBehaviour.addSubBehaviour(new ArtifactDetailsResponder(this, artifactDetailsTemplate));
		
		//Add parallell behaviour to sequential behaviour
		sequentialBehaviour.addSubBehaviour(parallelBehaviour);
		addBehaviour(sequentialBehaviour);

		//add sequential behaviour
	}
		
	//Class to handle messages coming from TourGudie requesting the building of a  tour
	class BuildTourResponder extends SimpleAchieveREResponder{
		public BuildTourResponder(Agent agent, MessageTemplate messageTemplate){
			super(agent, messageTemplate);
		}
		//Define how to handle incoming BuildTour message
		@Override
		protected ACLMessage prepareResponse(ACLMessage request){
			
			System.out.println("Curator received BuildTour message from TourGuide");
			ACLMessage reply = request.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			
			User user;
			try {
				user = (User) request.getContentObject();
				ArrayList<Long> artifacts = getTour(user);
				reply.setContentObject(artifacts);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return reply;
		}
	}
	//Class to handle messages coming from Profiler requesting Artifact details
	class ArtifactDetailsResponder extends SimpleAchieveREResponder{
		public ArtifactDetailsResponder(Agent agent, MessageTemplate messageTemplate){
			super(agent, messageTemplate);
		}
		//Define how to handle incoming ArtifactDetailRequests message
		@Override
		protected ACLMessage prepareResponse(ACLMessage request){
			
			System.out.println("Curator received ArtifactDetailsRequest message from Profiler");
			ACLMessage reply = request.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			
			try {
				ArrayList<Long> artifactsIDs = (ArrayList<Long>) request.getContentObject();
				ArrayList<Artifact> artifactsDetailed = getArtifactDetails(artifactsIDs);
				reply.setContentObject(artifactsDetailed);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return reply;
		}
	}
	//Picks out artifacts fitting user preferences returning list of IDs
	private ArrayList<Long> getTour(User user){
		ArrayList<Long> artifactsIDs = new ArrayList<Long>();
		
		for(Artifact a : artCollection.values()){
			if(a.getGenre().equals(user.getGenreInterest()) && a.getYearOfCreation() > user.getYearsOfInterest()-50 && a.getYearOfCreation() < user.getYearsOfInterest()+50){
				artifactsIDs.add(a.getID());
			}
		}
		return artifactsIDs;
	}
	
	//Fetching details on every requested artifact by ID
	private ArrayList<Artifact> getArtifactDetails(ArrayList<Long> artifactIDs){
		ArrayList<Artifact> artifacts = new ArrayList<Artifact>();
		
		for(long id: artifactIDs){
			artifacts.add(artCollection.get(id));
		}
		return artifacts;
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
}
