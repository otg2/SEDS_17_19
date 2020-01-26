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



@SuppressWarnings("serial")
public class ProfilerAgent extends Agent{

	public JFrame _mainFrame;
	private Image _adImage;
	
	UserProfile _profile;	
	ArrayList<AID> tourGuide_country;	
	ArrayList<AID> tourGuide_era;
	ArrayList<AID> tourGuide_genre;
	ArrayList<AID> tourGuide_narrow;
	ArrayList<AID> tourGuide_all;
	ArrayList<AID> curator;	
	ArrayList<String> user_input;	
	ArrayList<ArtDisplay> _ARTIFACTS_FOUND;
	
	JLabel _adLabel;
	JLabel _adLabel_image;
	JLabel _tourInfoLabel;
	JPanel _mainPanel;
	
	int _randomAd;
	
	@SuppressWarnings("serial")
	@Override
	protected void setup() 
	{
		//super.setup();
		System.out.println("Starting new profile agent " + getLocalName());
		
		user_input = new ArrayList<String>();
		Object[] args = getArguments();
		if (args != null && args.length > 0)
		{ 
			for(int i = 0 ; i < args.length; i++)
			{
				user_input.add(((String)args[i]).toUpperCase());
			}
		}
		else 
		{
			user_input.add("ALL");
		}
		System.out.println("user_input " + user_input);
		
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Profiler" );
        sd.setName( getLocalName() );
        register( sd );
        subscribe ("TourGuide_COUNTRY");
        subscribe ("TourGuide_GENRE");
        subscribe ("TourGuide_ERA");
        subscribe ("TourGuide_NARROW");
        subscribe ("TourGuide_ALL");
        subscribe ("Curator");
		
        tourGuide_country = new ArrayList<AID>();
        tourGuide_era = new ArrayList<AID>();
        tourGuide_genre = new ArrayList<AID>();
        tourGuide_narrow = new ArrayList<AID>();
        tourGuide_all = new ArrayList<AID>();
		curator= new ArrayList<AID>();
		_ARTIFACTS_FOUND = new ArrayList<ArtDisplay>();
		
        addBehaviour(new myMsgReceiver());

		addBehaviour(new OneShotBehaviour() 
		{
			public void action() 
			{
				System.out.println("Profiler "+ getLocalName() +": creating new profile...");
				_profile = new UserProfile();
				initFrame();
			}
		} );
		
		addBehaviour(new WakerBehaviour(this, 2000) 
		{
			@SuppressWarnings("unused")
			protected void handleElapsedTimeout() 
			{
				if(_ARTIFACTS_FOUND.isEmpty())
				{					
					System.out.println("Profiler "+ getLocalName() +": collecting all the interesting art objects");
					ParallelBehaviour pb= new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
					if(user_input.contains("ALL")){
						pb.addSubBehaviour(new queryTourGuideAll());
					    pb.addSubBehaviour(new queryTourGuideCountry());
					    pb.addSubBehaviour(new queryTourGuideGenre());
					    pb.addSubBehaviour(new queryTourGuideEra());
					    pb.addSubBehaviour(new queryTourGuideNarrow());}
					    else if(user_input.contains("COUNTRY"))
						pb.addSubBehaviour(new queryTourGuideCountry());
					    else if(user_input.contains("GENRE"))
						pb.addSubBehaviour(new queryTourGuideGenre());
					    else if(user_input.contains("ERA"))
						pb.addSubBehaviour(new queryTourGuideEra());
					    else if(user_input.contains("NARROW"))
						pb.addSubBehaviour(new queryTourGuideNarrow());
					myAgent.addBehaviour(pb);
				}
			}
		} );
		
		addBehaviour(new WakerBehaviour(this, 15000) 
		{
			@SuppressWarnings("unused")
			protected void handleElapsedTimeout() 
			{
				if(_ARTIFACTS_FOUND.isEmpty())
				{					
					System.out.println("Profiler "+ getLocalName() +": no interesting art objects collected, please start your agent again with correct inputs");
					System.out.println("Profiler "+ getLocalName() +": current online TourGuides are:");
					if(tourGuide_country.size()>0)
						System.out.println("COUNTRY");
					if(tourGuide_era.size()>0)
						System.out.println("ERA");
					if(tourGuide_genre.size()>0)
						System.out.println("GENRE");
					if(tourGuide_narrow.size()>0)
						System.out.println("NARROW");
					if(tourGuide_all.size()>0)
						System.out.println("ALL");
					myAgent.doDelete();
				}
			}
		} );
		
		// Add ad refreshments
		addBehaviour(new TickerBehaviour(this, 5000)
		{ 
			protected void onTick() 
			{			
				refreshImage();
			}
		});
	}
	
