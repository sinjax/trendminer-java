package org.openimaj.twitter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.openimaj.twitter.USMFStatus.User;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;

public class GeneralJSONRubyT extends GeneralJSON{
	
	long twitterID;
	String created_at;
	String user;
	String text;
	
	@Override
	public void readASCII(Scanner in) throws IOException {
		this.fillFromString(in.nextLine());
	}
	
	@Override
	public void writeASCII(PrintWriter out) throws IOException {
		CSVPrinter print = new CSVPrinter(out);
		print.print(new String[]{twitterID + "",created_at,user,text});
		print.flush();
	}

	@Override
	public void fillUSMF(USMFStatus status) {
		status.id = twitterID;
		status.date = created_at;
		status.user = new User();
		status.user.name = user;
		status.text = text;
		status.service = "twitter";
	}

	@Override
	public void fromUSMF(USMFStatus status) {
		this.created_at = status.date;
		this.twitterID = status.id;
		this.user = status.user.name;
		this.text = status.text;
	}

	@Override
	public GeneralJSON instanceFromString(String line) {
		GeneralJSONRubyT instance = new GeneralJSONRubyT();
		instance.fillFromString(line);
		return instance;
	}

	private void fillFromString(String line) {
		String[] parts = CSVParser.parse(line)[0];
		twitterID = Long.parseLong(parts[0]);
		created_at = parts[1];
		user = parts[2];
		text = parts[3];
	}

}
