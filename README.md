# ScriptAggregator Module

ScriptAggregator is a module to extend KairosDB aggregators using Javascript language (Java 8 Nashorn engine).

Four aggregators were added :

- `jsfunction`
- `jsfilter`
- `jsrange`
- `jsmoving`


## Installation

Java 8 and KairosDB are required

1. Add the jar in KairosDB lib directory
2. Add the properties file in KairosDB conf directory
3. Restart KairosDB


## Javascript aggregators

### jsfunction Aggregator

Applies a JS function to each value.

```json
{
	"name": "jsfunction",
	"script": "..."
}
```

```javascript
/**
 * @param timestamp {Long} Timestamp (Unix milliseconds) of the data point
 * @param value {Double} Value of the datapoint
 * @return {object} Resulting data point {"timestamp":Long,"value":Double}
**/
function fx(timestamp,value){
        var result = {};

		//Simple formula
        result.timestamp=timestamp;
        result.value = value*6.25-25;

        //Offset on a specific data range
        /*if(timestamp<(new Date().getTime()-1000*60*60*8) && timestamp>(new Date().getTime()-1000*60*60*20) ){
            result.timestamp=timestamp;
            result.value = value+4;
        }else{
            result.timestamp=timestamp;
            result.value = value;
        }*/
        return result;
}
```

### jsfilter Aggregator

Applies a JS function to each value which is a predicate returning a boolean. 

```json
{
	"name": "jsfilter",
	"script": "..."
}
```

```javascript
/**
 * @param timestamp {Long} Timestamp (Unix milliseconds) of the data point
 * @param value {Double} Value of the datapoint
 * @return {Boolean}
**/
function fx(timestamp,value){
    //Filter data with threshold
    /*if(value>10){
            return true;
    }else{
            return false;
    }*/

    //Filter ldata on a specific range
    if(timestamp<(new Date().getTime()-1000*60*60*8) && timestamp>(new Date().getTime()-1000*60*60*20)){
            return true;
    }else{
            return false;
    }
}
```

### jsrange Aggregator

Applies a JS function to each data group defined by a sampling period. 

```json
{
	"name": "jsrange",
	"script": "...",
	"sampling": {
		"value": "1",
		"unit": "hours"
	}
}
```

```javascript
/**
 * @param timestamps {Array} Array of timestamps (Unix milliseconds) of the data points
 * @param values {Array} Array of values of data points
 * @return {Object} Resulting data point {"timestamp":Long,"value":Double}
**/
function fx(timestamps,values){
        
        //Calculate standard deviation
        var result = {
	        "timestamp":timestamps[0];
	        "value":getStandardDeviation(values,2);
        };
        return result;
}

var getNumWithSetDec = function( num, numOfDec ){
        var pow10s = Math.pow( 10, numOfDec || 0 );
        return ( numOfDec ) ? Math.round( pow10s * num ) / pow10s : num;
}

var getAverageFromNumArr = function( numArr, numOfDec ){
        var i = numArr.length,
                sum = 0;
        while( i-- ){
                sum += numArr[ i ];
        }
        return getNumWithSetDec( (sum / numArr.length ), numOfDec );
}

var getVariance = function( numArr, numOfDec ){
        var avg = getAverageFromNumArr( numArr, numOfDec ),
                i = numArr.length,
                v = 0;

        while( i-- ){
                v += Math.pow( (numArr[ i ] - avg), 2 );
        }
        v /= numArr.length;
        return getNumWithSetDec( v, numOfDec );
}

var getStandardDeviation = function( numArr, numOfDec ){
        var stdDev = Math.sqrt( getVariance( numArr, numOfDec ) );
        return getNumWithSetDec( stdDev, numOfDec );
}
```

### jsmoving Aggregator

Applies a JS function to each data group defined by a sample size.

Instead of extracting subsets delimited by time (like `jsrange`), subsets are a number of consecutive datapoints (set by a `size` parameter). Then the subset is modified by "shifting forward"; that is, excluding the first number of the series and including the next number following the original subset in the series. This is usefull to calculate trendlines or forecasts (like moving average,...)

```json
{
	"name": "jsmoving",
	"size": 10,
	"script": "..."
}
```

```javascript
/**
 * @param timestamps {Array} Timestamp (Unix milliseconds) of the data point
 * @param values {Array} Value of the datapoint
 * @return {Object} Resulting data point {"timestamp":Long,"value":Double}
**/
function fx(timestamps,values){
    //Calculate moving average
    var count=0;
    var sum=0;
    for(var i=0;i<values.length;i++){
        sum=sum+values[i];
        count++;
    }
    var result = {
    	"timestamp":timestamps[values.length-1];
    	"value":sum/count;
    };
    return result;
}
```