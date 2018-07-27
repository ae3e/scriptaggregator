package com.ae.scriptaggregator;

import java.util.List;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.annotation.FeatureComponent;
import org.kairosdb.core.annotation.FeatureProperty;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.core.datapoints.LongDataPoint;
import org.kairosdb.core.datapoints.StringDataPoint;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.groupby.GroupByResult;
import org.kairosdb.plugin.Aggregator;

import com.google.inject.Inject;

import jdk.nashorn.api.scripting.ScriptObjectMirror;


@FeatureComponent(name = "jsfunction", description = "Apply javascript function to each data point.")
public class ScriptFunctionAggregator implements Aggregator
{
	

	private ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	private Invocable invocable = null;
	
	@FeatureProperty(
			label = "script",
			description = "The script applied to each value"
	)
	private String m_script;

	@Inject
	public ScriptFunctionAggregator ()
	{
	}

	@Override
	public String getAggregatedGroupType(String groupType)
	{
		return groupType;
	}
	
	@Override
	public boolean canAggregate(String groupType)
	{
		return true;//DataPoint.GROUP_NUMBER.equals(groupType);
	}

	@Override
	public DataPointGroup aggregate(DataPointGroup dataPointGroup)
	{
		//checkState(m_function != 0.0);
		return new ScriptSDataPointGroup(dataPointGroup);
	}

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

	public class ScriptSDataPointGroup implements DataPointGroup
	{
		private DataPointGroup m_innerDataPointGroup;
		
		

		public ScriptSDataPointGroup(DataPointGroup innerDataPointGroup)
		{
			m_innerDataPointGroup = innerDataPointGroup;
			
		}

		@Override
		public boolean hasNext()
		{
			return (m_innerDataPointGroup.hasNext());
		}

		@Override
		public DataPoint next()
		{
			DataPoint dp = m_innerDataPointGroup.next();
			
			Object value = null;
			
			try {

				if(!dp.isDouble() && !dp.isLong()) {
					StringDataPoint sdp = (StringDataPoint) dp;
					value = sdp.getValue();
				}else if(dp.isDouble()){
					DoubleDataPoint ddp = (DoubleDataPoint) dp;
					value = ddp.getDoubleValue();
				}else if(dp.isLong()){
					LongDataPoint ddp = (LongDataPoint) dp;
					value = ddp.getLongValue();
				}

				ScriptObjectMirror mirror = (ScriptObjectMirror) invocable.invokeFunction("fx", dp.getTimestamp(),value);
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

			return (dp);
		}

		@Override
		public void remove()
		{
			m_innerDataPointGroup.remove();
		}

		@Override
		public String getName()
		{
			return (m_innerDataPointGroup.getName());
		}

		@Override
		public List<GroupByResult> getGroupByResult()
		{
			return (m_innerDataPointGroup.getGroupByResult());
		}


		@Override
		public void close()
		{
			m_innerDataPointGroup.close();
		}

		@Override
		public Set<String> getTagNames()
		{
			return (m_innerDataPointGroup.getTagNames());
		}

		@Override
		public Set<String> getTagValues(String tag)
		{
			return (m_innerDataPointGroup.getTagValues(tag));
		}
	}
}

