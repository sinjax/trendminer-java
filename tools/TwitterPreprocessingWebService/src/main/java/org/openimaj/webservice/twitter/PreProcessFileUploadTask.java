package org.openimaj.webservice.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemHeadersSupport;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;
import org.apache.log4j.Logger;
import org.openimaj.logger.LoggerUtils;
import org.openimaj.tools.twitter.modes.preprocessing.TwitterPreprocessingMode;
import org.openimaj.twitter.GeneralJSON;
import org.openimaj.twitter.USMFStatus;
import org.openimaj.twitter.collection.StreamTwitterStatusList;
import org.openimaj.twitter.collection.TwitterStatusListUtils;
import org.restlet.Request;
import org.restlet.ext.fileupload.RepresentationContext;
import org.restlet.ext.fileupload.RestletFileUpload;

class PreProcessFileUploadTask extends FileUpload implements Runnable {

	private Request req;
	private PreProcessAppOptions options;
//	private List<FileItem> items;
//	private FileItem fi;
	private static Logger logger = Logger.getLogger(PreProcessFileUploadTask.class);

	public PreProcessFileUploadTask(Request request, PreProcessAppOptions options) throws IOException {
		this.req = request;
		this.options = options;
	}

	@Override
	public void run() {
		try{
			logger.debug("Parsing the request");
			parseRequest(new RepresentationContext(req.getEntity()));
			
		} catch(Exception e){
			e.printStackTrace();
			return;
		}
		options.close();
	}
	
	
	
	public List<FileItem> parseRequest(RepresentationContext ctx) throws FileUploadException {
		try {
            FileItemIterator iter = getItemIterator(ctx);
            
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                if(!item.getFieldName().equals("data")){
                	throw new FileUploadException(String.format("File item found not called 'data', called: '%s', failing",item.getName()));
//                	continue;
                }
                try {
                    consumeStream(item.openStream());
                } catch (FileUploadIOException e) {
                    throw (FileUploadException) e.getCause();
                } catch (IOException e) {
                    throw new IOFileUploadException("Processing of " + MULTIPART_FORM_DATA + " request failed. " + e.getMessage(), e);
                } catch (Exception e) {
                    throw new FileUploadException(e.getMessage(),e);
                }
            }
            return null;
        } catch (FileUploadIOException e) {
            throw (FileUploadException) e.getCause();
        } catch (IOException e) {
            throw new FileUploadException(e.getMessage(), e);
        }
		
	}

	private void consumeStream(InputStream fi) throws Exception {
		List<USMFStatus> list = StreamTwitterStatusList.readUSMF(fi, options.getInputClass().type(), "UTF-8");
		long seen = 0;
		for (USMFStatus usmfStatus : list) {
			LoggerUtils.debug(logger, String.format("Processing item: %d",seen++), seen%1000==0);
			processStatus(usmfStatus, options);
		}
	}

	private void processStatus(USMFStatus usmfStatus,PreProcessAppOptions options) throws Exception {
		if(usmfStatus.isInvalid() || usmfStatus.text.isEmpty()){
			if(options.veryLoud()){
				logger.debug("\nTWEET INVALID, skipping.");
			}
			return;
		}
		if(options.preProcessesSkip(usmfStatus)) return;
		for (TwitterPreprocessingMode<?> mode : options.modeOptionsOp) {
			try {
				TwitterPreprocessingMode.results(usmfStatus, mode);
			} catch (Exception e) {
				logger.error(String.format("Problem producing %s for %s",
						usmfStatus.id, mode.toString()), e);
			}
		}
		
		if(options.postProcessesSkip(usmfStatus)) return;
		PrintWriter outputWriter = options.getOutputWriter();
		options.ouputMode().output(options.convertToOutputFormat(usmfStatus), outputWriter);
		outputWriter.println();
		outputWriter.flush();
	}

}
