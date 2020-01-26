package stuff;

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

import jade.core.AID;
import jade.core.Agent;
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
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class Queen extends Agent 
{
	int[] position;
	int sizeofboard;
	int myID;
	int DFSeq;
	int AbortSeq;
	boolean settled;
	ArrayList<AID> queens;
	AID chessboard;
	AID previousQueen;
	AID nextQueen;
	ArrayList<String> possibleY;
	String AllPositionsRecv;
	String AllPositionsSent;
	
	protected void setup() 
    { 
        Object[] args = getArguments();
        if (args != null) {
        	sizeofboard = Integer.valueOf((String)args[0]);
        	myID = Integer.valueOf((String)args[1]);
        }
        
        System.out.println("Starting new queen agent " + myID +"/"+ sizeofboard);
        
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Queen" );
        sd.setName( getLocalName() );
        register( sd );
        subscribe ("Queen");
        
        possibleY = new ArrayList<String>();
        for (int i=1; i<=sizeofboard;i++)
        {
        	possibleY.add(String.valueOf(i));
        }
        position = new int[2];
        position[0] = myID;
        position[1] = Integer.valueOf(possibleY.get(0));
        AllPositionsSent = String.valueOf(position[0])+","+String.valueOf(position[1]);
        chessboard = getService("Chessboard");
        DFSeq =0;
        AbortSeq = 0;
        
        queens = new ArrayList<AID>();
        
        if(myID==1)
        	sendPositions(chessboard);
        
        addBehaviour(new myMsgReceiver());
    }
	
	public class myMsgReceiver extends CyclicBehaviour 
	{
		public void action() {	
			ACLMessage msg = receive();
			while (msg!=null)
			{
				if (msg.getSender().equals(getDefaultDF()))
				{
					try 
					{
						DFAgentDescription[] dfds = DFService.decodeNotification(msg.getContent());
			            if (dfds.length > 0 && msg.getContent().contains("Queen"))
			            {
			               DFSeq +=1;
			         	   for (int i=0; i<dfds.length; i++)
			         	   {	
			         		  queens.add(dfds[i].getName());
			         	   }
			         	  if (DFSeq ==2 && dfds.length ==1)
		         		  {
			         		 nextQueen= dfds[0].getName();
			         		 sendPositions(nextQueen);
		         		  }
			            }
		            }
					catch(Exception e)
					{
						e.printStackTrace();
					}
					System.out.println("Queen "+ getLocalName() +": received notify from DF");	
				}
				/*else if (msg.getSender().equals(chessboard))
				{
					sizeofboard = Integer.valueOf(msg.getContent());
			        position[1] = (int)(1+Math.random() * sizeofboard);
			        for (int i=1; i<=sizeofboard;i++)
			        {
			        	possibleY.add(String.valueOf(i));
			        }
			        if (myID==1)
		        	    sendMyPosition(chessboard);
				}*/
				else
				{
					handleMessage(msg);	
				}
				msg = receive();
			}		
			block();
	    }
	}

	
	void sendPositions(AID Agent)
	{
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent(AllPositionsSent);
		msg.addReceiver(Agent);
		msg.setLanguage("ENGLISH");
		send(msg);
	    System.out.println("Queen agent " + myID +" sent positions: "+AllPositionsSent);
	}
	
	void sendAbort2PreQueen(AID Agent)
	{
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent("Abort");
		msg.addReceiver(Agent);
		msg.setLanguage("ENGLISH");
		send(msg);
		System.out.println("Queen agent " + myID +" sent abort");
	}
	
	void sendRearrange2PreQueen(AID Agent)
	{
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent("Rearrange");
		msg.addReceiver(Agent);
		msg.setLanguage("ENGLISH");
		send(msg);
		System.out.println("Queen agent " + myID +" sent Rearrange");
	}
	
	void handleMessage(ACLMessage msg)
	{
		if (!msg.getContent().equals("Abort") && !msg.getContent().equals("Rearrange"))
		{
			previousQueen = msg.getSender();
			AllPositionsRecv = msg.getContent();
			System.out.println("Queen agent " + myID +" received positions: "+AllPositionsRecv);
			String[] positions = msg.getContent().split("_");
			for (int i=0;i<positions.length;i++)
			{
				String[] position_tmp = positions[i].split(",");
				possibleY.remove(position_tmp[1]);
				possibleY.remove(String.valueOf(Integer.valueOf(position_tmp[1])+(position[0]-Integer.valueOf(position_tmp[0]))));
				possibleY.remove(String.valueOf(Integer.valueOf(position_tmp[1])-(position[0]-Integer.valueOf(position_tmp[0]))));
			}
			System.out.println("Queen agent " + myID +" possible Y: "+possibleY);
			if (possibleY.isEmpty())
			{
				for (int i=1; i<=sizeofboard;i++)
		        {
		        	possibleY.add(String.valueOf(i));
		        }
				sendAbort2PreQueen(previousQueen);
			}
			else
				{
				    position[1] = Integer.valueOf(possibleY.get(0));
				    AllPositionsSent = AllPositionsRecv + "_"+String.valueOf(position[0])+","+String.valueOf(position[1]);
				    if (nextQueen !=null)
				        sendPositions(nextQueen);
				    else
				    	sendPositions(chessboard);
				}
		}
		else if(msg.getContent().equals("Abort"))
		{
			System.out.println("Queen agent " + myID +" received abort");
			AbortSeq+=1;
			if (possibleY.isEmpty() || possibleY.size()<AbortSeq+1 )
			{
				possibleY = new ArrayList<String>();
				for (int i=1; i<=sizeofboard;i++)
		        {
		        	possibleY.add(String.valueOf(i));
		        }
				AbortSeq=0;
				sendAbort2PreQueen(previousQueen);
			}
			else
				{
				    position[1] = Integer.valueOf(possibleY.get(AbortSeq));
				    if (myID==1)
				    	AllPositionsSent = String.valueOf(position[0])+","+String.valueOf(position[1]);
				    else 
				        AllPositionsSent = AllPositionsRecv + "_"+String.valueOf(position[0])+","+String.valueOf(position[1]);
				    if (nextQueen !=null)
				        sendPositions(nextQueen);
				    else
				    	sendPositions(chessboard);
				}
		}
		else //rearrange
		{
			System.out.println("Queen agent " + myID +" received rearrange");
			AbortSeq+=1;
			if (possibleY.isEmpty() || possibleY.size()<AbortSeq+1 )
			{
				possibleY = new ArrayList<String>();
				for (int i=1; i<=sizeofboard;i++)
		        {
		        	possibleY.add(String.valueOf(i));
		        }
				AbortSeq=0;
				if (previousQueen!=null)
				    sendRearrange2PreQueen(previousQueen);
				else
				{
					position[1] = Integer.valueOf(possibleY.get(AbortSeq));
				    AllPositionsSent = String.valueOf(position[0])+","+String.valueOf(position[1]);
				    if (nextQueen !=null)
				        sendPositions(nextQueen);
				    else
				    	sendPositions(chessboard);
				}
			}
			else
				{
				    position[1] = Integer.valueOf(possibleY.get(AbortSeq));
				    if (myID==1)
				    	AllPositionsSent = String.valueOf(position[0])+","+String.valueOf(position[1]);
				    else 
				        AllPositionsSent = AllPositionsRecv + "_"+String.valueOf(position[0])+","+String.valueOf(position[1]);
				    if (nextQueen !=null)
				        sendPositions(nextQueen);
				    else
				    	sendPositions(chessboard);
				}
		}
		
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
}
