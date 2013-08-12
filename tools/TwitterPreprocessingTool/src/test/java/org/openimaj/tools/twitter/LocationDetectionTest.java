package org.openimaj.tools.twitter;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.tools.twitter.SentimentExtractor.MockSentiment;
import org.openimaj.tools.twitter.modes.preprocessing.LocationDetectionMode;
import org.openimaj.tools.twitter.modes.preprocessing.SentimentExtractionMode;
import org.openimaj.tools.twitter.modes.preprocessing.TwitterPreprocessingMode;
import org.openimaj.twitter.GeneralJSONTwitter;
import org.openimaj.twitter.USMFStatus;
import org.openimaj.twitter.collection.FileTwitterStatusList;
import org.openimaj.twitter.collection.TwitterStatusList;
/**
 *
 * @author daniel
 */
public class LocationDetectionTest {
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private File fileFromStream(InputStream stream) throws IOException {
            System.out.println(stream);
            File f = folder.newFile("tweet" + stream.hashCode() + ".txt");
            PrintWriter writer = new PrintWriter(f,"UTF-8");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line = null;
            while((line = reader.readLine()) != null){
                    writer.println(line);
            }
            writer.flush(); writer.close();
            return f;
    }
    @Test
    public void testLocationDetection() throws IOException, Exception{
        File unanalysed = fileFromStream(LocationDetectionTest.class.getResourceAsStream("/org/openimaj/twitter/location_det.txt"));
        TwitterStatusList<USMFStatus> tweets = FileTwitterStatusList.readUSMF(unanalysed,"UTF-8",GeneralJSONTwitter.class);
        USMFStatus tweet = tweets.get(0);
        Map<String, Object> results = TwitterPreprocessingMode.results(tweet, new LocationDetectionMode());
    }
}