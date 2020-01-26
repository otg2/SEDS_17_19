package stuff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MarkovChain {

	// Hashmap
	public static Hashtable<String, Vector<String>> markovChain_Artwork = new Hashtable<String, Vector<String>>();
	public static Hashtable<String, Vector<String>> markovChain_Artist = new Hashtable<String, Vector<String>>();
	static Random rnd = new Random();
	
	private String _fileName = "resource/Artworks.csv";
	static int _maxLearn = 200;
	List<String> lines = null;
	/*
	 * Main constructor
	 */
	public MarkovChain() 
	{
		// TODO: remove " if first letter
		// 			remove ( and )
		
		
		// Create the first two entries (k:_start, k:_end)
		markovChain_Artwork.put("_start", new Vector<String>());
		markovChain_Artwork.put("_end", new Vector<String>());
		
		markovChain_Artist.put("_start", new Vector<String>());
		markovChain_Artist.put("_end", new Vector<String>());
		
		try 
		{
			
			lines = Files.readAllLines(Paths.get(_fileName),
			        StandardCharsets.UTF_8);
			
			//System.out.println("Found " + lines.size() + " lines");
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("File can't be opened.");
		}
		
		for(int i = 0 ; i < _maxLearn; i++)
		{
			int _randomIndex = (int) (lines.size() * Math.random());
			
		    String[] _splitString = lines.get(_randomIndex).split(",");
		    try
		    {
			    addWords(_splitString[0] + "^" ,markovChain_Artwork);
			    addWords(_splitString[1] + "^" ,markovChain_Artist);
		    }
		    catch(Exception e)
		    {
		    	//System.out.println("Failed at creating index " + i);
		    }
		}
	}
	
	/*
	 * Add words
	 */
	public static void addWords(String phrase, Hashtable<String, Vector<String>> aHashTable) {
		// put each word into an array
		String[] words = phrase.split(" ");
		if(words.length < 2) return;		
		// Loop through each word, check if it's already added
		// if its added, then get the suffix vector and add the word
		// if it hasn't been added then add the word to the list
		// if its the first or last word then select the _start / _end key
		
		for (int i=0; i<words.length; i++) 
		{
						
			// Add the start and end words to their own
			if (i == 0) 
			{
				Vector<String> startWords = aHashTable.get("_start");
				startWords.add(words[i]);
				
				Vector<String> suffix = aHashTable.get(words[i]);
				if (suffix == null) 
				{
					suffix = new Vector<String>();
					suffix.add(words[i+1]);
					aHashTable.put(words[i], suffix);
				}
				
			} 
			else if (i == words.length-1) 
			{
				Vector<String> endWords = aHashTable.get("_end");
				endWords.add(words[i]);
				
			} 
			else 
			{	
				Vector<String> suffix = aHashTable.get(words[i]);
				if (suffix == null) 
				{
					suffix = new Vector<String>();
					suffix.add(words[i+1]);
					aHashTable.put(words[i], suffix);
				} 
				else 
				{
					suffix.add(words[i+1]);
					aHashTable.put(words[i], suffix);
				}
			}
		}		
	}
	
	public String generateSentence_Artwork() {
		
		return generateSentence(markovChain_Artwork);
	}
	
	public String generateSentence_Artist()
	{
		return generateSentence(markovChain_Artist);
	}
	
	/*
	 * Generate a markov phrase
	 */
	public String generateSentence(Hashtable<String, Vector<String>> aHashTable) {
		
		// Vector to hold the phrase
		Vector<String> newPhrase = new Vector<String>();
		
		// String for the next word
		String nextWord = "";
				
		// Select the first word
		Vector<String> startWords = aHashTable.get("_start");
		int startWordsLen = startWords.size();
		nextWord = startWords.get(rnd.nextInt(startWordsLen));
		newPhrase.add(nextWord);
		
		try
		{
			// Keep looping through the words until we've reached the end
			while (nextWord.charAt(nextWord.length()-1) != '^') 
			{
				Vector<String> wordSelection = aHashTable.get(nextWord);
				int wordSelectionLen = wordSelection.size();
				nextWord = wordSelection.get(rnd.nextInt(wordSelectionLen)).trim();
				newPhrase.add(nextWord);
			}
		}
		catch(Exception e)
		{
			String _tryAgain = generateSentence(aHashTable); 
			newPhrase.add(_tryAgain);
		}
		
		String _returnString = newPhrase.toString();
		_returnString = _returnString.substring(1, _returnString.length() -2); // remove [ and ^]
		
		String _finalString = "";
        String[] _splitBySpace = _returnString.split("[,\\,\\(\\)]");
		for(int i = 0 ; i < _splitBySpace.length ; i++)
		{
			if(_splitBySpace[i].trim() != "")
				_finalString += _splitBySpace[i].replace("\"", "").trim() + " ";
		}
		
		return _finalString.trim();//_returnString.substring(1, _returnString.length() -2); // remove [ and ^]	
	}
		
}