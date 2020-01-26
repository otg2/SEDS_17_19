package clone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BuyerResult implements Serializable {
	public int amount;
	public long price;
	public BuyerResult(int amount1,long price1)
	{
		amount = amount1;
		price = price1;
	}
	public String print()
	{
		return "Bought "+ this.amount +" at price "+this.price;
	}
}
