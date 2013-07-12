/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimaj.tools.twitter;

import java.io.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.tools.twitter.modes.preprocessing.SentimentExtractionMode;
import org.openimaj.tools.twitter.modes.preprocessing.TwitterPreprocessingMode;
import org.openimaj.twitter.GeneralJSONTwitter;
import org.openimaj.twitter.USMFStatus;
import org.openimaj.twitter.collection.FileTwitterStatusList;
import org.openimaj.twitter.collection.TwitterStatusList;

/**
 *
 * @author bill
 */
public class SentimentExtractionTest {
    
    /**
        * the output folder
        */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private File fileFromStream(InputStream stream) throws IOException {
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
    public void testSentimentExtraction() throws IOException, Exception{
        File unanalysed = fileFromStream(SentimentExtractionTest.class.getResourceAsStream("/org/openimaj/twitter/json_tweets.txt"));
        TwitterStatusList<USMFStatus> tweets = FileTwitterStatusList.readUSMF(unanalysed,"UTF-8",GeneralJSONTwitter.class);
        USMFStatus tweet = tweets.get(0);
        TwitterPreprocessingMode.results(tweet, new SentimentExtractionMode());
    }
}
