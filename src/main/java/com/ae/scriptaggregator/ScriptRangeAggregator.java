package com.ae.scriptaggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.aggregator.PercentileAggregator;
import org.kairosdb.core.aggregator.RangeAggregator;
import org.kairosdb.core.aggregator.annotation.AggregatorName;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@AggregatorName(name = "jsrange", description = "Apply a javascript function on a range of data points")
public class ScriptRangeAggregator extends RangeAggregator
{
	public static final Logger logger = LoggerFactory.getLogger(PercentileAggregator.class);

	private DoubleDataPointFactory m_dataPointFactory;
	
	private ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	private Invocable invocable = null;

	@Inject
	public ScriptRangeAggregator(DoubleDataPointFactory dataPointFactory)
	{
		m_dataPointFactory = dataPointFactory;
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return DataPoint.GROUP_NUMBER.equals(groupType);
	}

	@SuppressWarnings("unused")
	private String function;

	public void setFunction(String function)
	{
		this.function = function;
		
		try {
			engine.eval(function);
			invocable = (Invocable) engine;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected RangeSubAggregator getSubAggregator()
	{
		return (new PercentileDataPointAggregator());
	}

	private class PercentileDataPointAggregator implements RangeSubAggregator
	{

		@Override
		public Iterable<DataPoint> getNextDataPoints(long returnTime, Iterator<DataPoint> dataPointRange)
		{

			DataPoint dp = m_dataPointFactory.createDataPoint(0,0);
			List<Double> listValues = new ArrayList<Double>();
			List<Long> listDates = new ArrayList<Long>();
			
			while (dataPointRange.hasNext()){
				DataPoint dpt = dataPointRange.next();
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

			return Collections.singletonList(dp);
		}
	}
}
