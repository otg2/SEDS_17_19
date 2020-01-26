package stuff;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Hashtable;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JMenuBar;

@SuppressWarnings("serial")
public class Chessboard extends Agent {
	private int _rows;
	private ArrayList<AID> _queens;
	private ArrayList<String> _positions;
	
	private Hashtable _table = new Hashtable();
	
	boolean _lastResize;
	
	private JFrame _frame;
	
	private JPanel _panel;
	
	String _FinalPosition;
	
	@Override
	protected void setup() 
	{

		Object[] arguments = getArguments();
		if (arguments.length > 0) _rows = Integer.parseInt( (String) arguments[0]);
		// 8 by default
		else _rows = 8;
		
		_lastResize = false;
		_FinalPosition = null;

		//_FinalPosition = "1,4_2,6_3,8_4,2_5,7_6,1_7,3_8,5";
		
		System.out.println("Starting chessboard: " +getLocalName() + " with rows " + _rows);
		
		_queens = new ArrayList<AID>();
		_positions = new ArrayList<String>();
		
		ServiceDescription sd  = new ServiceDescription();
        sd.setType( "Chessboard" );
        sd.setName( getLocalName() );
        register( sd );
        subscribe ("Queen");
		
        // Create a message receiver
 		addBehaviour(new myMsgReceiver());
		
 		createInterface(_rows);
 		
		createTable(_rows);
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
						//System.out.println("DF SEDNDI");
						//System.out.println(msg.getContent());
			            DFAgentDescription[] dfds = DFService.decodeNotification(msg.getContent());
			            if (dfds.length > 0 && msg.getContent().contains("Queen"))
			            {
			         	   for (int i=0; i<dfds.length; i++)
			         	   {	
			         		   	_queens.add(dfds[i].getName());
			         		   	
				         		/*ACLMessage inform = msg.createReply();
				         		inform.addReceiver(dfds[i].getName());
				         		inform.setPerformative(ACLMessage.INFORM);
				         		inform.setContent(String.valueOf(_rows));
				     			myAgent.send(inform);*/
			         	   }
			         	   
			            }
		            }
					catch(Exception e)
					{
						e.printStackTrace();
					}
					System.out.println("Chessboard  "+ getLocalName() +": received notify from DF");	
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
		if(_performative == ACLMessage.INFORM)
		{
			try
			{
				_FinalPosition = msg.getContent();
				createTable(_rows);
			}
			catch (Exception e)
			{
				_FinalPosition = null;
				//createTable(_rows);
				
				System.out.println("Chessboard "+ getLocalName() +": failed to receive items");
				e.printStackTrace();
			}
		}
		
	}

	/*
	 * ------------------------ INTERFACE
	 */
	
	private void createInterface(int aRow)
	{
		_frame = new JFrame();
		
		JButton _button = new JButton("Refresh image");
		_panel = new JPanel(new GridLayout(aRow, aRow));
		
		
		JMenuBar _menu = new JMenuBar();
		_menu.add(_button);
		_button.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				//_FinalPosition = "1,8_2,6_3,4_4,2_5,7_6,5_7,3_8,1";
				createTable(_rows);
				
				int _resizeAdder = _lastResize ? 1 : -1;
				_lastResize = !_lastResize;
				_frame.setSize(_frame.getWidth()+_resizeAdder, _frame.getHeight()+_resizeAdder);
				//createTable(_rows);
			}
		});
		
		JButton _rearrangeButton = new JButton("Request another solution");
		
		_menu.add(_rearrangeButton);
		_rearrangeButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				requestReArrangement();
			}
		});
		

		_frame.setJMenuBar(_menu);
		_frame.setSize((int) (50 * aRow), (int) (50 * aRow + 100));
		_frame.setVisible(true);
	}
	
	
	private void requestReArrangement()
	{
		
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setPerformative(ACLMessage.INFORM);
		msg.setContent("Rearrange");
		msg.addReceiver(_queens.get(_queens.size()-1));
 		this.send(msg);
		
	}
	
	private void createTable(int aRow) 
	{
		_panel.removeAll();
		
		JLabel[][] _theBoard = new JLabel[aRow][aRow];
		for(int y = aRow-1 ; y > -1; y--)
		{
			for(int x = 0 ; x < aRow; x++)
			{
				_theBoard[x][y] = new JLabel();
				Color _colorToSet;
				
				if (x % 2 == 0) 
				{
					int nRow = (y / aRow) % 2;
					if (nRow != 0) 
						_colorToSet = y % 2 == 0 ? Color.WHITE : Color.BLACK;
					else 
						_colorToSet = y% 2 == 0 ? Color.BLACK : Color.WHITE;
				}
				else 
					_colorToSet = y % 2 == 0 ? Color.WHITE : Color.BLACK;
				
				_theBoard[x][y].setText(String.valueOf(x+1) + "," + String.valueOf(y+1));
				_theBoard[x][y].setBackground(_colorToSet);
				_theBoard[x][y].setOpaque(true);
				_panel.add(_theBoard[x][y]);
			}
		}
		
		// _FinalPosition = "1,4_2,6_3,8_4,2_5,7_6,1_7,3_8,5";
		System.out.println("Done");
		if(_FinalPosition != null)
		{
			String[] _allPosition = _FinalPosition.split("_");
			for(int i = 0 ; i < _allPosition.length; i++)
			{
				String[] _queenPosition = _allPosition[i].split(",");
				int X = Integer.valueOf(_queenPosition[0]) -1;
				int Y = Integer.valueOf(_queenPosition[1]) -1;
				
				_theBoard[X][Y].setIcon(new ImageIcon("queen_icon.gif"));
			}
		}
		
		_frame.add(_panel);
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
	
	public void refreshQueens( String service )
	{
		DFAgentDescription dfd = new DFAgentDescription();
   		ServiceDescription sd = new ServiceDescription();
   		sd.setType( service );
		dfd.addServices(sd);
		try
		{
			DFAgentDescription[] result = DFService.search(this, dfd);
			for(int i = 0 ; i < result.length; i++)
			{
				_queens.add(result[0].getName());
			}
		}
        catch (FIPAException fe) { fe.printStackTrace(); }
	}	
}
