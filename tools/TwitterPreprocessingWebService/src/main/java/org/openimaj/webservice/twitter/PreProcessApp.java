package org.openimaj.webservice.twitter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.jws.WebService;

import org.apache.log4j.Logger;
import org.openimaj.twitter.GeneralJSON;
import org.openimaj.twitter.GeneralJSONTwitter;
import org.openimaj.twitter.USMFStatus;
import org.openimaj.twitter.collection.StreamTwitterStatusList;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.routing.Router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



@WebService(targetNamespace = "http")
public class PreProcessApp extends Application {
	private static final Gson gson = new GsonBuilder().create();
	private static Logger logger = Logger.getLogger(PreProcessApp.class);
	public static class PreProcessService extends AppTypedResource<PreProcessApp>{

		@Post
		public Representation level(Representation rep) {
			
			try {
				String data = rep.getText();
				String intype = (String) this.getRequestAttributes().get("intype");
				String outtype = (String) this.getRequestAttributes().get("outtype");
				logger.info(String.format("Input: %s, Output: %s, Data Len: %d",intype,outtype,data.length()));
				Class<? extends GeneralJSON> inputClass = getTypeClass(intype);
				Class<? extends GeneralJSON> outputClass = getTypeClass(outtype);
				Scanner dataScanner = new Scanner(data);
				InputStream is = new ByteArrayInputStream( data.getBytes("UTF-8") );
				List<USMFStatus> list = StreamTwitterStatusList.readUSMF(is, inputClass,"UTF-8");
				String[] modes = this.getQuery().getValuesArray("m");
				System.out.println(Arrays.toString(modes));
				for (USMFStatus usmfStatus : list) {
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Map<Object,Object> retMap = new HashMap<Object, Object>();
			JsonRepresentation ret = new JsonRepresentation(gson.toJson(retMap));
			return ret;
		}

		private Class<? extends GeneralJSON> getTypeClass(String intype) {
			if(intype.equals("twitter")){
				return GeneralJSONTwitter.class;
			}
			else if(intype.equals("usmf")){
				return USMFStatus.class;
			}
			return null;
		}
	}
	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/process/{intype}.{outtype}", PreProcessService.class);
		return router;
	}	
}
