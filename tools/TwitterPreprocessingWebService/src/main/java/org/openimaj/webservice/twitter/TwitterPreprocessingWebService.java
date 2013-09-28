package org.openimaj.webservice.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openimaj.logger.LoggerUtils;
import org.restlet.Component;
import org.restlet.data.Protocol;


/**
 * 
 * @author Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 */
public class TwitterPreprocessingWebService extends Component{

	public TwitterPreprocessingWebService(String port) throws Exception {
		loadProps();
		this.getContext().getLogger().setLevel(java.util.logging.Level.ALL);
		getServers().add(Protocol.HTTP,Integer.parseInt(port));
		getDefaultHost().attach("/process", new PreProcessApp());
		getDefaultHost().attach("/job", new PreProcessJobApp());
		LoggerUtils.prepareConsoleLogger();
	}
	public void loadProps() throws IOException{
		Properties props = new Properties();
		InputStream res = TwitterPreprocessingWebService.class.getResourceAsStream("smows.properties");
		if(res==null) return;
		props.load(res);
		for (Object key : props.keySet()) {
			if (!System.getProperties().containsKey(key)) {
				System.getProperties().setProperty((String) key,(String) props.get(key));
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length == 0)
			new TwitterPreprocessingWebService("8080").start();
		else
			new TwitterPreprocessingWebService(args[0]).start();
		
	}
}