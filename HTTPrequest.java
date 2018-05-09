import java.io.*;
import java.lang.*;
import java.util.*;

public class HTTPrequest {
	private String method;
	private String url;
	private String httpVersion;
	private Map<String, String> headers;

	public HTTPrequest(InputStream istream) throws IllegalArgumentException, IOException{
		headers = new HashMap<String, String>();

		BufferedReader br = new BufferedReader(new InputStreamReader(istream));
		String request = br.readLine();
		String[] temp = request.split(" ");

		if(temp.length != 3)
			throw new IllegalArgumentException("Request couldn't be understood");

		String[] temp2 = temp[2].split("/");
		if(temp2.length != 2 || !temp2[0].equals("HTTP"))
			throw new IllegalArgumentException("Request couldn't be understood");

		method = temp[0];
		url = temp[1];
		httpVersion = temp2[1];

		String line;
		while(!(line = br.readLine()).equals("")){
			temp = line.split(": ");
			if(temp.length != 2 || headers.containsKey(temp[0]))
				throw new IllegalArgumentException("Request couldn't be understood");
			headers.put(temp[0], temp[1]);
		}
	}

	public String getUrl(){
		return url;
	}

	/*
	public static void send(OutputStream ostream){
		PrintWriter out = new PrintWriter(ostream, true);
		out.println(method+" "+url+" HTTP/"+httpVersion);
		for(Map.Entry<String, String> header : headers.entrySet())
			out.println(header.getKey()+": "+header.getValue());
		out.println();
		out.close();
	}*/
}