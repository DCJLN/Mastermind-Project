import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

public class MastermindWorker extends Thread{
	private OutputStream ostream;
	private InputStream istream;
	private Socket cs;
	private byte correctCombo[];

	private Random rand = new Random();

	public MastermindWorker(Socket clientSocket) throws IOException{
		cs = clientSocket;
		ostream = cs.getOutputStream();
		istream = cs.getInputStream();
		correctCombo = new byte[]{1,0,1,0};
	}
	@Override
	public void run(){
		try {
			System.out.println("Received connection, HTTP request handler started.");
			HTTPrequest request;
			try{
				request = new HTTPrequest(istream);
			}
			catch(IllegalArgumentException e){
				System.out.println("Unrecognized request, sending 400 and closing connection");
				HTTPresponse resp = new HTTPresponse("400");
				resp.send(ostream);
				cs.close();
				return;
			}

			if(request.getUrl().equals("/")){
				System.out.println("Requested root page: redirecting.");
				HTTPresponse resp = new HTTPresponse("303");
				resp.addHeader("Location","/play.html");
				resp.send(ostream);
				cs.close();
				return;
			}
			
			String[] url = request.getUrl().split("\\?");
			// Removing leading slash
			String filename = url[0].substring(1);
			System.out.println("Requested: "+filename);
			String[]  temp = filename.split("\\.");


			if(filename.equals("eval")){
				if(url.length != 2){
					System.out.println("Request improperly formated: 400");
					HTTPresponse resp = new HTTPresponse("400");
					resp.send(ostream);
					cs.close();
					return;
				}
				String params = url[1];
				testCombination(params);
				cs.close();
				return;
			}

			File file = new File(filename);
			// if file doesn't exist or has 0 or more than 1 extensions
			if (!file.exists() || temp.length != 2) {
				System.out.println("Requested file doesn't exist: 404");
				HTTPresponse resp = new HTTPresponse("404");
				resp.send(ostream);
			    cs.close();
			    return;
			}
			// Else the file exists, send the file
			String ext = temp[1];
			String ctype = "";
			if(ext.equals("css"))
				ctype = "css";
			else if(ext.equals("js"))
				ctype = "javascript";
			else if(ext.equals("html"))
				ctype = "html";
			else{ // Requested an unauthorized file format, send 404
				System.out.println("Requested unauthorized file format: 403");
				HTTPresponse resp = new HTTPresponse("403");
				resp.send(ostream);
				cs.close();
				return;
			}
			System.out.println("Authorized file format and file exists: sending file...");
			// Header
			HTTPresponse resp = new HTTPresponse("200");
			resp.addHeader("Content-type", "text/"+ctype);
			resp.addHeader("Transfer-Encoding", "Chunked");
			BufferedReader bfr = new BufferedReader(new FileReader(file));
			String line;
			while ((line = bfr.readLine()) != null) {
			    resp.addRespLine(line);
			}
			bfr.close();
			resp.send(ostream);
			cs.close();
			System.out.println("Request Processed successfully");
			return;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private void testCombination(String vars){
		System.out.println("Entered testCombination().");
		String[] combination = vars.split("-");
		if(combination.length != 4){
			System.out.println("Combination is not 4 long, sending 400");
			HTTPresponse resp = new HTTPresponse("400");
			resp.send(ostream);
			return;		
		}
		byte[] combo = new byte[4];
		for(int i = 0; i < 4; i++){
			try{
				combo[i] = Byte.parseByte(combination[i]);
			}
			catch(NumberFormatException e){
				// Couldn't parse one of the colors
				System.out.println("Couldn't parse combination, sending 400");
				HTTPresponse resp = new HTTPresponse("400");
				resp.send(ostream);
				return;
			}
		}

		byte correct = 0;
		byte misplaced = 0;
		byte cmb[] = correctCombo.clone();

		// Count correct guesses and remove them from both cmb and combo so they're not counted multiple times
		for(byte i = 0; i < 4; i++){
			if(combo[i] == cmb[i]){
				correct++;
				combo[i] = -1;
				cmb[i] = -1;
			}
		}
		// Count misplaced guesses, remove them after so they're not counted multiple times.
		for(byte i = 0; i < 4; i++){
			for(byte j = 0; j < 4; j++){
				if(combo[i] != -1 && combo[i] == cmb[j]){
					System.out.println("misplaced: "+i+" matches with "+j);
					misplaced++;
					combo[i] = -1;
				}
			}
		}

		System.out.println("Successfully tested combination");
		HTTPresponse resp = new HTTPresponse("204");
		resp.addHeader("Correct", Integer.toString(correct));
		resp.addHeader("Misplaced", Integer.toString(misplaced));
		resp.send(ostream);
	}
}