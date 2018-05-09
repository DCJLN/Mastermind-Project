import java.lang.*;
import java.io.*;
import java.util.*;

public class HTTPresponse {
	private String httpVersion;
	private String returnCode;
	private String status;
	private Map<String, String> headers;
	private String responseBody;

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

	public HTTPresponse(String httpVersion, String returnCode, String status, Map headers, String responseBody){

	}

	public HTTPresponse(String returnCode) throws IllegalArgumentException{
		defaultStatus = new HashMap<String, String>();
		headers = new HashMap<String, String>();
		responseBody = "";
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
			for(Map.Entry<String, String> header : headers.entrySet())
				pw.println(header.getKey()+": "+header.getValue());
		pw.println(); // header ends with an empty line.
		if(responseBody != null){
			if(headers.containsKey("Transfer-Encoding") && headers.get("Transfer-Encoding").equals("Chunked")){
				BufferedReader br = new BufferedReader(new StringReader(responseBody));
				String line;
				try{
					while((line = br.readLine()) != null){
						if(line.length() == 0)
							continue;
						pw.println(Integer.toHexString(line.length()+1));
						pw.println(line);
					}
					pw.println("0");
				}
				catch(IOException e){
					e.printStackTrace();
				}
			}
			else
				pw.println(responseBody);
		}
		pw.close();
	}

	public void addHeader(String key, String value){
		headers.put(key,value);
	}

	public void addRespLine(String line){
		responseBody += line+"\r\n";
	}
}