	public class myMsgReceiver extends CyclicBehaviour 
	{
		public void action() {	
			
			ACLMessage msg = receive();
			
			while (msg!=null){
				if (msg.getSender().equals(getDefaultDF()))
				{
					handleNotification(myAgent, msg);
					System.out.println("Profiler "+ getLocalName() +": received notify from DF");	
				}
				else
				{
					handleTourMessage(msg);
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
            if (dfds.length > 0 && msg.getContent().contains("TourGuide_ALL")){
         	   for (int i=0; i<dfds.length; i++){	
         		   tourGuide_all.add(dfds[i].getName());
         		   if(user_input.contains("ALL")){
         			  sendRequestToTourGuide(myAgent, dfds[i].getName());}	
         	   }
         		   
            }
            if (dfds.length > 0 && msg.getContent().contains("TourGuide_COUNTRY")){
          	   for (int i=0; i<dfds.length; i++){	
          		   tourGuide_country.add(dfds[i].getName());
         		   if(user_input.contains("COUNTRY") || user_input.contains("ALL")){
         			  sendRequestToTourGuide(myAgent, dfds[i].getName());}	
          	   }
          		   
             }
            if (dfds.length > 0 && msg.getContent().contains("TourGuide_GENRE")){
          	   for (int i=0; i<dfds.length; i++){	
          		   tourGuide_genre.add(dfds[i].getName());
         		   if(user_input.contains("GENRE")|| user_input.contains("ALL")){
         			  sendRequestToTourGuide(myAgent, dfds[i].getName());}	
          	   }
          		   
             }
            if (dfds.length > 0 && msg.getContent().contains("TourGuide_ERA")){
          	   for (int i=0; i<dfds.length; i++){	
          		   tourGuide_era.add(dfds[i].getName());
         		   if(user_input.contains("ERA")|| user_input.contains("ALL")){
         			  sendRequestToTourGuide(myAgent, dfds[i].getName());}	
          	   }
          		   
             }
            if (dfds.length > 0 && msg.getContent().contains("TourGuide_NARROW")){
          	   for (int i=0; i<dfds.length; i++){	
          		   tourGuide_narrow.add(dfds[i].getName());
         		   if(user_input.contains("NARROW")|| user_input.contains("ALL")){
         			  sendRequestToTourGuide(myAgent, dfds[i].getName());}	
          	   }
          		   
             }
            if (dfds.length > 0 && msg.getContent().contains("Curator")){
         	   for (int i=0; i<dfds.length; i++){	
         		   curator.add(dfds[i].getName());
         	   }
         		   
            }
          }
          catch (Exception ex) {ex.printStackTrace();}
	}
	
	private void handleTourMessage(ACLMessage msg)
	{
		String _ontology = msg.getOntology();
		
		if(_ontology == "Profiler-TourGuide")
		{
			try 
			{
				ArrayList<ArtDisplay> _displayReceived = (ArrayList<ArtDisplay>) msg.getContentObject();
				ArrayList<ArtDisplay> _tempDisplay = new ArrayList<ArtDisplay>();
				String _localNameOfSent = _displayReceived.size() > 0 ? _displayReceived.get(0)._tourGuideName : "NOITEMS";
				if (_displayReceived.size() > 0)
				{		
					System.out.println("Profiler "+ getLocalName() +": received " + _displayReceived.size()+" art objects from TourGuide "+_displayReceived.get(0)._tourGuideName+" "+_displayReceived.get(0)._tourSpecification);
				}
				// Clear all existing artifacts from tour guide
				for(int i = 0 ; i < _ARTIFACTS_FOUND.size(); i++)
				{
					if(!_ARTIFACTS_FOUND.get(i)._tourGuideName.equals(_localNameOfSent))
					{
						_tempDisplay.add(_ARTIFACTS_FOUND.get(i));
					}
				}
				
				_tempDisplay.addAll(_displayReceived);
				
				_ARTIFACTS_FOUND = _tempDisplay ;
				System.out.println("Profiler "+ getLocalName() +": new art object size " + _ARTIFACTS_FOUND.size());
				
			} catch (UnreadableException e1) 
			{
				e1.printStackTrace();
			}
			createGuiToSelect();
			
		}
		else if (_ontology=="Profiler-Curator")
		{
			ArtObject _artObject;
			try
			{
				ArtObject _receivedObject = (ArtObject)msg.getContentObject();
					
				Frame frame = new JFrame("new frame from item " + _receivedObject._id + " | "
				+_receivedObject._name);
				
				
				String[] _data = new String[]
				{
						"Id: " + String.valueOf(_receivedObject._id),
						"Name: " + _receivedObject._name,
						"Artist: " + _receivedObject._creator,
						"Genre: " + _receivedObject._genre,
						"From: " + _receivedObject._placeOfCreation,
						"Year: " + _receivedObject._yearOfCreation,
						"Museum: " + msg.getSender().getLocalName()
				};
				JList _list = new JList(_data);
				
				frame.add(_list);
				frame.setSize(400, 400);
				frame.setLocation(450, 450);
				frame.setVisible(true);
			}
			catch (Exception e)
			{
				_artObject = null;
				
				System.out.println("Profiler "+ getLocalName() +": failed to load art object");
			}
		}
	}
	
	public class queryTourGuideAll extends OneShotBehaviour 
	{

		public void action() {
			sendRequestToTourGuides(myAgent, tourGuide_all);		
		}
	}
	
	public class queryTourGuideCountry extends OneShotBehaviour 
	{

		public void action() {
			sendRequestToTourGuides(myAgent, tourGuide_country);		
		}
	}
	
	public class queryTourGuideGenre extends OneShotBehaviour 
	{
		public void action() {
			sendRequestToTourGuides(myAgent, tourGuide_genre);		
		}
	}
	
	public class queryTourGuideEra extends OneShotBehaviour 
	{
		public void action() {
			sendRequestToTourGuides(myAgent, tourGuide_era);		
		}
	}
	
	public class queryTourGuideNarrow extends OneShotBehaviour 
	{
		public void action() {
			sendRequestToTourGuides(myAgent, tourGuide_narrow);		
		}
	}
	
	void sendRequestToTourGuides(Agent myAgent, ArrayList<AID> tourGuide)
	{
		for(int i = 0 ; i < tourGuide.size(); i++)
		{
			sendRequestToTourGuide(myAgent,tourGuide.get(i));
		}
	}
	
	void sendRequestToTourGuide(Agent myAgent, AID tourGuide)
	{
			ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
			
			try 
			{
				msg.setContentObject(_profile);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			msg.addReceiver(tourGuide);
			msg.setLanguage("ENGLISH");
			msg.setOntology("Profiler-TourGuide");
							
			SenderBehaviour sb = new SenderBehaviour(myAgent, msg);
			sb.action();
	}
	@Override
	protected void takeDown()
	{	
		if(_mainFrame != null)
			_mainFrame.dispose();
		
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
	
	private void initFrame()
	{
		_mainFrame = new JFrame();
		
		// Add the whole panel
		createGuiToSelect();
		
		_mainFrame.pack();
		_mainFrame.setSize(1000, 800);
		_mainFrame.setLocation(200,0);
		
		_mainFrame.setVisible(true);
		
		_mainFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				doDelete(); // Delete the agent
				takeDown(); // Clean up operations
				_mainFrame.dispose();
			}
		});
	}
	
