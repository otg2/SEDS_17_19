package stuff;

import java.io.Serializable;

public class ArtDisplay implements Serializable
{
	public int _id;
	public String _name;
	public String _artist;
	public String _tourGuideName;
	public String _curatorName;
	
	public String _tourSpecification;
	
	public ArtDisplay(int anId, String aName, String anArtist, String aTourGuide, String aCurator)
	{
		this._id = anId;
		this._name = aName;
		this._artist = anArtist;
		this._tourGuideName = aTourGuide;
		this._curatorName = aCurator;
	}
	
	
}
