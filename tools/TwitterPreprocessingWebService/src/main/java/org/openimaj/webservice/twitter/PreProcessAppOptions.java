package org.openimaj.webservice.twitter;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.openimaj.tools.twitter.options.AbstractTwitterPreprocessingToolOptions;
import org.restlet.data.Form;

class PreProcessAppOptions extends
		AbstractTwitterPreprocessingToolOptions {

	private PrintWriter writer = null;

	public PreProcessAppOptions(Form query, Map<String, Object> reqAttr) throws CmdLineException {
		super(constructArgs(query,reqAttr));
	}

	@Override
	public boolean validate() throws CmdLineException {
		return true;
	}

	public void setOutputWriter(Writer ow) {
		this.writer = new PrintWriter(ow);
	}

	public PrintWriter getOutputWriter() {
		return writer;
	}

	public void close() {
		this.writer.close();
	}
	private static String[] constructArgs(Form query, Map<String, Object> reqAttr) {
		List<String> arglist = new ArrayList<String>();
		List<String> argNames = new ArrayList<String>();
		for (int i = 0; i < query.size(); i++) {
			String paramname = query.get(i).getName();
			if(!argNames.contains(paramname)){
				argNames.add(paramname);
			}
		}
		for (String arg : argNames) {
			String[] argvals = query.getValuesArray(arg);
			for (String argval : argvals) {
				arglist.add(String.format("-%s", arg));
				arglist.add(String.format("%s", argval));
			}
		}
		String intype = (String) reqAttr.get("intype");
		String outtype = (String) reqAttr.get("outtype");
		arglist.add("-it");
		arglist.add(intype);
		arglist.add("-ot");
		arglist.add(outtype);
		return arglist.toArray(new String[arglist.size()]);
	}
}