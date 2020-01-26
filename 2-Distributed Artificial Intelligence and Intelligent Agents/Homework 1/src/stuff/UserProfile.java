package stuff;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class UserProfile implements Serializable {

	public int 					_age;
	public String 				_occupation;
	public String 				_gender;
	public String				_country;
	public ArrayList<String> 	_interests;
	public int 					_eraOfInterest;
	
	private int 		_userInterestRange;
	private final int 	_baseRange = 150;
	private final int	_differRange = 120;
	
	final String[] _genres = new String[]{"PAINTING", "STATUE", "TEXT", "FOOD", "TECHNOLOGY"};
	
	public UserProfile()
	{
		this._age = (int) (15 + Math.random() * 20);
		this._occupation = "NONE";
		this._gender = Math.random() < 0.5 ? "Male" : "Female";
		
		this._interests = new ArrayList<String>();
		
		this._userInterestRange = (int) (_baseRange + Math.random() * _differRange);
		this._eraOfInterest = (int) (1000 + Math.random() * 1017);
		
		String[] _locales = Locale.getISOCountries();
		int _localeIndex = (int) (Math.random() * _locales.length);
		Locale obj = new Locale("", _locales[_localeIndex]);
		
		this._country = obj.getDisplayCountry();
		
		System.out.println("Profiler: New profile created with age " 
				+ String.valueOf(this._age) + " who is a " + this._gender
				+ " with interest in era " + String.valueOf(this._eraOfInterest));
		
		System.out.println("Profiler: Likes the genres: ");
		Random _random = new Random();
		// Only like 2 genres total
		for(int i = 0 ; i < 2; i++)
		{
			String _likedGenre = _genres[_random.nextInt(_genres.length)];
			this._interests.add(_likedGenre);
			System.out.println(_likedGenre);
		}
		
	}
	
	public boolean isInterestedByGenre(ArtObject anArtObject)
	{
			for(int i = 0 ; i < this._interests.size(); i++)
			{
				if(anArtObject._genre.equals(this._interests.get(i)))
				{
					/*System.out.println("Matched item id: " + anArtObject._id);
					System.out.println("Name: " + anArtObject._name);
					System.out.println("Creator: " + anArtObject._creator);
					System.out.println("Year Of creation: " + anArtObject._yearOfCreation);
					System.out.println("Place Of creation: " + anArtObject._placeOfCreation);
					System.out.println("Genre: " + anArtObject._genre);*/
					return true;
				}
					
			}
		
		return false;
	}
	
	public boolean isInterestedByEra(ArtObject anArtObject)
	{
		return
			anArtObject._yearOfCreation < this._eraOfInterest + this._userInterestRange && 
			anArtObject._yearOfCreation > this._eraOfInterest - this._userInterestRange;
		
	}
	public boolean isInterestedByCountry(ArtObject anArtObject)
	{
		return anArtObject._placeOfCreation.equals(this._country);
	}
}
