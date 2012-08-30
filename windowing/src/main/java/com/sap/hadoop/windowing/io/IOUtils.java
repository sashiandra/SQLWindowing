package com.sap.hadoop.windowing.io;

import static com.sap.hadoop.windowing.Constants.INPUT_INPUTFORMAT_CLASS;
import static com.sap.hadoop.windowing.Constants.INPUT_KEY_CLASS;
import static com.sap.hadoop.windowing.Constants.INPUT_PATH;
import static com.sap.hadoop.windowing.Constants.INPUT_SERDE_CLASS;
import static com.sap.hadoop.windowing.Constants.INPUT_VALUE_CLASS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;

import com.sap.hadoop.HiveUtils;
import com.sap.hadoop.windowing.WindowingException;

class IOUtils
{
	private static final Log LOG = LogFactory.getLog("com.sap.hadoop.windowing.io");
	
	@SuppressWarnings("unchecked")
	public static WindowingInput createTableWindowingInput(String dbName, String tableName, Configuration conf) throws WindowingException
	{
		try
		{
			HiveMetaStoreClient client = HiveUtils.getClient(conf);
			String db = HiveUtils.validateDB(client, dbName);
			Table t = HiveUtils.getTable(client, db, tableName);
			StorageDescriptor sd = t.getSd();
			
			HiveConf hConf = new HiveConf(conf, IOUtils.class);
			JobConf job = new JobConf(hConf);
			
			Class<? extends InputFormat<? extends Writable, ? extends Writable>> inputFormatClass =
				(Class<? extends InputFormat<? extends Writable, ? extends Writable>>) Class.forName(sd.getInputFormat());
			hConf.setClass("mapred.input.format.class", inputFormatClass, InputFormat.class);
			hConf.set(INPUT_INPUTFORMAT_CLASS, inputFormatClass.getName());
			InputFormat<? extends Writable, ? extends Writable> iFmt = inputFormatClass.newInstance();
			if (iFmt instanceof TextInputFormat)
			{
				((TextInputFormat)iFmt).configure(job);
			}
			FileInputFormat.addInputPath(job, new Path(sd.getLocation()));
			InputSplit[] iSplits = iFmt.getSplits(job, 1);
			org.apache.hadoop.mapred.RecordReader<Writable, Writable> rdr =
					(org.apache.hadoop.mapred.RecordReader<Writable, Writable>) iFmt.getRecordReader(iSplits[0], job, Reporter.NULL);
			
			hConf.set(INPUT_PATH, sd.getLocation());
			hConf.set(INPUT_KEY_CLASS, rdr.createKey().getClass().getName());
			hConf.set(INPUT_VALUE_CLASS, rdr.createValue().getClass().getName());
			
			hConf.set(INPUT_SERDE_CLASS, sd.getSerdeInfo().getSerializationLib());
			
			TableWindowingInput tIn = new TableWindowingInput();
			
			tIn.initialize(null, hConf, MetaStoreUtils.getSchema(t) );
			
			return tIn;
			
			
		}
		catch(WindowingException w)
		{
			throw w;
		}
		catch(Exception e)
		{
			throw new WindowingException(e);
		}
	}
	
}