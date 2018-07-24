package com.ae.scriptaggregator;

import ch.qos.logback.classic.Logger;
//import com.google.inject.name.Names;
import java.util.Properties;
import org.kairosdb.core.CoreModule;
import org.slf4j.LoggerFactory;

public class ScriptAggregatorModule extends CoreModule{

	public static final Logger logger = (Logger) LoggerFactory.getLogger(ScriptAggregatorModule.class);

	
	public ScriptAggregatorModule(Properties props)
	{
		super(props);
	}
	
	@Override
	protected void configure() {
		bind(ScriptFunctionAggregator.class);
		bind(ScriptFilterAggregator.class);
		bind(ScriptMovingAggregator.class);
		bind(ScriptRangeAggregator.class);
	}
}