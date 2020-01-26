package clone;

import java.io.Serializable;

public class AuctionHistory implements Serializable {

	public String 	_name;
	public int 		_amount;
	public Long		_price;
	
	public AuctionHistory(String aName, int anAmount, Long aPrice)
	{
		this._name = aName;
		this._amount = anAmount;
		this._price = aPrice;
	}
}
