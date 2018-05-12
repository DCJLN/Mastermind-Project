import java.lang.*;
import java.io.*;
import java.util.*;

public class HTTPresponse {
	private String httpVersion;
	private String returnCode;
	private String status;
	private Map<String, Set<String>> headers;
	private List<String> responseBody;

	private Map<String, String> defaultStatus;

	public void initializeDefaultStatus(){
		defaultStatus.put("200", "OK");
		defaultStatus.put("204", "No Content");
		defaultStatus.put("303", "See Other");
		defaultStatus.put("400", "Bad Request");
		defaultStatus.put("403", "Forbidden");
		defaultStatus.put("404", "Not Found");
		defaultStatus.put("404", "Not Found");
		defaultStatus.put("405", "Method Not Allowed");
		defaultStatus.put("411", "Length Required");
		defaultStatus.put("501", "Not Implemented");
		defaultStatus.put("505", "HTTP Version Not Supported");
	}

	public HTTPresponse(String returnCode) throws IllegalArgumentException{
		defaultStatus = new HashMap<String, String>();
		headers = new HashMap<String, Set<String>>();
		responseBody = new ArrayList<String>();
		initializeDefaultStatus();
		if(!defaultStatus.containsKey(returnCode))
			throw new IllegalArgumentException("Request couldn't be understood");
		this.returnCode = returnCode;
		httpVersion = "1.1";
		status = defaultStatus.get(returnCode);
	}

	public void send(OutputStream ostream){
		PrintWriter pw = new PrintWriter(ostream);
		pw.println("HTTP/"+httpVersion+" "+returnCode+" "+status);
		if(headers != null)
			for(Map.Entry<String, Set<String>> header : headers.entrySet())
				for(String value : header.getValue())
					pw.println(header.getKey()+": "+value);
		if(responseBody != null){
			if(headers.containsKey("Transfer-Encoding") && headers.get("Transfer-Encoding").contains("Chunked")){
				pw.println(); // header ends with an empty line.
				for(String line : responseBody){
					if(line.length() == 0)
						continue;
					// length + 1 for the newline character.
					pw.println(Integer.toHexString(line.length()+1));
					pw.println(line);
				}
				pw.println("0");
			}
			else{
				int contentLength = 0;
				for(String line : responseBody)
					contentLength += line.length()+1;
				pw.println("Content-Length: "+contentLength);
				pw.println(); // header ends with an empty line.
				for(String line : responseBody)
					pw.println(responseBody);
			}
		}
		pw.close();
	}

	public void addHeader(String key, String value){
		if(headers.containsKey(key))
			headers.get(key).add(value);
		else
			headers.put(key, new HashSet<String>(Arrays.asList(value)));
	}

	public void addRespLine(String line){
		responseBody.add(line);
	}

	public List<String> getBody(){
		return responseBody;
	}
}