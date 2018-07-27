package com.ae.scriptaggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.aggregator.PercentileAggregator;
import org.kairosdb.core.aggregator.RangeAggregator;
import org.kairosdb.core.annotation.FeatureComponent;
import org.kairosdb.core.annotation.FeatureProperty;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.core.datapoints.LongDataPoint;
import org.kairosdb.core.datapoints.StringDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@FeatureComponent(name = "jsrange", description = "Apply a javascript function on a range of data points")
public class ScriptRangeAggregator extends RangeAggregator
{
	public static final Logger logger = LoggerFactory.getLogger(PercentileAggregator.class);
	
	private ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	private Invocable invocable = null;

	@Inject
	public ScriptRangeAggregator()
	{
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return true;//DataPoint.GROUP_NUMBER.equals(groupType);
	}

	@Override
	public String getAggregatedGroupType(String groupType)
	{
		return groupType;
	}
	
	@FeatureProperty(
			label = "script",
			description = "The script applied to each range"
	)
	private String m_script;

	public void setScript(String script)
	{
		m_script = script;
		
		try {
			engine.eval(m_script);
			invocable = (Invocable) engine;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected RangeSubAggregator getSubAggregator()
	{
		return (new ScriptRangeDataPointAggregator());
	}

	private class ScriptRangeDataPointAggregator implements RangeSubAggregator
	{

		@Override
		public Iterable<DataPoint> getNextDataPoints(long returnTime, Iterator<DataPoint> dataPointRange)
		{


			List<Object> listValues = new ArrayList<Object>();
			List<Long> listDates = new ArrayList<Long>();
			
			Object value = null;
			DataPoint dp = null;
			
			
			
			while (dataPointRange.hasNext()){
				
				DataPoint dpt = dataPointRange.next();
				
				if(!dpt.isDouble() && !dpt.isLong()) {
					StringDataPoint sdp = (StringDataPoint) dpt;
					value = sdp.getValue();
				}else if(dpt.isDouble()){
					DoubleDataPoint ddp = (DoubleDataPoint) dpt;
					value = ddp.getDoubleValue();
				}else if(dpt.isLong()){
					LongDataPoint ddp = (LongDataPoint) dpt;
					value = ddp.getLongValue();
				}
				
				
				listValues.add(value);
				listDates.add(dpt.getTimestamp());
			}
			
			try {

				ScriptObjectMirror mirror = (ScriptObjectMirror) invocable.invokeFunction("fx", listDates.toArray(),listValues.toArray());
				Object result = mirror.getMember("value");
				
				if(result instanceof Double) {
					dp = new DoubleDataPoint(Long.valueOf(mirror.getMember("timestamp").toString()), (Double) mirror.getMember("value"));
				}else if(result instanceof Integer) {
					dp = new LongDataPoint(Long.valueOf(mirror.getMember("timestamp").toString()), (Integer) mirror.getMember("value"));
				}else if(result instanceof String) {
					dp = new StringDataPoint(Long.valueOf(mirror.getMember("timestamp").toString()), (String) mirror.getMember("value"));
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return Collections.singletonList(dp);
		}
	}
}
