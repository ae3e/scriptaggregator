package com.ae.scriptaggregator;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.annotation.FeatureComponent;
import org.kairosdb.core.annotation.FeatureProperty;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.groupby.GroupByResult;
import org.kairosdb.plugin.Aggregator;

import com.google.inject.Inject;


@FeatureComponent(name = "jsfilter", description = "Apply javascript function to each data point.")
public class ScriptFilterAggregator implements Aggregator
{
	
	
	@SuppressWarnings("unused")
	private DoubleDataPointFactory m_dataPointFactory;

	private ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	private Invocable invocable = null;
	
	@FeatureProperty(
			label = "script",
			description = "The script applied to each function"
	)
	private String m_script;

	@Inject
	public ScriptFilterAggregator (DoubleDataPointFactory dataPointFactory)
	{
		m_dataPointFactory = dataPointFactory;
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return DataPoint.GROUP_NUMBER.equals(groupType);
	}

	@Override
	public String getAggregatedGroupType(String groupType)
	{
		return m_dataPointFactory.getGroupType();
	}
	
	@Override
	public DataPointGroup aggregate(DataPointGroup dataPointGroup)
	{
		checkNotNull(dataPointGroup);		
		return new ScriptFilterDataPointGroup(dataPointGroup);
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

	private class ScriptFilterDataPointGroup  implements DataPointGroup
	{
		private DataPointGroup m_innerDataPointGroup;
		private DataPoint nextPoint;
		
		public ScriptFilterDataPointGroup(DataPointGroup innerDataPointGroup)
		{
			m_innerDataPointGroup = innerDataPointGroup;
			nextPoint = null;
		}
		
		@Override
		public boolean hasNext()
		{
			if(nextPoint != null)
				return true;
			while(m_innerDataPointGroup.hasNext()){
				nextPoint = m_innerDataPointGroup.next();
				if(invokeFunction(nextPoint))
					break;
				else
					nextPoint=null;
			}
			return nextPoint!=null;
		}
		
		@Override
		public DataPoint next()
		{
			hasNext();
			DataPoint dp = nextPoint;
			nextPoint = null;
			return dp;
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
		
		private boolean invokeFunction(DataPoint dp) {
			boolean returnVal =  false;
			try {
				returnVal = (Boolean) invocable.invokeFunction("fx", dp.getTimestamp(), dp.getDoubleValue());
			} catch (Exception e) {
				e.printStackTrace();

			}
			return returnVal;
		}
	}
}
