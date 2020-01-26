package clone;


import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SenderBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.mobility.CloneAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

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

public class Auctioneer extends Agent {

	private TickerBehaviour _auctionTicker = null;
	private double 		_decreaseFactor;
	private long 		_originalPrice;
	private long 		_minimalPrice;
	private long 		_currentPrice;
	private int 		_originalAmount;
	private int 		_currentAmount;
	private int			_auctionCount;
	private int 		_itemsBoughtLastRound;
	
	ArrayList<AID> 				_buyers;	
	AuctionHistory 	_history;
	
	private AID controller;
	private AID origAuctioneer;
	private Location destination;
	private Location origLocation;
	private ArrayList<Location> locations = new ArrayList<Location>();
	int resultCounter;
	long bestPrice;
	ReceiveCommands ReceiveCommands;
	
	@Override
	protected void setup()
	{
		destination = here();
		origLocation = here();
		origAuctioneer = getAID();
		resultCounter =0;
		bestPrice = 0;
		System.out.println("Starting new auctioner agent " + getLocalName()+" in container "+ destination.getName());
		
		_buyers = new ArrayList<AID>();
		_history = new AuctionHistory("buyer", 1, (long) 20000);
		
		// register and subscribe
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Auctioneer");
        sd.setName( getLocalName() );
        register( sd );
        controller = getService("Controller");

		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
		
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
        
		ReceiveCommands= new ReceiveCommands();
		addBehaviour(ReceiveCommands);
	}

	protected void beforeMove()
	{
		System.out.println("Auctioneer agent " + getLocalName()+" starts to move");
	}
	
	protected void afterMove()
	{
		getContentManager().registerLanguage(new SLCodec());
  	    getContentManager().registerOntology(MobilityOntology.getInstance());
    	destination = here();
    	System.out.println("Auctioneer agent " + getLocalName()+" moved to "+destination.getName());
    	
    	if(destination.equals(origLocation)){
    		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    		msg.setContent(String.valueOf(_history._price));
    		msg.addReceiver(origAuctioneer);
    		msg.setLanguage("ENGLISH");
    		send(msg);
    	}
    	else
    	{
    		ServiceDescription sd  = new ServiceDescription();
            sd.setType( "Auctioneer-"+destination.getName() );
            sd.setName( getLocalName() );
            register( sd );
            subscribe ("Buyer-"+destination.getName());
            
            addBehaviour(new myMsgReceiver());
    		
    		addBehaviour(new WakerBehaviour(this, 5000)
    		{
    			protected void handleElapsedTimeout() 
    			{
    				startNewAuction(1, (long) 20000);
    			}
    		}
    		);
    		
    		_auctionTicker = new TickerBehaviour(this, 5000)
    		{ 
    			protected void onTick() 
    			{		
    				if(_auctionCount == 0)
    				{
    					_currentPrice = _originalPrice;
    				}
    				else
    				{
    					_currentPrice = (long) (_currentPrice * _decreaseFactor);
    					
    					// reset items for each round
    					_itemsBoughtLastRound = 0;
    					if(_currentPrice < _minimalPrice)
    					{
    						endAuction();
    						return;
    					}
    				}
    				_auctionCount++;

    				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
    				CfpContent _auctionObject = new CfpContent(_currentAmount, _currentPrice, null);
    				
    				try 
    				{
    					msg.setContentObject(_auctionObject);
    				} 
    				catch (IOException e) 
    				{
    					e.printStackTrace();
    				}
    				
    				for(AID aBuyer: _buyers)
    				{
    					msg.addReceiver(aBuyer);
    				}
    				
    				msg.setLanguage("ENGLISH");
    				send(msg);
    				
    				
    				
    				System.out.println("Auctioner " + getLocalName()+" sending auction bidding price: " + _currentPrice + " with " + _currentAmount + " items left ");
    			}
    		};
    	}
    	
	}
	
	protected void beforeClone()
	{
		System.out.println("Auctioneer agent " + getLocalName()+" starts to clone");
	}

    protected void afterClone() 
    {
    	getContentManager().registerLanguage(new SLCodec());
  	    getContentManager().registerOntology(MobilityOntology.getInstance());
    	destination = here();
    	System.out.println("Starting new cloned auctioneer agent " + getLocalName()+" in container "+destination.getName());
    	
    	removeBehaviour(ReceiveCommands);
    	
    	String[] myName = getLocalName().split("-");
    	for(int i=0; i<locations.size();i++)
    	{
    		if(locations.get(i).getName().contains(myName[2]))
    			doMove(locations.get(i));
    	}
	}
    
