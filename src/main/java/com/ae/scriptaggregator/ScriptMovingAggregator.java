package com.ae.scriptaggregator;

import java.util.ArrayList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.aggregator.Aggregator;
import org.kairosdb.core.aggregator.annotation.AggregatorName;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.kairosdb.core.datastore.DataPointGroup;

import com.ae.scriptaggregator.AggregatedDataPointGroupWrapper;

import com.google.inject.Inject;

@AggregatorName(name = "jsmoving", description = "Apply javascript function on successive data points.")
public class ScriptMovingAggregator implements Aggregator
{
	private DoubleDataPointFactory m_dataPointFactory;
	private ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	private Invocable invocable = null;
	
	private String m_function;
	
	private int m_size;

	@Inject
	public ScriptMovingAggregator(DoubleDataPointFactory dataPointFactory)
	{
		m_dataPointFactory = dataPointFactory;
	}

	@Override
	public DataPointGroup aggregate(DataPointGroup dataPointGroup)
	{
		return new JsMovingDataPointGroup(dataPointGroup);
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return DataPoint.GROUP_NUMBER.equals(groupType);
	}

	public void setSize(int size)
	{
		m_size = size;
	}
	
	public void setFunction(String function)
	{
		m_function = function;
		try {
			engine.eval(m_function);
			invocable = (Invocable) engine;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class JsMovingDataPointGroup extends AggregatedDataPointGroupWrapper
	{

		public JsMovingDataPointGroup(DataPointGroup innerDataPointGroup)
		{
			super(innerDataPointGroup,m_size);
			//ArrayList in = (ArrayList) innerDataPointGroup;
		}

		@Override
		public boolean hasNext()
		{
			return currentDataPoint != null && hasNextInternal();
		}

		@Override
		public DataPoint next()
		{

			if (hasNextInternal())
			{
				currentDataPoint = nextInternal();

			}

			DataPoint dp = m_dataPointFactory.createDataPoint(0,0);
			List<Double> listValues = new ArrayList<Double>();
			List<Long> listDates = new ArrayList<Long>();

			for(int i=0;i<subSet.size();i++){
				DataPoint dpt = subSet.get(i);
				listValues.add(dpt.getDoubleValue());
				listDates.add(dpt.getTimestamp());
			}
			
			try {
				ScriptObjectMirror mirror = (ScriptObjectMirror) invocable.invokeFunction("fx", listDates.toArray(),listValues.toArray());
				dp = m_dataPointFactory.createDataPoint(Long.valueOf(mirror.getMember("timestamp").toString()), (Double) mirror.getMember("value"));
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return dp;
		}
	}
}
