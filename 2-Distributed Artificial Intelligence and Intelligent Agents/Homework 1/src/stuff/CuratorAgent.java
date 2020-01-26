package stuff;

import jade.core.AID;

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
import jade.core.behaviours.ReceiverBehaviour.NotYetReady;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class CuratorAgent extends Agent {

	ArrayList<ArtObject> _allArtifacts;
	
	protected void setup()
	{
		System.out.println("Curator: creating a new Curator agent " + getLocalName());
		
		SequentialBehaviour addServiceInSequence = new SequentialBehaviour();

		addServiceInSequence.addSubBehaviour(new OneShotBehaviour() {			
			@Override
			public void action() 
			{
				// Create the arts
				MarkovChain _chain = new MarkovChain();			
				_allArtifacts = new ArrayList<ArtObject>();
				
				int _numberOfArts = 50 + (int) (Math.random()*50);
				for (int i = 0; i < _numberOfArts; i++) 
				{
					ArtObject _newArtifact = new ArtObject(_chain);
					_allArtifacts.add(_newArtifact);
					
					System.out.println("Curator: created artifact id " + _newArtifact._id + 
							" name " + _newArtifact._name + 
							" by " + _newArtifact._creator + 
							". Country " + _newArtifact._placeOfCreation);
				}
			}
		});
		
		addServiceInSequence.addSubBehaviour(new OneShotBehaviour() 
		{
			@Override
			public void action() 
			{
				ServiceDescription sd  = new ServiceDescription();
		        sd.setType( "Curator" );
		        sd.setName( getLocalName() );
		        register( sd );
			}
		});
		
		
		
		addServiceInSequence.addSubBehaviour(new CyclicBehaviour(){
			public void action(){
				
				ACLMessage received= receive();
                while (received!=null)
                {
                	try 
					{
						String _ontology = received.getOntology();
						
						if(_ontology == "TourGuide-Curator")
						{
							UserProfile _user;
							try
							{
								_user = (UserProfile) received.getContentObject();							
								ArrayList<ArtDisplay> _interestingArts = generateTourForUserPreference(_user);							
								ACLMessage replyArtifacts = received.createReply();
								replyArtifacts.setPerformative(ACLMessage.PROPOSE);
								if(_interestingArts.size()>4)
								{
									ArrayList<ArtDisplay> _shortinterestingArts = new ArrayList<ArtDisplay>();
									for (int i=0; i<5; i++)
									{
										_shortinterestingArts.add(_interestingArts.get(i));
									}
									replyArtifacts.setContentObject(_shortinterestingArts);	
								}
								else
									replyArtifacts.setContentObject(_interestingArts);	   		
								myAgent.send(replyArtifacts);
								
							}
							catch (Exception e)
							{
								_user = null;
								e.printStackTrace();
							}
						}
						else if(_ontology == "Profiler-Curator")
						{
							ArtObject _artObject;
							try
							{
								int _receivedId = (Integer) received.getContentObject();

								_artObject = fetchArtifactDetail(_receivedId);
								
								ACLMessage replyArtifacts = received.createReply();
								replyArtifacts.setPerformative(ACLMessage.PROPAGATE);
								replyArtifacts.setContentObject(_artObject);
								replyArtifacts.setLanguage("CURATOR");
								
								myAgent.send(replyArtifacts);
							}
							catch (Exception e)
							{
								_artObject = null;
								e.printStackTrace();
								ACLMessage reply = received.createReply();
								reply.setPerformative(ACLMessage.PROPOSE);
								reply.setContent("reply");								
								myAgent.send(reply);

							}
						}
						
						
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
                	received = receive();
                }
                block();
			}
		} );
		
		addBehaviour(addServiceInSequence);
	}
		
	
	
	private ArrayList<ArtDisplay> generateTourForUserPreference(UserProfile aUser) 
	{
		ArrayList<ArtDisplay> _artifact_ID = new ArrayList<ArtDisplay>();
		
		for (ArtObject _artifact : _allArtifacts) 
		{
			String _matchingType = aUser._occupation;
			switch(_matchingType)
			{
				case "GENRE" : 
					if(aUser.isInterestedByGenre(_artifact)) 
					{
						_artifact_ID.add
						(
							new ArtDisplay(_artifact._id,_artifact._name,_artifact._creator, "NONE", this.getLocalName())
						);
					}; 
					break;
				case "ERA" : 
					if(aUser.isInterestedByGenre(_artifact)) 
					{
						_artifact_ID.add
						(
							new ArtDisplay(_artifact._id,_artifact._name,_artifact._creator, "NONE", this.getLocalName())
						);
					}; 
					break;
				case "COUNTRY" : 
					if(aUser.isInterestedByCountry(_artifact)) 
					{
						_artifact_ID.add
						(
							new ArtDisplay(_artifact._id,_artifact._name,_artifact._creator, "NONE", this.getLocalName())
						);
					}; 
					break;
				case "NARROW" :
					if(aUser.isInterestedByGenre(_artifact) && aUser.isInterestedByEra(_artifact) && aUser.isInterestedByCountry(_artifact))  
					{
						_artifact_ID.add
						(
							new ArtDisplay(_artifact._id,_artifact._name,_artifact._creator, "NONE", this.getLocalName())
						);
					}; 
					break;
				case "ALL" :
					if(aUser.isInterestedByGenre(_artifact) || aUser.isInterestedByEra(_artifact) || aUser.isInterestedByCountry(_artifact)) 
					{
						_artifact_ID.add
						(
							new ArtDisplay(_artifact._id,_artifact._name,_artifact._creator, "NONE", this.getLocalName())
						);
					}; 
					break;
			}
		}
		
		return _artifact_ID;
	}


	private ArtObject fetchArtifactDetail(int anId) 
	{
		
		for (ArtObject obj : _allArtifacts) 
		{
			if(obj._id == anId)
				return obj;
		}
		
		return null;
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
}


