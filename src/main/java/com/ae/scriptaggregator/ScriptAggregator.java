package com.ae.scriptaggregator;

import com.google.inject.Inject;

import java.util.List;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.aggregator.Aggregator;
import org.kairosdb.core.aggregator.annotation.AggregatorName;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.groupby.GroupByResult;


@AggregatorName(name = "js", description = "Apply javascript function to each data point.")
public class ScriptAggregator implements Aggregator
{
	private DoubleDataPointFactory m_dataPointFactory;

	private ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	private Invocable invocable = null;
	
	private String m_function;

	@Inject
	public ScriptAggregator (DoubleDataPointFactory dataPointFactory)
	{
		m_dataPointFactory = dataPointFactory;
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return DataPoint.GROUP_NUMBER.equals(groupType);
	}

	@Override
	public DataPointGroup aggregate(DataPointGroup dataPointGroup)
	{
		//checkState(m_function != 0.0);
		return new JsDataPointGroup(dataPointGroup);
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

	public class JsDataPointGroup implements DataPointGroup
	{
		private DataPointGroup m_innerDataPointGroup;
		
		

		public JsDataPointGroup(DataPointGroup innerDataPointGroup)
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
			
			
			try {
				
				ScriptObjectMirror mirror = (ScriptObjectMirror) invocable.invokeFunction("fx", dp.getTimestamp(),dp.getDoubleValue());
				dp = m_dataPointFactory.createDataPoint(Long.valueOf(mirror.getMember("timestamp").toString()), (Double) mirror.getMember("value"));
				
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

