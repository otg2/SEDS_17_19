package stuff;

import jade.core.AID;
import jade.core.Agent;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SenderBehaviour;
import jade.core.behaviours.ReceiverBehaviour.NotYetReady;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.states.MsgReceiver;


public class TourGuideAgent extends Agent {

	List<AID> curator;
	ACLMessage request;
	ACLMessage reply;
	UserProfile user;
	ArrayList<ArtDisplay> artObjects;
	String _TourGeneration_Type;
	@Override
	protected void setup()
	{	
		System.out.println("Starting new Tourguide agent " + getLocalName());
		
		Object[] args = getArguments();
		if (args != null && args.length > 0)
		{ 
			_TourGeneration_Type =  ((String)args[0]).toUpperCase();
			System.out.println("TourGuide "+ getLocalName() +": With focus on " + _TourGeneration_Type);
		}
		else 
		{
			System.out.println("TourGuide "+ getLocalName() +": No special focus. Matches all interests");
			_TourGeneration_Type = "ALL";
			//doDelete();
		}
		
		// Register to DF
        ServiceDescription sd  = new ServiceDescription();
        sd.setType( "TourGuide_"+_TourGeneration_Type );
        sd.setName( getLocalName() );
        register( sd );
        subscribe ("Curator");
        
        curator= new ArrayList<AID>();
        
        addBehaviour(new myMsgReceiver());
	}
	
	public class myMsgReceiver extends CyclicBehaviour 
	{
		public void action() {	
			
			ACLMessage msg = receive();
			
			while (msg!=null){
				if (msg.getSender().equals(getDefaultDF()))
				{
					handleNotification(msg);
					System.out.println("TourGuide "+ getLocalName() +": received notify from DF");	
				}
				else
				{
					handleTourMessage(msg, myAgent);
				}
				msg = receive();
			}		
			block();
	    }
	}
	
	private void handleNotification(ACLMessage msg)
	{
		try {
            DFAgentDescription[] dfds =    
                 DFService.decodeNotification(msg.getContent());
            if (dfds.length > 0 && msg.getContent().contains("Curator")){
         	   for (int i=0; i<dfds.length; i++){	
         		   curator.add(dfds[i].getName());
         	   }
         		   
            }
          }
          catch (Exception ex) {ex.printStackTrace();}
	}
	
	private void handleTourMessage(ACLMessage msg, Agent myAgent)
	{
		try{
			request = msg;
			FSMBehaviour fsm = new FSMBehaviour(myAgent)
			{
	            public int onEnd() {
	                System.out.println("TourGuide "+ getLocalName() +": FSMBehaviour finished");	
					return super.onEnd();
	            }
			};
			fsm.registerFirstState(new ReceiverForProfile(), "RCV_FROM_PRO");
	        fsm.registerState(new SenderForCurator(), "SENT_TO_CUR");
	        fsm.registerState(new ReceiverForCurator(), "RCV_FROM_CUR");
	        fsm.registerState(new SenderForProfile(), "RSP_TO_PRO");
			fsm.registerLastState(new CleanUp(), "Clean_UP");
			fsm.registerDefaultTransition("RCV_FROM_PRO", "SENT_TO_CUR");
			fsm.registerDefaultTransition("SENT_TO_CUR", "RCV_FROM_CUR");			
			fsm.registerDefaultTransition("RCV_FROM_CUR", "RSP_TO_PRO");
			fsm.registerDefaultTransition("RSP_TO_PRO", "Clean_UP");
			myAgent.addBehaviour(fsm);	
		}catch(Exception e){ e.printStackTrace();}
	}
	
	public class ReceiverForProfile extends OneShotBehaviour 
	{
		public void action() {
			if (request !=null)
			{
				try 
				{
				  user = (UserProfile) request.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				System.out.println("TourGuide "+ getLocalName() +": received request from profile");
		    }
	}
	}
	
