package com.sap.hadoop.windowing.runtime2;

import java.io.PrintStream;

import org.apache.hadoop.io.Writable;

import com.sap.hadoop.windowing.WindowingException;
import com.sap.hadoop.windowing.io.IOUtils;
import com.sap.hadoop.windowing.io.WindowingInput;
import com.sap.hadoop.windowing.query2.definition.QueryDef;
import com.sap.hadoop.windowing.query2.specification.HiveTableSpec;
import com.sap.hadoop.windowing.query2.translate.QueryTranslationInfo;

public class LocalExecutor extends Executor
{
	PrintStream out;
	
	public LocalExecutor()
	{
		this(System.out);
	}
	
	public LocalExecutor(PrintStream out)
	{
		this.out = out;
	}
	
	public void execute(QueryDef qDef, WindowingShell wShell) throws WindowingException
	{
		QueryTranslationInfo tInfo = qDef.getTranslationInfo();
		HiveTableSpec hvTblSpec = qDef.getInput().getHiveTableSpec();
		
		WindowingInput wIn = IOUtils.createTableWindowingInput(hvTblSpec.getDbName(), hvTblSpec.getTableName(), tInfo.getHiveCfg());
		//Partition p = IOUtils.createPartition(partClassName, partMemSize, wIn);
		
		PartitionsIterator partsItr = new PartitionsIterator(wIn, qDef);
		
		while(partsItr.hasNext())
		{
			Partition p = partsItr.next();
			Partition oP = executeChain(qDef, p);
			//IOUtils.dumpPartition(oP, System.out);
			executeSelectList(qDef, oP, new SysOutRS(out));
		}
	}
	
	public static class SysOutRS implements ForwardSink
	{
		PrintStream out;
		
		public SysOutRS(PrintStream out)
		{
			this.out = out;
		}
		
		public void collectOutput(Writable key, Writable value)
		{
			out.println(value);
		}

		@Override
		public void collectOutput(Object[] o)
		{
			throw new UnsupportedOperationException();
			
		}

		@Override
		public boolean acceptObject()
		{
			return false;
		}
	}
}
