package org.openimaj.tools.twitter.modes.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openimaj.twitter.USMFStatus;

/**
 *
 * @author daniel
 */
public class LocalTimeMode extends TwitterPreprocessingMode<Map<String, Object>> {

    private TwitterPreprocessingMode<Map<String, List<String>>> tokMode;
    final static String LOCATION = "geo";
    public static HashMap<String,String[]> loc = new HashMap<String,String[]>(); 
    
    public LocalTimeMode() throws IOException {
        try {
            tokMode = new TokeniseMode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(LocationDetectionMode.class.getResourceAsStream("/org/openimaj/tools/location/locations.csv"), "UTF-8"));
            String line = null;
            while((line = reader.readLine()) != null){
                    String[] lin = line.split("\\|");
                    String[] li=new String[lin.length];
                    for (int i=0;i<lin.length;i++) 
                        li[i]=lin[i].trim().toLowerCase().substring(1,lin[i].length()-1);
                    if (loc.containsKey(li[1])) {
                        boolean sw=false;
                        int pold=0,pnew=0;
                        String[] s=loc.get(li[1]);
                        if (!s[5].isEmpty()) pold=Integer.parseInt(s[5]);
                        if (!li[5].isEmpty()) pnew=Integer.parseInt(li[5]);
                        if (pold==0 && pnew==0)
                            if (s[7].isEmpty())
                                sw=true;
                        if (pold==0 && pnew>0)
                            if (!li[7].isEmpty())
                                sw=true;
                        if (pold>0 && pnew>0)
                            if (!s[7].isEmpty() && !li[7].isEmpty()) {
                                if (pnew>pold) sw=true;
                                } else {
                                if (s[7].isEmpty()) sw=true; 
                                }	    			
                        if (sw) {
                            loc.remove(s[1]);
                            loc.put(li[1],li);
                        } else loc.put(s[1],s);
	    		}
	    		else { loc.put(li[1],li);}
                        
            }
            /*
            Set set = loc.entrySet(); 
            Iterator i = set.iterator(); 
            while(i.hasNext()) { 
                Map.Entry me = (Map.Entry)i.next(); 
                System.out.print(me.getKey() + ": "); 
                String[] a=(String[])me.getValue();
                System.out.println(a[0]); 
            } 
            System.out.println(); 
            */
        }
        catch (Exception e) {
            throw new IOException("Error loading the location list", e);
        }
        
    }
    @Override
    //public Map<String, Object> process(USMFStatus twitterStatus) {
    //    throw new UnsupportedOperationException("fuck");
    //}
    public Map<String, Object> process(USMFStatus twitterStatus) {
        String locstring=twitterStatus.user.location.trim().toLowerCase();
        Map<String,Object> geo = new HashMap<String,Object>();
        String[] b=loc.get(locstring); 
        geo.put("db_link",b[0]);
        geo.put("city",b[1]);
        geo.put("db_category",b[2]);
        geo.put("lat",b[3]);
        geo.put("long",b[4]);
        geo.put("population",b[5]);
        geo.put("county",b[6]);
        geo.put("region",b[7]);
        geo.put("country",b[8]);
        

        twitterStatus.addAnalysis(LOCATION, geo);
        
        try {
            Map<String, List<String>> a = TwitterPreprocessingMode.results(twitterStatus, tokMode);
            //System.out.println(a.get(TokeniseMode.TOKENS_ALL));            
        } catch (Exception ex) {
            Logger.getLogger(LocationDetectionMode.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        System.out.println(twitterStatus.getAnalysis(LOCATION));
        return geo;        
    }

    @Override
    public String getAnalysisKey() {
        return LOCATION;
    }
}