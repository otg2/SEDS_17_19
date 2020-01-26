package topten;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;

public class TopTen 
{
    // This helper function parses the stackoverflow into a Map for us.
    public static Map<String, String> transformXmlToMap(String xml) 
	{
		Map<String, String> map = new HashMap<String, String>();
		try {
			String[] tokens = xml.trim().substring(5, xml.trim().length() - 3).split("\"");
			for (int i = 0; i < tokens.length - 1; i += 2) {
			String key = tokens[i].trim();
			String val = tokens[i + 1];
			map.put(key.substring(0, key.length() - 1), val);
			}
		} catch (StringIndexOutOfBoundsException e) {
			System.err.println(xml);
		}

		return map;
    }

    public static class TopTenMapper extends Mapper<Object, Text, NullWritable, Text> 
	{
		// Stores a map of user reputation to the record
		TreeMap<Integer, Text> repToRecordMap = new TreeMap<Integer, Text>();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			// Create a map of strings from given funcio
			Map<String, String> dataInfo = transformXmlToMap(value.toString());
			
			//Returns true if this map contains a mapping for the specified key. 
			// https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#containsKey-java.lang.Object-
			if (dataInfo.containsKey("Id"))
			{
				
				//Get the int, based on their reputation.
				Integer reputation = Integer.valueOf(dataInfo.get("Reputation"));
				// Put to the tree map
				repToRecordMap.put(reputation, new Text(value));
				
				// If 10 records have already been added, remove the lowest 11th
				// Removes and returns a key-value mapping associated with the least key in this map, or null if the map is empty.
				// https://docs.oracle.com/javase/8/docs/api/java/util/NavigableMap.html?is-external=true#pollFirstEntry--
				if (repToRecordMap.size() > 10) 
				{
					repToRecordMap.pollFirstEntry();
				}	
			}
			else return;
		}
		
		// gets called once after all key-value pairs have been through the map function.
		protected void cleanup(Context context) throws IOException, InterruptedException 
		{
			// Write the context of the records to reducer
			for (Text topText : repToRecordMap.values())
			{
				context.write(NullWritable.get(), topText);
			}
		}
    }

    public static class TopTenReducer extends TableReducer<NullWritable, Text, NullWritable> 
	{
		// Stores a map of user reputation to the record
		private TreeMap<Integer, Text> repToRecordMap = new TreeMap<Integer, Text>();

		public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException 
		{
			// For ever value received from the mappers
			for (Text aValue : values) 
			{
				// Change the value to the format used
				Map<String, String> dataInfo = transformXmlToMap(aValue.toString());

				//Merge all received top 10 reputations from all mappers and add to record map 
				Integer reputation = Integer.valueOf(dataInfo.get("Reputation"));
				repToRecordMap.put(reputation, new Text(aValue));

				// If 10 records have already been added, remove the lowest 11th
				// Removes and returns a key-value mapping associated with the least key in this map, or null if the map is empty.
				// https://docs.oracle.com/javase/8/docs/api/java/util/NavigableMap.html?is-external=true#pollFirstEntry--
				if (repToRecordMap.size() > 10) 
				{
					repToRecordMap.pollFirstEntry();
				}
			}
			for(Map.entry<Integer,Text> entry  : repToRecordMap.entrySet())
			{
				Integer topKey = entry.getKey();
				Text topValue = entry.getValue();
				
				Put insHBASE = new Put(topValue.getBytes());
				insHBASE.addColumn(Bytes.toBytes("info"), Bytes.toBytes("rep"), Bytes.toBytes(topKey));
				context.write(NullWritable.get(), insHBase);
			}
			
		}
    }

    public static void main(String[] args) throws Exception 
	{
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "topten");
		job.setJarByClass(TopTen.class);
		
		job.setMapperClass(TopTenMapper.class);
		job.setCombinerClass(TopTenReducer.class);
		job.setReducerClass(TopTenReducer.class);
		
		// multiple reducers would shard the data and would result in multiple top ten lists.
		job.setNumReduceTasks(1);
		
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		TableMapReduceUtil.initTableReducerJob("topten", TopTenReducer.class, job);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}