package clone;

import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.ProfileImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SenderBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.ReceiverBehaviour.NotYetReady;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

import java.util.*;
import java.io.*;

import jade.lang.acl.*;
import jade.content.*;
import jade.content.onto.basic.*;
import jade.content.lang.*;
import jade.content.lang.sl.*;
import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.mobility.*;
import jade.domain.JADEAgentManagement.*;
import jade.gui.*;

public class Container extends Agent
{	
	ArrayList<AID> 				_buyers;	
	ArrayList<AID> 				_Auctioneers;	
	private Location destination;
	private ArrayList<Location> locations = new ArrayList<Location>();
	int cloneCounterBuyer;
	int cloneCounterAuctioneer;
	
	protected void setup()
	{
		try 
		{
			getContentManager().registerLanguage(new SLCodec());
			getContentManager().registerOntology(MobilityOntology.getInstance());
			
			destination = here();
			cloneCounterAuctioneer=0;
			cloneCounterBuyer =0;
			ServiceDescription sd  = new ServiceDescription();
	        sd.setType( "Controller" );
	        sd.setName( getLocalName() );
	        register( sd );
	        _buyers = new ArrayList<AID>();
	        _Auctioneers = new ArrayList<AID>();
	        subscribe ("Buyer");
	        subscribe ("Auctioneer");
	        
			AgentContainer main = this.getContainerController();
			jade.core.Runtime _runtime = jade.core.Runtime.instance();
			
			
			AgentContainer container_1 = 
					_runtime.createAgentContainer(new ProfileImpl());
			AgentContainer container_2 = 
					_runtime.createAgentContainer(new ProfileImpl());
			
			AgentController _auctioner_ONE_Control = 
					main.createNewAgent("Auctioneer", Auctioneer.class.getName(), new Object[0]);
			
			// Create two buyers
			AgentController _buyer_ONE_Control = 
					container_1.createNewAgent("buyer1", Buyer.class.getName(), new Object[0]);
			AgentController _buyer_TWO_Control = 
					container_2.createNewAgent("buyer2", Buyer.class.getName(), new Object[0]);
			
			_auctioner_ONE_Control.start();
			_buyer_ONE_Control.start();
			_buyer_TWO_Control.start();
			
			try {
		    	sendRequest(new Action(getAMS(), new QueryPlatformLocationsAction()));
		    	
		    	MessageTemplate mt = MessageTemplate.and(
		                MessageTemplate.MatchSender(getAMS()),
		                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		        ACLMessage resp = blockingReceive(mt);
		        ContentElement ce = getContentManager().extractContent(resp);
		        Result result = (Result) ce;
		        jade.util.leap.Iterator it = result.getItems().iterator();
		        while (it.hasNext()) {
		          Location loc = (Location)it.next();
		          locations.add(loc);
		        };
		    }
		    catch (Exception e) { e.printStackTrace(); }
			
			addBehaviour(new myMsgReceiver());
			
			addBehaviour(new WakerBehaviour(this, 5000)
			{
				protected void handleElapsedTimeout() 
				{
					addBehaviour(new TickerBehaviour(myAgent, 5000){
						protected void onTick(){
							if(cloneCounterBuyer<2) {
								for (int i=0; i<_buyers.size();i++){
									 Location aloc= here();
							         MobileAgentDescription mad = new MobileAgentDescription();
							         mad.setName(_buyers.get(i));
							         mad.setDestination(aloc);
							         String newName = _buyers.get(i).getLocalName()+"-clone-"+(cloneCounterBuyer+1);
							         CloneAction ca = new CloneAction();
							         ca.setNewName(newName);
							         ca.setMobileAgentDescription(mad);
							         sendRequest(new Action(_buyers.get(i), ca));
								}
								cloneCounterBuyer+=1;
							}
							if(cloneCounterAuctioneer<2 && cloneCounterBuyer ==2)
							{
								for (int i=0; i<_Auctioneers.size();i++){
									 Location aloc= here();
							         MobileAgentDescription mad = new MobileAgentDescription();
							         mad.setName(_Auctioneers.get(i));
							         mad.setDestination(aloc);
							         String newName = _Auctioneers.get(i).getLocalName()+"-clone-"+(cloneCounterAuctioneer+1);
							         CloneAction ca = new CloneAction();
							         ca.setNewName(newName);
							         ca.setMobileAgentDescription(mad);
							         sendRequest(new Action(_Auctioneers.get(i), ca));
								}
								cloneCounterAuctioneer+=1;
							}
						}
					});
				}
			}
			);

		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	protected void sleep(long timer)
	{
		Long StartTime=System.currentTimeMillis();
		Long CurrentTime=System.currentTimeMillis();
		while(CurrentTime-StartTime<timer)
		{
			CurrentTime=System.currentTimeMillis();
		}
	}
	
	public class myMsgReceiver extends CyclicBehaviour 
	{
		public void action() {	
			
			ACLMessage msg = receive();
			
			while (msg!=null){
				if (msg.getSender().equals(getDefaultDF()))
				{
					handleNotification(myAgent, msg);
				}
				msg = receive();
			}		
			block();
	    }
	}
	
	private void handleNotification(Agent myAgent, ACLMessage msg)
	{
		try {
            DFAgentDescription[] dfds =    
                 DFService.decodeNotification(msg.getContent());
            if (dfds.length > 0 && msg.getContent().contains("Buyer")){
         	   for (int i=0; i<dfds.length; i++){	
         		  _buyers.add(dfds[i].getName());
         	   }
            }
            if (dfds.length > 0 && msg.getContent().contains("Auctioneer")){
          	   for (int i=0; i<dfds.length; i++){	
          		 _Auctioneers.add(dfds[i].getName());
          	   }
             }
          }
          catch (Exception ex) {ex.printStackTrace();}
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
	
	void sendRequest(Action action)
	{
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

