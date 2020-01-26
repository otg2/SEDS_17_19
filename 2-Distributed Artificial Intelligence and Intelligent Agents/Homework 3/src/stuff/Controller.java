package stuff;

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
import jade.wrapper.StaleProxyException;
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

public class Controller extends Agent
{
	int sizeofboard;
	int queenCounter;
	protected void setup()
	{
		try 
		{
			Object[] args = getArguments();
	        if (args != null) {
	        	sizeofboard = Integer.valueOf((String)args[0]);
	        }
	        queenCounter=0;
			
			final AgentContainer main = this.getContainerController();
			jade.core.Runtime _runtime = jade.core.Runtime.instance();
			
			AgentController _Chessboard_Control = 
					main.createNewAgent("Chessboard", Chessboard.class.getName(), args);
			
			_Chessboard_Control.start();
			
			addBehaviour(new WakerBehaviour(this, 5000)
			{
				protected void handleElapsedTimeout() 
				{
					addBehaviour(new TickerBehaviour(myAgent, 5000){
						protected void onTick(){
		                    if(queenCounter<sizeofboard){
								try {
									AgentController _queen_Control;
									Object[] args1 = new Object[2];
									args1[0]=String.valueOf(sizeofboard);
									args1[1]=String.valueOf(queenCounter+1);
									_queen_Control = main.createNewAgent("Queen"+String.valueOf(queenCounter+1), Queen.class.getName(), args1);
									_queen_Control.start();
									queenCounter+=1;
								} catch (StaleProxyException e) {
									e.printStackTrace();
								}
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
}

