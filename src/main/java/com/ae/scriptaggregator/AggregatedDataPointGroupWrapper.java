package com.ae.scriptaggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.kairosdb.core.DataPoint;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.groupby.GroupByResult;

public abstract class AggregatedDataPointGroupWrapper implements DataPointGroup
{
	protected DataPoint currentDataPoint = null;
	protected ArrayList<DataPoint> subSet = null;
	private int size = 0;
	private DataPointGroup innerDataPointGroup;


	public AggregatedDataPointGroupWrapper(DataPointGroup innerDataPointGroup)
	{
		this.innerDataPointGroup = innerDataPointGroup;

		if (innerDataPointGroup.hasNext())
			currentDataPoint = innerDataPointGroup.next();
	}

	public AggregatedDataPointGroupWrapper(DataPointGroup innerDataPointGroup, int size)
	{
		this.innerDataPointGroup = innerDataPointGroup;
		this.subSet = new ArrayList<DataPoint>();
		this.size=size;

		for(int i=0;i<size-1;i++){
			if (innerDataPointGroup.hasNext()){
				currentDataPoint = innerDataPointGroup.next();
				subSet.add(currentDataPoint);
			}
		}
		
			
		
	}
	
	@Override
	public String getName()
	{
		return (innerDataPointGroup.getName());
	}

	@Override
	public Set<String> getTagNames()
	{
		return (innerDataPointGroup.getTagNames());
	}

	@Override
	public Set<String> getTagValues(String tag)
	{
		return (innerDataPointGroup.getTagValues(tag));
	}

	@Override
	public boolean hasNext()
	{
		return currentDataPoint != null;
	}

	@Override
	public abstract DataPoint next();

	protected boolean hasNextInternal()
	{
		boolean hasNext = innerDataPointGroup.hasNext();
		if (!hasNext)
			currentDataPoint = null;
		return hasNext;
	}

	protected DataPoint nextInternal()
	{
		DataPoint dp  = innerDataPointGroup.next();
		this.subSet.add(dp);
		if(this.subSet.size()>size){
			this.subSet.remove(0);
		}
		return dp;
	}

	protected ArrayList<DataPoint> getSubSet()
	{
		return this.subSet;
	}
	
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void close()
	{
		innerDataPointGroup.close();
	}

	@Override
	public List<GroupByResult> getGroupByResult()
	{
		return innerDataPointGroup.getGroupByResult();
	}

}
