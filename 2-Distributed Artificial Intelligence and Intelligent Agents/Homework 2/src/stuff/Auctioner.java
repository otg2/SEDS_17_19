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

public class Auctioner extends Agent {

	private TickerBehaviour _auctionTicker = null;
	private double 		_decreaseFactor;
	private long 		_originalPrice;
	private long 		_minimalPrice;
	private long 		_currentPrice;
	private int 		_originalAmount;
	private int 		_currentAmount;
	private int			_auctionCount;
	
	private int 		_itemsBoughtLastRound;
	private boolean		_smartAuctioner;
	
	private int			_selectedIndex;
	private String		_nameOfItem;
	
	Panel 				_itemPanel;
	TextField 			_amountField;
	TextField 			_priceField; 
	TextField 			_nameField;
	
	ArrayList<AID> 				_buyers;	
	ArrayList<AuctionHistory> 	_history;
	
	String[] _data = new String[]
			{
				"Classic pizza - 11 - 9000 - UNSOLD" ,
				"Dragon skin - 5 - 14000 - UNSOLD" ,
				"Old books - 60 - 1500 - UNSOLD" ,
				"Mona Lisa - 1 - 18000 - UNSOLD" 
			};
	
	@Override
	protected void setup()
	{
		

		// Whatever, just write anything
		Object[] args = getArguments();
		if (args != null && args.length > 0)
		{ 
			_smartAuctioner = false;
		}
		else 
		{
			_smartAuctioner = true;
		}
		
		System.out.println("Starting new " + (_smartAuctioner? "smart" : "dumb") +" auctioner agent " + getLocalName());
		
		_selectedIndex = -1;
		_buyers = new ArrayList<AID>();
		_history = new ArrayList<AuctionHistory>();
		
		// register and subscribe
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Auctioner" );
        sd.setName( getLocalName() );
        register( sd );
        subscribe ("Buyer");
		
		_decreaseFactor = 0.95;
        
		
		// Create a message receiver
		addBehaviour(new myMsgReceiver());
		
		createInterface();
		
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
					
					if(_smartAuctioner)
					{
						// change the decreasing factor based on number of items sold
						double _maxAdder = 0.095; 		// make sure we always lower but just by a little
						// Ranges from (0 to 1) * 0.095. 
						// Divide items bought last round with the storage of items had
						double _variation  = (
								(
									(double)_itemsBoughtLastRound
									/
									(double)(_currentAmount+_itemsBoughtLastRound))
								) * _maxAdder;
						
						// Reduce the decrease factor 
						_decreaseFactor = 0.9 + _variation;			
						System.out.println("Auctioner: Decrease variation: " + _variation);
						System.out.println("Auctioner: Current decrease factor: " + _decreaseFactor);
						// Change the minimal price
						Long _oldMinimalPrice = _minimalPrice;
						_minimalPrice *= (1 - _variation);
						System.out.println("Auctioner: Reserved price from : " + _oldMinimalPrice + " to " + _minimalPrice);
					}
					
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
				
				
				
				System.out.println("Auctioner: sending auction bidding price: " + _currentPrice + " with " + _currentAmount + " items left ");
			}
		};
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
					
					System.out.println("Auctioner: Buyer "+ msg.getSender().getLocalName() +" bought " 
										+ _amountToBuy + " items for " + _totalAmount);
					
					_history.add(new AuctionHistory(msg.getSender().getLocalName(),_amountToBuy,_currentPrice));
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
		System.out.println("Auctioner: Auction terminated with " + _currentAmount + " items left at the price of " + _currentPrice + " with " + _auctionCount + " steps");
		for(AuctionHistory hist: _history)
		{
			System.out.println("Buyer " + hist._name + " bought " + hist._amount + " items for the price of " + hist._price);
		}
		
		// Edit the list
		if(_selectedIndex != -1)
		{
			String[] _itemText_data = _data[_selectedIndex].split("-");
			String _manyLeft = _currentAmount == 0 ? "SOLD" : "UNSOLD";
			_data[_selectedIndex] = _itemText_data[0] + " - " + String.valueOf(_currentAmount) + " - " +String.valueOf(_currentPrice) + " - " + _manyLeft;
			//System.out.println("reset list");
			setItemPanel();
		}
	}
	
	private void startNewAuction(final int anAmount, final Long anAuctionPrice)
	{
		addBehaviour(new OneShotBehaviour() 
		{
			public void action() 
			{
				// make sure we wont reference wrong items in list if it is changed
				if(!_nameField.getText().trim().equals(_nameOfItem))
				{
					_selectedIndex = -1;
					System.out.println("Auctioner: user changed after selection");
				}
				
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
				
				System.out.println("Auctioner: New Auction started");
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
	
	private void setInterfaceText(int anIndex)
	{
		if(anIndex != -1)
		{
			String[] _itemText_data = _data[anIndex].split("-");
			
			if(_itemText_data[3].trim() != "SOLD")
			{
				_nameOfItem = _itemText_data[0].trim();
				_nameField.setText(_itemText_data[0].trim());
				_amountField.setText(_itemText_data[1].trim());
				_priceField.setText(_itemText_data[2].trim());
			}
		}
		else
		{
			_nameOfItem = "";
			_amountField.setText("");
			_priceField.setText("");
			_nameField.setText("");
		}
		_selectedIndex = anIndex;
	}

	
	private void setItemPanel()
	{
		_itemPanel.removeAll();
		// Add the list
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
		           setInterfaceText(index);
		        } 
		        if (evt.getClickCount() == 3) 
		        {
		            // Double-click detected
		           setInterfaceText(-1);
		        } 
		    }
		});
		_itemPanel.add(_list);
	}
			
	private void createInterface()
	{
		final Frame _mainFrame = new Frame("Auction stuff");
		_itemPanel = new Panel();
		setItemPanel();
		
		Panel _mainPanel = new Panel();
		_mainFrame.add(_itemPanel);
		
		// Add the starting auction gui
		JLabel label_name = new JLabel("Name:");
		_nameField = new TextField();
		_nameField.setColumns(15);
		
		JLabel label_amount = new JLabel("Amount:");
		_amountField = new TextField();
		_amountField.setColumns(15);
		
		JLabel label_price = new JLabel("Price:");
		_priceField = new TextField();
		_priceField.setColumns(15);

		Button _button = new Button("Start a new auction");
		_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg) 
			{
				try 
				{
					int _auctionAmount = Integer.parseInt(_amountField.getText());
					Long _auctionPrice = Long.parseLong(_priceField.getText());
					startNewAuction(_auctionAmount, _auctionPrice);
					
					System.out.println("Auctioner: Start auction for " + _nameField.getText() + " price: " + _auctionPrice);
				}
				catch (NumberFormatException e) 
				{
					System.out.println("Auctioner: Please enter a correct price");
					e.printStackTrace();
				}
			}
		});

		_mainPanel.add(label_name);
		_mainPanel.add(_nameField);
		
		_mainPanel.add(label_amount);
		_mainPanel.add(_amountField);
		_mainPanel.add(label_price);
		_mainPanel.add(_priceField);
		_mainPanel.add(_button);
		_mainFrame.add(_mainPanel, BorderLayout.SOUTH);
		_mainFrame.setSize(800, 300);
		_mainFrame.setLocation(200,0);
		//_mainFrame.pack();
		_mainFrame.setVisible(true);
		_mainFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				doDelete();
				_mainFrame.dispose();
			}
		});
	}
	
}