	public class ReceiverForCurator extends OneShotBehaviour 
	{
		public void action() {
			//System.out.println("ReceiverForCurator starts");
			if (request != null){
				long StartTime=System.currentTimeMillis();
				long currentTime=System.currentTimeMillis();
				
				while(reply == null && currentTime-StartTime < 10000){
					reply = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
					currentTime=System.currentTimeMillis();
				}
				if (reply !=null)
				{
					try 
					{
					  artObjects = (ArrayList<ArtDisplay>) reply.getContentObject();
					  System.out.println("TourGuide "+ getLocalName() +": found " + artObjects.size() + " items");
					  
					  
					} 
					catch (UnreadableException e) 
					{
						e.printStackTrace();
					}
					System.out.println("TourGuide "+ getLocalName() +": received reply from curator");
			    }
			}
		}
	}
	
	public class SenderForCurator extends OneShotBehaviour 
	{
		public void action() {
			//System.out.println("SenderForCurator starts");
			if (request != null && user != null)
			{
				ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
				
				user._occupation = _TourGeneration_Type;
				
				try {
					msg.setContentObject(user);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (curator.isEmpty()){
					msg.addReceiver(getService("Curator"));
				}
				else
				    msg.addReceiver(curator.get(0));
				
				msg.setLanguage("ENGLISH");
				msg.setOntology("TourGuide-Curator");
				
				SenderBehaviour sb = new SenderBehaviour(myAgent, msg);
				sb.action();
				System.out.println("TourGuide "+ getLocalName() +": forward request to curator");	
			}
		}
	}
	
	public class SenderForProfile extends OneShotBehaviour 
	{
		
		public void action() {
			//System.out.println("SenderForProfile starts");
			if (request != null && reply !=null && artObjects != null)
			{
				ACLMessage msg = request.createReply();
				msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				
				// Mark all objects received with its name
				
				for(ArtDisplay disp : artObjects)
				{
					disp._tourGuideName = myAgent.getLocalName();
					disp._tourSpecification = _TourGeneration_Type;
				}
				
				try {
					msg.setContentObject(artObjects);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				msg.setLanguage("ENGLISH");
				msg.setOntology("Profiler-TourGuide");
				
				myAgent.send(msg);
				System.out.println("TourGuide "+ getLocalName() +": forward reply to Profile");
			}			
		}
	}
	
	public class CleanUp extends OneShotBehaviour 
	{
		public void action() {
			request = null;
			reply =null;
			user = null;
			artObjects = null;
            System.out.println("TourGuide "+ getLocalName() +": clean up done");		
		}
	}
	
	protected void takeDown()
	{
		try { DFService.deregister(this); }
		catch (Exception e) {}
		
	}
	
	void register( ServiceDescription sd)
	{
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

        try {
        	DFAgentDescription list[] = DFService.search( this, dfd );
			if ( list.length>0 ) 
            	DFService.deregister(this);
            	
            dfd.addServices(sd);
			DFService.register(this,dfd);
		}
        catch (FIPAException fe) { fe.printStackTrace(); }
	}
	
	AID getService( String service )
	{
		DFAgentDescription dfd = new DFAgentDescription();
   		ServiceDescription sd = new ServiceDescription();
   		sd.setType( service );
		dfd.addServices(sd);
		try
		{
			DFAgentDescription[] result = DFService.search(this, dfd);
			if (result.length>0)
				return result[0].getName() ;
		}
        catch (FIPAException fe) { fe.printStackTrace(); }
      	return null;
	}
	
	void subscribe( String service )
	{
		DFAgentDescription dfd = new DFAgentDescription();
   		ServiceDescription sd = new ServiceDescription();
   		sd.setType( service );
		dfd.addServices(sd);
	    SearchConstraints sc = new SearchConstraints();
	    sc.setMaxResults(new Long(1));
	      
	    send(DFService.createSubscriptionMessage(this, getDefaultDF(), 
	                                                       dfd, sc));
	}

	
}