    class ReceiveCommands extends CyclicBehaviour 
    {
        public void action() {	
			ACLMessage msg = receive();			
			while (msg!=null){
				if (msg.getSender().equals(getDefaultDF()))
				{
					try 
					{
			            DFAgentDescription[] dfds = DFService.decodeNotification(msg.getContent());
			            if (dfds.length > 0 && msg.getContent().contains("Buyer"))
			            {
			         	   for (int i=0; i<dfds.length; i++)
			         	   {	
			         		   _buyers.add(dfds[i].getName());
			         	   }
			            }
		            }
					catch(Exception e)
					{
						e.printStackTrace();
					}
					System.out.println("Auctioner "+ getLocalName() +": received notify from DF");	
				}
				else if (msg.getSender().equals(controller))
				{
					if (msg.getPerformative() == ACLMessage.REQUEST){
	    			    try {
	    				   ContentElement content = getContentManager().extractContent(msg);
	    				   Concept concept = ((Action)content).getAction();
	    				   if (concept instanceof CloneAction){
	    					  CloneAction ca = (CloneAction)concept;
	    					  String newName = ca.getNewName();
	    					  Location l = ca.getMobileAgentDescription().getDestination();
	    					  if (l != null) destination = l;
	    					  doClone(destination, newName);
	    				   }
	    				}
	    				catch (Exception ex) { ex.printStackTrace(); }
	    			 }
	    			 else { System.out.println("Auctioner "+ getLocalName() +" :Unexpected msg from controller agent"); }
				}
				else
				{
					if (Long.valueOf(msg.getContent())>bestPrice)
						bestPrice=Long.valueOf(msg.getContent());
					resultCounter +=1;
					if (resultCounter ==2)
						System.out.println("Auctioner "+ getLocalName() +" :Auction finished with best price "+bestPrice);
				}
				msg = receive();
			}		
			block();
	    }
    }
    
	public class myMsgReceiver extends CyclicBehaviour 
	{
		public void action() {	
			ACLMessage msg = receive();
			while (msg!=null)
			{
				// Use this later when we want to add new buyers
				if (msg.getSender().equals(getDefaultDF()))
				{
					try 
					{
			            DFAgentDescription[] dfds = DFService.decodeNotification(msg.getContent());
			            if (dfds.length > 0 && msg.getContent().contains("Buyer"))
			            {
			         	   for (int i=0; i<dfds.length; i++)
			         	   {	
			         		   _buyers.add(dfds[i].getName());
			         	   }
			            }
		            }
					catch(Exception e)
					{
						e.printStackTrace();
					}
					System.out.println("Auctioner "+ getLocalName() +": received notify from DF");	
				}
				else
				{
					handleMessage(msg);
				}
				msg = receive();
			}		
			block();
	    }
	}
	
	private void handleMessage(ACLMessage msg)
	{
		int _performative = msg.getPerformative();		
		if(_performative == ACLMessage.PROPOSE)
		{
			try
			{
				int _amountToBuy = Integer.valueOf(msg.getContent());
				ACLMessage _replyMsg = msg.createReply();
				// What is left amount
				int _itemsLeft =  _currentAmount - _amountToBuy;
				// If the auctioner doesnt have more items, let him know
				if(_itemsLeft < 0)
				{
					_replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);
				}
				else
				{
					_replyMsg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					
					_currentAmount = _itemsLeft;
					// For how much is he buying
					Long _totalAmount = _amountToBuy * _currentPrice;
					
					System.out.println("Auctioner " + getLocalName()+": Buyer "+ msg.getSender().getLocalName() +" bought " 
										+ _amountToBuy + " items for " + _totalAmount);
					
					_history = new AuctionHistory(msg.getSender().getLocalName(),_amountToBuy,_currentPrice);
					_itemsBoughtLastRound += _amountToBuy;
				}
				
				this.send(_replyMsg);
				
				if(_itemsLeft == 0)
				{
					endAuction();
				}
				
			}
			catch (Exception e)
			{
				System.out.println("Auctioner "+ getLocalName() +": failed to receive items");
				e.printStackTrace();
			}
		}
		
		
		if(_performative == ACLMessage.NOT_UNDERSTOOD)
		{
			System.out.println("Auctioner "+ getLocalName() +": Whatevs man");
		}
	}
	
	private void endAuction()
	{

		//Kill ticker behaviour and send that the auction is closed
		this.removeBehaviour(_auctionTicker);
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent("AUCTION_ENDED");
		for(AID aBuyer: _buyers)
		{
			msg.addReceiver(aBuyer);
		}
		
		msg.setLanguage("ENGLISH");
		send(msg);
		
		// Print all items sold in this auction
		System.out.println("Auctioner " + getLocalName()+": Auction terminated with " + _currentAmount + " items left at the price of " + _currentPrice + " with " + _auctionCount + " steps");
		System.out.println("Buyer " + _history._name + " bought " + _history._amount + " items for the price of " + _history._price);
		doMove(origLocation);
	}
	
	private void startNewAuction(final int anAmount, final Long anAuctionPrice)
	{
		addBehaviour(new OneShotBehaviour() 
		{
			public void action() 
			{
				_auctionCount = 0;
				_decreaseFactor = 0.95;
				_itemsBoughtLastRound = 0;
				_originalPrice = anAuctionPrice;
				_minimalPrice = (anAuctionPrice / 2);
				
				_originalAmount = anAmount;
				_currentAmount = anAmount;
				
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				
				try 
				{
					msg.setContentObject(anAuctionPrice);
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
				
				for(AID aBuyer: _buyers)
				{
					msg.addReceiver(aBuyer);
				}
				
				msg.setLanguage("ENGLISH");
				
				SenderBehaviour sb = new SenderBehaviour(myAgent, msg);
				sb.action();
				
				System.out.println("Auctioner " + getLocalName()+": New Auction started");
			}
		} ); 
		addBehaviour(_auctionTicker);
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

