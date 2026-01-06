package com.ae.scriptaggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.annotation.FeatureComponent;
import org.kairosdb.core.annotation.FeatureProperty;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.groupby.GroupByResult;
import org.kairosdb.plugin.Aggregator;

import com.google.inject.Inject;

@FeatureComponent(name = "jsmoving", description = "Apply javascript function on successive data points.")
public class ScriptMovingAggregator implements Aggregator
{
	
	private DoubleDataPointFactory m_dataPointFactory;
	private ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
	private Invocable invocable = null;
	
	@FeatureProperty(
			label = "script",
			description = "The script applied to each range"
	)
	private String m_script;
	
	@FeatureProperty(
			label = "size",
			description = "The number of values in each range"
	)
	private int m_size;

	@Inject
	public ScriptMovingAggregator(DoubleDataPointFactory dataPointFactory)
	{
		m_dataPointFactory = dataPointFactory;
	}

	@Override
	public DataPointGroup aggregate(DataPointGroup dataPointGroup)
	{
		return new ScriptMovingDataPointGroup(dataPointGroup);
	}

	@Override
	public String getAggregatedGroupType(String groupType)
	{
		return m_dataPointFactory.getGroupType();
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
	public void init()
	{

	}
	
	private class ScriptMovingDataPointGroup implements DataPointGroup
	{
		private DataPointGroup m_innerDataPointGroup;
		ArrayList<DataPoint> subSet = new ArrayList<DataPoint>();

		public ScriptMovingDataPointGroup(DataPointGroup innerDataPointGroup)
		{
			m_innerDataPointGroup = innerDataPointGroup;

			for(int i=0;i<m_size-1;i++){
				if (innerDataPointGroup.hasNext()){
					subSet.add(innerDataPointGroup.next());
				}
			}
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

			subSet.add(dp);
			if(subSet.size()>m_size){
				subSet.remove(0);
			}
			
			//DataPoint dptt = m_dataPointFactory.createDataPoint(0,0);
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

		@Override
        public String getAlias()
        {
            return m_innerDataPointGroup.getAlias();
        }
	}
}
