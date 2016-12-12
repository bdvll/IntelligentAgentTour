package hw3;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.wrapper.AgentContainer;

public class QueenController extends Agent{
	int NUMBER_OF_QUEENS = 3;
	
	@Override
	protected void setup() {
		super.setup();
		//Behaviour for creating Queens
		Object[] args = getArguments();
        this.NUMBER_OF_QUEENS = Integer.parseInt(args[0].toString());
		AgentContainer home = this.getContainerController();
		SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();
		for(int i=NUMBER_OF_QUEENS-1; i>=0; i--){
			int n= i;
			OneShotBehaviour oneShotBehaviour = new OneShotBehaviour() {
				@Override
				public void action() {
					try {
						Thread.sleep(150*(NUMBER_OF_QUEENS-n));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String queenName = "Queen"+n;
					createAgent(queenName, home, n);
				}
			};
			sequentialBehaviour.addSubBehaviour(oneShotBehaviour);
		}
		addBehaviour(sequentialBehaviour);
	}

	void createAgent(String name, AgentContainer container, int number){

	     jade.wrapper.AgentController a = null;
      try {
    	  
    	  Object[] args = new Object[2];
    	  args[0] = number;
    	  args[1] = NUMBER_OF_QUEENS;
    	  a = container.createNewAgent(name, QueenAgent.class.getName(), args);
	      a.start();
	     }
      catch (Exception ex) {
		    System.out.println("Problem creating new agent");
		    ex.printStackTrace();
	     }
      return;
  }
}

