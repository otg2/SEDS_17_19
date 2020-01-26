package stuff;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

public class ArtObject implements Serializable {

	public int 		_id;
	public String 	_name;
	public String 	_creator;
	public int 		_yearOfCreation;
	public String 	_placeOfCreation;
	public String 	_genre;
	
	final String[] _genres = new String[]{"PAINTING", "STATUE", "TEXT", "FOOD", "TECHNOLOGY"};
	
	
	public ArtObject(MarkovChain aNameGenerator)
	{
		this._id = (int) (1000000 + Math.random() *100000);
		this._name = aNameGenerator.generateSentence_Artwork(); //TODO: Create random sequences of chars
		this._creator = aNameGenerator.generateSentence_Artist(); //TODO: Create random sequences of chars
		
		this._yearOfCreation = (int) (1000 + Math.random() * 1017);
		
		String[] _locales = Locale.getISOCountries();
		int _localeIndex = (int) (Math.random() * _locales.length);
		Locale obj = new Locale("", _locales[_localeIndex]);
		
		this._placeOfCreation = obj.getDisplayCountry();
		
		int _genreIndex = (int) (Math.random() * _genres.length);
		this._genre = _genres[_genreIndex];
		
	}
	
	
	
}
