package clone;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SenderBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.mobility.CloneAction;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.states.MsgReceiver;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

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


public class Buyer extends Agent
{
	int wantedAmount;
	int boughtAmount;
	long acceptedPrice;
	long boughtPrice;
	ArrayList<BuyerResult> results;
	String state;
	private AID controller;
	private Location destination;
	myMsgReceiver myMsgReceiver;
	
	protected void setup()
	{
		destination = here();
		wantedAmount = 1;
		acceptedPrice = (long) (10000+Math.random() * 10000);
    	System.out.println("Starting new buyer agent " + getLocalName()+" willing to buy "+wantedAmount+" item at price "+acceptedPrice+ " in container "+destination.getName());
    	
    	results = new ArrayList<BuyerResult>();
		state= new String("waiting_for_auction_start");
		
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Buyer");
        sd.setName( getLocalName() );
        register( sd );
        
        ServiceDescription sd1  = new ServiceDescription();
        sd1.setType( "Buyer-"+destination.getName() );
        sd1.setName( getLocalName() );
        register( sd1 );
        
        controller = getService("Controller");
		
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
		
		myMsgReceiver=new myMsgReceiver();
		addBehaviour(myMsgReceiver);
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
	
	protected void beforeClone()
	{
		System.out.println("Buyer agent " + getLocalName()+" starts to clone");
	}

    protected void afterClone() 
    {
    	getContentManager().registerLanguage(new SLCodec());
  	    getContentManager().registerOntology(MobilityOntology.getInstance());
    	destination = here();
    	wantedAmount = 1;
		acceptedPrice = (long) (10000+Math.random() * 10000);
    	System.out.println("Starting new cloned buyer agent " + getLocalName()+" willing to buy "+wantedAmount+" item at price "+acceptedPrice+ " in container "+destination.getName());
    	
    	results = new ArrayList<BuyerResult>();
		state= new String("waiting_for_auction_start");
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Buyer-"+destination.getName() );
        sd.setName( getLocalName() );
        register( sd );
        
        removeBehaviour(myMsgReceiver);
        addBehaviour(new myMsgReceiver());
	}
    
    class ReceiveCommands extends CyclicBehaviour 
    {
        public void action() {	
			ACLMessage msg = receive();			
			while (msg!=null){
				if (msg.getSender().equals(controller))
				{
					if (msg.getPerformative() == ACLMessage.REQUEST){
	    			    try {
	    				   ContentElement content = getContentManager().extractContent(msg);
	    				   Concept concept = ((Action)content).getAction();
	    				   if (concept instanceof CloneAction){
	    					  CloneAction ca = (CloneAction)concept;
	    					  String newName = ca.getNewName();	    				
	    					  doClone(destination, newName);
	    				   }
	    				}
	    				catch (Exception ex) { ex.printStackTrace(); }
	    			 }
	    			 else { System.out.println("Buyer "+ getLocalName() +": Unexpected msg from controller agent"); }
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
			
			while (msg!=null){
				if (msg.getSender().equals(controller))
				{
					if (msg.getPerformative() == ACLMessage.REQUEST){
	    			    try {
	    				   ContentElement content = getContentManager().extractContent(msg);
	    				   Concept concept = ((Action)content).getAction();
	    				   if (concept instanceof CloneAction){
	    					  CloneAction ca = (CloneAction)concept;
	    					  String newName = ca.getNewName();	    				
	    					  doClone(destination, newName);
	    				   }
	    				}
	    				catch (Exception ex) { ex.printStackTrace(); }
	    			 }
	    			 else { System.out.println("Buyer "+ getLocalName() +": Unexpected msg from controller agent"); }
				}
				else if (state.equals("waiting_for_auction_start") && msg.getPerformative()==ACLMessage.INFORM)
				{
					state="waiting_for_cfp";
					System.out.println("Buyer "+ getLocalName() +": auction started, waiting for cfp");	
					
				}
				else if (state.equals("waiting_for_cfp") && msg.getPerformative()==ACLMessage.CFP)
				{
					handleCfp(myAgent, msg);
				}
				else if (state.equals("proposed") && msg.getPerformative()==ACLMessage.REJECT_PROPOSAL)
				{
					handleRejectedProposal(msg);
				}
				else if (state.equals("proposed") && msg.getPerformative()==ACLMessage.ACCEPT_PROPOSAL)
				{
					handleAcceptedProposal(msg);
				}
				else if (!(state.equals("waiting_for_auction_start")) && msg.getPerformative()==ACLMessage.INFORM)
				{
					handle2ndInform(msg);
				}
				msg = receive();
			}		
			block();
	    }
	}
	
	void handleCfp(Agent myAgent, ACLMessage msg)
	{
		try 
		{
			CfpContent cfp=(CfpContent)msg.getContentObject();
			if (cfp.price>acceptedPrice && wantedAmount>0)
			{
				System.out.println("Buyer "+ getLocalName() +": waiting for lower price");
			}
			else if (cfp.price<=acceptedPrice && wantedAmount>0)
			{
				sendProposal(cfp, msg, myAgent);
			}
			else
			{
				sendNotUnderstood(msg, myAgent);
			}
		} 
		catch (UnreadableException e) 
		{
			e.printStackTrace();
		}		
	}
	
	void sendProposal(CfpContent cfp, ACLMessage msg, Agent myAgent)
	{
		boughtPrice = cfp.price;
		ACLMessage reply = msg.createReply();
		reply.setPerformative(ACLMessage.PROPOSE);
		reply.setLanguage("ENGLISH");
		reply.setOntology("Dutch Auction");
		if (wantedAmount<=cfp.amount)
		{
			boughtAmount=wantedAmount;
		}
		else
		{
			boughtAmount=cfp.amount;
		}
		reply.setContent(String.valueOf(boughtAmount));
		
		myAgent.send(reply);
		System.out.println("Buyer "+ getLocalName() +": proposed to buy "+boughtAmount+" at price "+boughtPrice);
		state = "proposed";
	}
	
	void sendNotUnderstood(ACLMessage msg, Agent myAgent)
	{
		ACLMessage reply = msg.createReply();
		reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
		reply.setLanguage("ENGLISH");
		reply.setOntology("Dutch Auction");
		
		myAgent.send(reply);
		System.out.println("Buyer "+ getLocalName() +": do not understand the auction");
		state = "not_understood";
	}
	
	void handleRejectedProposal(ACLMessage msg)
	{
		System.out.println("Buyer "+ getLocalName() +": proposal to buy "+boughtAmount+" at price "+boughtPrice+" rejected");
		boughtAmount=0;
		boughtPrice =0;
		state = "waiting_for_cfp";
	}
	
	void handleAcceptedProposal(ACLMessage msg)
	{
		System.out.println("Buyer "+ getLocalName() +": proposal to buy "+boughtAmount+" at price "+boughtPrice+" accepted");
		wantedAmount -= boughtAmount;
		results.add(new BuyerResult(boughtAmount,boughtPrice));
		boughtAmount=0;
		boughtPrice =0;
		state = wantedAmount>0 ? "waiting_for_cfp":"auction_ended";
	}
	
	void handle2ndInform(ACLMessage msg)
	{
		state = "waiting_for_auction_start";
		System.out.println("Buyer "+ getLocalName() +": auction finished, results:");
		if (results.size()==0)
			System.out.println("no items bought");
		else
			{
			for (int i=0; i<results.size(); i++)
				System.out.println(results.get(i).print());
			}
		wantedAmount = 1;
		acceptedPrice = (long) (10000+Math.random() * 10000);
		System.out.println("Buyer " + getLocalName()+" ready to buy again and willing to buy "+wantedAmount+" item at price "+acceptedPrice);
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

