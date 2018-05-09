import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

public class MastermindWorker extends Thread{
	private OutputStream ostream;
	private InputStream istream;
	private Socket cs;
	private byte correctCombo[];
	private PrintWriter sockPrintWriter;

	private Random rand = new Random();

	public MastermindWorker(Socket clientSocket) throws IOException{
		ostream = clientSocket.getOutputStream();
		istream = clientSocket.getInputStream();
		cs = clientSocket;
		correctCombo = new byte[]{1,0,1,0};
	}
	@Override
	public void run(){
		try {
			System.out.println("Received connection, HTTP request handler started.");

			BufferedReader br = new BufferedReader(new InputStreamReader(istream));
			sockPrintWriter = new PrintWriter(ostream, true); //Autoflush = true: println autoflushes
			String requestString = br.readLine();
			System.out.println("Received requestString: " + requestString);

			String[] request = requestString.split(" ");
			if(request.length != 3 || !request[0].equals("GET") || !request[2].equals("HTTP/1.1")){
				sockPrintWriter.println("HTTP/1.1 400\r\n");
				System.out.println("Unrecognized request, sent 404 and closing httpHandler.");
				cs.close();
				return;
			}

			if(request[1].equals("/")){
				System.out.println("Requesting root page: redirecting.");
				sockPrintWriter.println("HTTP/1.1 303 See Other\r\nLocation: /play.html\r\n");
				cs.close();
				return;
			}
			
			String[] url = request[1].split("\\?");
			// Removing leading slash
			String filename = url[0].substring(1);
			System.out.println("Requested: "+filename);
			String[]  temp = filename.split("\\.");


			if(filename.equals("eval")){
				if(url.length != 2){
					System.out.println("Request improperly formated: 400");
					sockPrintWriter.println("HTTP/1.1 400\r\n");
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
			    sockPrintWriter.println("HTTP/1.1 404 Not Found\r\n"); // the file does not exists
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
				System.out.println("Requested unauthorized file format: 404");
				sockPrintWriter.println("HTTP/1.1 404 Not Found\r\n");
				cs.close();
				return;
			}
			System.out.println("Authorized file format and file exists: sending file...");
			// Header
			sockPrintWriter.println("HTTP/1.1 200 OK\r\nContent-type: text/"+ctype+"\r\n");
			BufferedReader bfr = new BufferedReader(new FileReader(file));
			String line;
			while ((line = bfr.readLine()) != null) {
			    sockPrintWriter.println(line);
			}
			bfr.close();
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
			sockPrintWriter.println("HTTP/1.1 400\r\n");
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
				sockPrintWriter.println("HTTP/1.1 400\r\n");
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
		sockPrintWriter.println("HTTP/1.1 204 No Content\r\nCorrect: "+correct+"\r\nMisplaced: "+misplaced+"\r\n");
	}
}