	private void createGuiToSelect() 
	{
		_mainPanel = new JPanel();
		
		_mainPanel.removeAll();
		_tourInfoLabel = new JLabel("Select a tour");
		_mainPanel.add(_tourInfoLabel);
		_mainFrame.add(_mainPanel);
		
		// Add all tour guides
		createTourInfo();
		
		// Ad display
		JPanel _adPanel = new JPanel();
		
		String[] _data = new String[]
		{
				"Age: " + String.valueOf(_profile._age),
				"Gender: " + _profile._gender,
				"Country: " + _profile._country,
				"Interests: " + _profile._interests.get(0) + " and " + _profile._interests.get(1) ,
				"Era: " + String.valueOf(_profile._eraOfInterest)
		};
		JList _list = new JList(_data);
		
		_adLabel = new JLabel("Place your ad here");
		_adPanel.add(_list);
		
		_mainFrame.add(_adPanel, BorderLayout.SOUTH);
		
		
		_adLabel_image = new JLabel();
		// Set the first ad
		_randomAd = 3;//(int) (Math.random() * 11);
		
		refreshImage();
		_adPanel.add(_adLabel_image);
	}
	
	private void createTourInfo()
	{
		if(_ARTIFACTS_FOUND.size() > 0)
		{
			
			String[] _data = new String[_ARTIFACTS_FOUND.size()];
			for(int i = 0; i < _data.length; i++)
			{
				String _infoString = 
						_ARTIFACTS_FOUND.get(i)._tourGuideName + 
						" (" + _ARTIFACTS_FOUND.get(i)._tourSpecification + ") - " + 
						String.valueOf(_ARTIFACTS_FOUND.get(i)._id) + " | " + 
						_ARTIFACTS_FOUND.get(i)._name + " | " + 
						_ARTIFACTS_FOUND.get(i)._artist;
				_data[i] = _infoString;
			}
			
			JList _list = new JList(_data);
			
			_list.addMouseListener(new MouseAdapter() 
			{
			    public void mouseClicked(MouseEvent evt) 
			    {
			        JList list = (JList)evt.getSource();
			        if (evt.getClickCount() == 2) 
			        {
			            // Double-click detected
			           int index = list.locationToIndex(evt.getPoint());
			           requestItemInformation(index);
			           
			        } 
			    }
			});
			
			_mainFrame.add(_list);
		}
		
	}
    
