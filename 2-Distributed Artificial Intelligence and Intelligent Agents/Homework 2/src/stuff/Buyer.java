package stuff;

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



public class Buyer extends Agent
{
	int wantedAmount;
	int boughtAmount;
	long acceptedPrice;
	long boughtPrice;
	//long wallet;
	float willingness;
	int decreaseCounter;
	float increaseFactor;
	float decreaseFactor;
	boolean strategyEnabled; 
	ArrayList<AID> buyers;
	ArrayList<BuyerResult> results;
	String state;
	ArrayList<Integer> user_input;	
	
	protected void setup()
	{
		user_input = new ArrayList<Integer>();
		Object[] args = getArguments();
		if (args != null && args.length > 0)
		{ 
			for(int i = 0 ; i < args.length; i++)
			{
				user_input.add(Integer.valueOf((String)args[i]));
			}
			wantedAmount = generateWantedAmount(user_input);
			acceptedPrice = generateAcceptedPrice(user_input);
			willingness = generateWillingness(user_input);
			strategyEnabled = generateStrategyEnabled(user_input);
		}
		else 
		{
			wantedAmount = (int) (1+Math.random() * 10);
			acceptedPrice = (long) (10000+Math.random() * 10000);
			willingness = (float) Math.random();
			strategyEnabled = true;
		}
		
		//wallet = (long)(acceptedPrice * wantedAmount + (int)(Math.random()* 100000));
		increaseFactor =1+willingness/4;
		decreaseFactor =1-(1-willingness)/4;
		decreaseCounter = (int)(Math.random()*5)+1;
		results = new ArrayList<BuyerResult>();
		buyers = new ArrayList<AID>();
		state= new String("waiting_for_auction_start");
		System.out.println("Starting new buyer agent " + getLocalName()+" willing("+willingness+") to buy "+wantedAmount+" items at price "+acceptedPrice);
		
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Buyer" );
        sd.setName( getLocalName() );
        register( sd );
        subscribe("Buyer");
        
        addBehaviour(new myMsgReceiver());
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
			    else if (state.equals("waiting_for_auction_start") && msg.getPerformative()==ACLMessage.INFORM)
				{
					if(strategyEnabled)
					{
						handle1stInform(msg);
					}
					else
					{
						state="waiting_for_cfp";
						System.out.println("Buyer "+ getLocalName() +": auction started, waiting for cfp");	
					}
				}
				else if (state.equals("waiting_for_cfp") && msg.getPerformative()==ACLMessage.CFP)
				{
					if(strategyEnabled)
					{
						handleCfpStrategy(myAgent, msg);
					}
					else
					{
						handleCfp(myAgent, msg);
					}
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
	
	private void handleNotification(Agent myAgent, ACLMessage msg)
	{
		try {
            DFAgentDescription[] dfds =    
                 DFService.decodeNotification(msg.getContent());
            if (dfds.length > 0 && msg.getContent().contains("Buyer")){
         	   for (int i=0; i<dfds.length; i++){	
         		   buyers.add(dfds[i].getName());
         	   }
         		   
            }
          }
          catch (Exception ex) {ex.printStackTrace();}
		System.out.println("Buyer "+ getLocalName() +": new buyers joined, current number of buyers is "+buyers.size());	
	}
	
	void handle1stInform(ACLMessage msg)
	{
		state="waiting_for_cfp";
		System.out.println("Buyer "+ getLocalName() +": auction started, waiting for cfp");	
		if(willingness>=0.7 || buyers.size()>=4)
		{
			acceptedPrice = (long)(acceptedPrice*increaseFactor);
			System.out.println("Buyer "+ getLocalName() +": increase acceptable price to "+acceptedPrice);
		}
		else if (willingness>=0.4 || buyers.size()==3)
		{
			System.out.println("Buyer "+ getLocalName() +": keep acceptable price unchanged");
		}
		else 
		{
			if(decreaseCounter>0)
			{
				acceptedPrice=(long)(acceptedPrice*decreaseFactor);
				System.out.println("Buyer "+ getLocalName() +": decrease acceptable price to "+acceptedPrice);
				decreaseCounter -= 1;
			}
		}	
	}
	
	void handleCfpStrategy(Agent myAgent, ACLMessage msg)
	{
		try 
		{
			CfpContent cfp=(CfpContent)msg.getContentObject();
			
			if (cfp.price>acceptedPrice && wantedAmount>0)
			{
				if(wantedAmount*2<=cfp.amount)
				{
					System.out.println("Buyer "+ getLocalName() +": lots items left, waiting for lower price");
				}
				else 
				{
					acceptedPrice=(long)(acceptedPrice*increaseFactor);
					System.out.println("Buyer "+ getLocalName() +": not many items left, increase acceptable price to "+acceptedPrice);
					if (cfp.price<=acceptedPrice)
					{
						sendProposal(cfp, msg, myAgent);
					}
				}	
			}
			else if (cfp.price<=acceptedPrice && wantedAmount>0)
			{
				if(wantedAmount*2<=cfp.amount&& decreaseCounter>0)
				{
					while (cfp.price<=acceptedPrice && decreaseCounter>0)
						{
						acceptedPrice = (long)(acceptedPrice*decreaseFactor);
						decreaseCounter -= 1;
						}
					System.out.println("Buyer "+ getLocalName() +": lots items left, decrease acceptable price to "+acceptedPrice+" and wait for it");
				}
				else 
				{
					sendProposal(cfp, msg, myAgent);
				}	
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
		if (!user_input.isEmpty())
		{ 
			wantedAmount = generateWantedAmount(user_input);
			acceptedPrice = generateAcceptedPrice(user_input);
			willingness = generateWillingness(user_input);
			strategyEnabled = generateStrategyEnabled(user_input);
		}
		else 
		{
			wantedAmount = (int) (1+Math.random() * 10);
			acceptedPrice = (long) (10000+Math.random() * 10000);
			willingness = (float) Math.random();
			strategyEnabled = true;
		}
		System.out.println("Buyer " + getLocalName()+" ready to buy again and willing("+willingness+") to buy "+wantedAmount+" items at price "+acceptedPrice);
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
	
	int generateWantedAmount( ArrayList<Integer> user_input)
	{
		int a = 0;
		switch(user_input.get(1)) 
		{ 
		   case 1: 
		       a= 3;
		       break;
		   case 2: 
			   a= 5;
			   break;
		   case 3: 
			   a= 7;
			   break;
		   case 4: 
			   a= 9; 
			   break;
		   case 5: 
			   a= 11;
			   break;
		}
		return a; 
	}
	
	long generateAcceptedPrice( ArrayList<Integer> user_input)
	{
		long a = 0;
		switch(user_input.get(1)) 
		{ 
		   case 1: 
		       a= 13000;
		       break;
		   case 2: 
			   a= 15000;
			   break;
		   case 3: 
			   a= 17000;
			   break;
		   case 4: 
			   a= 18500; 
			   break;
		   case 5: 
			   a= 20000;
			   break;
		}
		return a; 
	}
	
	float generateWillingness( ArrayList<Integer> user_input)
	{
		float a = 0;
		switch(user_input.get(1)) 
		{ 
		   case 1: 
		       a= (float) 0.9;
		       break;
		   case 2: 
			   a= (float) 0.7;
			   break;
		   case 3: 
			   a= (float) 0.5;
			   break;
		   case 4: 
			   a= (float) 0.4; 
			   break;
		   case 5: 
			   a= (float) 0.2;
			   break;
		}
		return a; 
	}
	
	boolean generateStrategyEnabled( ArrayList<Integer> user_input)
	{
		boolean a = true;
		switch(user_input.get(0)) 
		{ 
		   case 1: 
		       a= true;
		       break;
		   case 0: 
			   a= false;
			   break;
		}
		return a; 
	}
}

