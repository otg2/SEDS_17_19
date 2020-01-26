package clone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CfpContent implements Serializable {
	public int amount;
	public long price;
	public Object object;
	public CfpContent(int amount1,long price1, Object object1)
	{
		amount = amount1;
		price = price1;
		object = object1;
	}
}
