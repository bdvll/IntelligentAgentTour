package hw2;

import java.io.IOException;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.states.MsgReceiver;
import jade.util.leap.ArrayList;

public class TourGuideAgent extends Agent {
	
	public void setup(){
		System.out.println("TourGuideAgent started! ID: "+this.getName());
		register();
		tourRequestHandler();
		
		//Lyssna på meddelanden (message receiver) från ProfilerAgent
		//	slå upp curators i DF
		//	skicka message till curators (SimpleAchieveREinitiator)
		
	}
	
	
	//Handle tour guide request from Profiler
	private void tourRequestHandler(){
		MessageTemplate tourReqMsgTemp = MessageTemplate.MatchOntology("TourRequest");
		
		MsgReceiver msgReceiver = new MsgReceiver(this, tourReqMsgTemp, Long.MAX_VALUE,null,null){
			protected void handleMessage(ACLMessage request){
				super.handleMessage(request);
				System.out.println("TourGuide received tour guide request message from Profiler");
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				msg.addReceiver(lookUpCurator());
				msg.setOntology("BuildTour");

				try {
					msg.setContentObject(request.getContentObject());

				} catch (Exception e) {
					e.printStackTrace();
				}
				addBehaviour(new BuildTourInit(TourGuideAgent.this, msg, request) );
			}

			@Override
			//Should always be running, if it ends, reboot it
			public int onEnd() {
				tourRequestHandler();
				return super.onEnd();
			}
			
		};
		//adding the MsgReceiver to the behaviours 
		addBehaviour(msgReceiver);
	}
	

	
	//Register services to DF
	private void register(){
		DFAgentDescription desc = new DFAgentDescription();
		desc.setName(getAID());
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("TourGuide");
		serviceDesc.setName(getName());
		desc.addServices(serviceDesc);
		
		try {
			DFService.register(this,  desc);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
	}
	

	//Look up curator using DF
	private AID lookUpCurator(){
		AID curator = null;
		DFAgentDescription desc = new DFAgentDescription();
		ServiceDescription serviceDesc = new ServiceDescription();
		serviceDesc.setType("curator");
		desc.addServices(serviceDesc);
		
		try {
			DFAgentDescription[] foundCurators = DFService.search(this, desc);
			curator = foundCurators[0].getName();
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return curator;
		
	}
	
	//Initiate tour build at curator
	class BuildTourInit extends SimpleAchieveREInitiator{
		ACLMessage original;
		
		public BuildTourInit(Agent agent, ACLMessage msg, ACLMessage profilerMsg){
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
