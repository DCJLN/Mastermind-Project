import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

public class MastermindWorker extends Thread{
	private OutputStream ostream;
	private InputStream istream;
	private Socket cs;
	private byte correctCombo[];
	private Map<String, MastermindSession> sessions;
	private MastermindSession currentSession;
	private String[] colors;

	public MastermindWorker(Socket clientSocket, Map<String, MastermindSession> sessions) throws IOException{
		this.sessions = sessions;
		cs = clientSocket;
		ostream = cs.getOutputStream();
		istream = cs.getInputStream();
		colors = new String[]{"red", "blue", "yellow", "green", "white", "black"};
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

			int skip = 0;
			List<Byte[]> attempts = null;
			if(filename.equals("play.html") || filename.equals("eval")){
				System.out.println("Currently stored sessions: ");
				for(String id : sessions.keySet())
					System.out.println(id);
				deleteInvalidSessions();
				System.out.println("After deleting invalids: ");
				for(String id : sessions.keySet())
					System.out.println(id);
				Set<String> cookies = request.getHeader("Cookie");
				if(cookies != null){					
					String allCookies = "";
					for(String cookie : cookies)
						allCookies += cookie+"; ";
					String[] cookieArray = allCookies.split("; ");
					Map<String, String> cookieMap = new HashMap<String, String>();
					for(String cookie : cookieArray){
						String[] temp = cookie.split("=");
						if(temp.length == 2)
							cookieMap.put(temp[0], temp[1]);
					}
					if(cookieMap.containsKey("SESSID")){
						if(sessions.containsKey(cookieMap.get("SESSID"))){
							currentSession = sessions.get(cookieMap.get("SESSID"));
						}
						else{
							currentSession = new MastermindSession();
							sessions.put(currentSession.getID(), currentSession);
						}
					}
					else{
						currentSession = new MastermindSession();
						sessions.put(currentSession.getID(), currentSession);
					}
				}
				else{
					currentSession = new MastermindSession();
					sessions.put(currentSession.getID(), currentSession);
				}
				skip = (12 - currentSession.nbTries())*8;
				attempts = currentSession.previousTries();
				correctCombo = currentSession.getCombo();
				System.out.print("Correct combo: ");
				for(int i = 0; i < 4; i++)
					System.out.print(correctCombo[i]);
				System.out.println();
			}

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
			String[]  temp = filename.split("\\.");
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
			HTTPresponse resp = new HTTPresponse("200");
			resp.addHeader("Content-type", "text/"+ctype);
			resp.addHeader("Transfer-Encoding", "Chunked");
			if(filename.equals("play.html"))
				resp.addHeader("Set-Cookie", "SESSID="+currentSession.getID());
			BufferedReader bfr = new BufferedReader(new FileReader(file));
			String line;
			int currentAttempt = 11;
			int column = 0;
			while ((line = bfr.readLine()) != null) {
				if(filename.equals("play.html")){
					if(line.contains("&")){
						if(skip == 0){
							line.replace("&", colors[attempts.get(currentAttempt)[column%4]]);
							if(column == 7)
								currentAttempt--;
						}
						else{
							line.replace(" &", "");
							skip--;
						}
						column = (column+1)%8;
					}
				}
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

		System.out.print("Testing combination ");
		for(int i = 0; i < 4; i++)
			System.out.print(combo[i]);
		System.out.println();

		System.out.println("Current attempt: "+currentSession.nextTry());

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
					misplaced++;
					combo[i] = -1;
				}
			}
		}

		if(correct == 4)
			currentSession.invalidate();

		System.out.println("Successfully tested combination");
		HTTPresponse resp = new HTTPresponse("204");
		resp.addHeader("Correct", Integer.toString(correct));
		resp.addHeader("Misplaced", Integer.toString(misplaced));
		resp.addHeader("Set-Cookie", "SESSID="+currentSession.getID());
		resp.send(ostream);
	}

	public void deleteInvalidSessions(){
		// 10 min = 600000 ms
		sessions.entrySet().removeIf(entry -> entry.getValue().getAge() > 600000);
		sessions.entrySet().removeIf(entry -> entry.getValue().nbTries() >= 12);

	}
}