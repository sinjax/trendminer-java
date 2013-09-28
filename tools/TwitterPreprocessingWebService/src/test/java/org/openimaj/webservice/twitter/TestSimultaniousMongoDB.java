package org.openimaj.webservice.twitter;

import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.openimaj.util.parallel.GlobalExecutorPool;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

public class TestSimultaniousMongoDB {
	static class WriteWait implements Runnable{
		
		private DB mdb;
		public WriteWait() throws UnknownHostException {
			MongoClient mdb = new MongoClient("localhost");
			this.mdb = mdb.getDB("testdb");
			this.mdb.dropDatabase();
			
		}
		@Override
		public void run() {
			DBCollection col = mdb.getCollection("testcol");
			col.ensureIndex("count");
			int count = 0;
			while(true){
				BasicDBObject basicDBObject = new BasicDBObject("cheese",new Random().nextDouble());
				basicDBObject.put("count", count++);
				col.insert(basicDBObject);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	static class ReadFast implements Runnable{
		
		private DB mdb;
		public ReadFast() throws UnknownHostException {
			MongoClient mdb = new MongoClient("localhost");
			this.mdb = mdb.getDB("testdb");
			
		}
		@Override
		public void run() {
			DBCollection col = mdb.getCollection("testcol");
			int seen = -1;
			while(true){
				DBCursor all = col.find(greaterThan(seen)).sort(new BasicDBObject("count", "-1"));
				while(all.hasNext()){
					DBObject obj = all.next();
					System.out.println(obj.get("cheese"));
					seen = (Integer) obj.get("count");
				}
			}
		}
		private DBObject greaterThan(int seen) {
			return new BasicDBObject("count", new BasicDBObject("$gt", seen));
		}
	}
	
	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		ThreadPoolExecutor pool = GlobalExecutorPool.getPool();
		pool.execute(new WriteWait());
		pool.execute(new ReadFast());
		pool.shutdown();
		pool.awaitTermination(2, TimeUnit.MINUTES);
	}
}
