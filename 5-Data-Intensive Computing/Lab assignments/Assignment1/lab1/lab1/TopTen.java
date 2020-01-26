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
			// Create a map of parameter to value of user record
			Map<String, String> dataInfo = transformXmlToMap(value.toString());
			
			//Returns true if this map contains a mapping for the specified key. 
			if (dataInfo.containsKey("Id"))
			{
				Integer reputation = Integer.valueOf(dataInfo.get("Reputation"));
				repToRecordMap.put(reputation, new Text(value));

				
				// If 10 records have already been added, remove the lowest 11th
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
			for (Map.Entry<Integer,Text> entry  : repToRecordMap.entrySet())
			{
				context.write(NullWritable.get(), entry.getValue());
			}
		}
    }

    public static class TopTenReducer extends TableReducer<NullWritable, Text, NullWritable> 
	{
		// Stores a map of user reputation to id
		private TreeMap<Integer, Text> repToRecordMap = new TreeMap<Integer, Text>();

		public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException 
		{
		    try{
                        // For each record received from the mappers
			for (Text aValue : values) 
			{
                                // Create a map of parameter to value of user record
				Map<String, String> dataInfo = transformXmlToMap(aValue.toString());

				//Merge received user records from all mappers and add to ordered map 
				Integer reputation = Integer.valueOf(dataInfo.get("Reputation"));
				Text id = new Text(dataInfo.get("Id"));
				repToRecordMap.put(reputation, id);

				// If 10 records have already been added, remove the lowest 11th
				if (repToRecordMap.size() > 10) 
				{
					repToRecordMap.pollFirstEntry();
				}
			}
                        Integer i = 0;
                        System.out.println("Top 10 reputation:");
			for(Map.Entry<Integer,Text> entry  : repToRecordMap.descendingMap().entrySet())
			{
				Integer topKey = entry.getKey();
				Text topValue = entry.getValue();

                                Text textKey = new Text(topKey.toString());
                                Text textRow = new Text(i.toString());
				System.out.println(entry.getKey());
				Put insHBASE = new Put(textRow.getBytes());
				insHBASE.addColumn(Bytes.toBytes("info"), Bytes.toBytes("rep"), textKey.getBytes());
				insHBASE.addColumn(Bytes.toBytes("info"), Bytes.toBytes("id"), topValue.getBytes());
				context.write(key, insHBASE);
                                i++;
			}
                   }catch (Exception e) {
                        e.printStackTrace();
                    }
			
		}
    }

    public static void main(String[] args) throws Exception 
	{
		Configuration conf = HBaseConfiguration.create();
		Job job = Job.getInstance(conf, "topten");
		job.setJarByClass(TopTen.class);
		
		job.setMapperClass(TopTenMapper.class);
		
		// multiple reducers would shard the data and would result in multiple top ten lists.
		job.setNumReduceTasks(1);
		
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		TableMapReduceUtil.initTableReducerJob("topten", TopTenReducer.class, job);

		System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