	private void requestItemInformation(final int anIndex)
	{
		addBehaviour(new OneShotBehaviour() 
		{
			public void action() 
			{
				ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
				String _curatorsLocalName = "";
				try 
				{
					int _artIdRequest = _ARTIFACTS_FOUND.get(anIndex)._id;
					_curatorsLocalName = _ARTIFACTS_FOUND.get(anIndex)._curatorName;
					System.out.println("id " + _artIdRequest + " curator " + _curatorsLocalName);
					msg.setContentObject(_artIdRequest);	
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				
				AID _foundCurator = null;
				
				for(int i = 0 ; i < curator.size(); i++)
					if(curator.get(i).getLocalName().equals(_curatorsLocalName))
						_foundCurator = curator.get(i);
				
				msg.addReceiver(_foundCurator);
				msg.setLanguage("ENGLISH");
				msg.setOntology("Profiler-Curator");
				
				SenderBehaviour sb = new SenderBehaviour(myAgent, msg);
				sb.action();
			}
		} );   
	}
	
	private void refreshImage()
	{
		int _newRandomAd = (int) (Math.random() * 11);
		
		if(_randomAd == _newRandomAd)
		{
			if (_newRandomAd == 0)
			{
				_newRandomAd = 1;
			}
			else if (_newRandomAd == 11)
			{
				_newRandomAd = 10;
			}
			else _randomAd = _newRandomAd = _newRandomAd+1;
		}
		_randomAd = _newRandomAd;
		String path = "images/ad"+String.valueOf(_randomAd) +".JPG";
		
        File file = new File(path);
        try	
        {	
        	_adLabel_image.removeAll();
        	_adLabel_image.revalidate();
        	BufferedImage image = ImageIO.read(file);
        	_adLabel_image.setIcon(new ImageIcon(image));
        	_adLabel_image.revalidate();
        	_adLabel_image.repaint();
        }
        catch(Exception e)
        {
        }
	}	
}























