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
		colors = new String[]{"red", "blue", "yellow", "green", "white", "black", "correct", "misplaced"};
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

			if(request.getMethod().equals("GET") && request.getUrl().equals("/")){
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
				// if testing a combo, reset the session if the game was won last turn
				if(filename.equals("eval") && ((currentSession.nbTries() > 0 && currentSession.previousTries().get(currentSession.nbTries()-1)[4] == 4) || currentSession.nbTries() >= 12))
					currentSession.reset();
				correctCombo = currentSession.getCombo();
				System.out.print("Correct combo: ");
				for(int i = 0; i < 4; i++)
					System.out.print(correctCombo[i]);
				System.out.println();
			}

			if(filename.equals("eval")){
				if(request.getMethod().equals("GET")){
					if(url.length != 2){
						System.out.println("Request improperly formated: 400");
						HTTPresponse resp = new HTTPresponse("400");
						resp.send(ostream);
						cs.close();
						return;
					}
					String params = url[1];
					testCombination(params, false);	
				}
				if(request.getMethod().equals("POST")){
					Map<String, String> paramsMap = request.getParams();
					String params = paramsMap.get("guess1")+"-"+paramsMap.get("guess2")+"-"+paramsMap.get("guess3")+"-"+paramsMap.get("guess4");
					testCombination(params, true);
				}
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
			while ((line = bfr.readLine()) != null) {
				resp.addRespLine(line);
			}
			bfr.close();
			if(filename.equals("play.html"))
				fillAttemptGrid(resp);
			resp.send(ostream);
			cs.close();
			System.out.println("Request Processed successfully");
			return;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private void testCombination(String vars, boolean reloadPage){
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
		byte[] attempt = combo.clone();

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
					cmb[j] = -1;
				}
			}
		}

		currentSession.addTry(new Byte[]{attempt[0], attempt[1], attempt[2], attempt[3], correct, misplaced});

		System.out.println("Successfully tested combination, "+correct+" correct, "+misplaced+" misplaced.");
		if(reloadPage){
			HTTPresponse resp = new HTTPresponse("303");
			resp.addHeader("Location","/play.html");
			resp.addHeader("Set-Cookie", "SESSID="+currentSession.getID());
			resp.send(ostream);
			return;
		}

		HTTPresponse resp = new HTTPresponse("204");
		resp.addHeader("Correct", Integer.toString(correct));
		resp.addHeader("Misplaced", Integer.toString(misplaced));
		resp.addHeader("Set-Cookie", "SESSID="+currentSession.getID());
		resp.send(ostream);
	}

	public void deleteInvalidSessions(){
		// 10 min = 600000 ms
		sessions.entrySet().removeIf(entry -> entry.getValue().getAge() > 600000);
		sessions.entrySet().removeIf(entry -> entry.getValue().nbTries() > 12);
		sessions.entrySet().removeIf(entry -> !entry.getValue().isValid());
	}

	public void fillAttemptGrid(HTTPresponse resp){
		List<Byte[]> attempts = currentSession.previousTries();
		List<String> respBody = resp.getBody();
		int nbTries = currentSession.nbTries();
		int[][] grid = new int[12][8];
		for(int i = 11; i >= 0; i--){
			if(nbTries > i){
				Byte[] att = attempts.get(i);
				int j;
				for(j = 0; j < 4; j++){
					grid[i][j] = att[j];
				}
				for(int k = 0; k < att[4]; k++){
					grid[i][j] = 6; //correct
					j++;
				}
				for(int k = 0; k < att[5]; k++){
					grid[i][j] = 7; //misplaced
					j++;
				}
				for(;j < 8; j++)
					grid[i][j] = -1;

			}
			else{
				for(int j = 0; j < 8; j++)
					grid[i][j] = -1;
			}
		}

		int counter = 0;
		for(ListIterator<String> i = respBody.listIterator(); i.hasNext();){
			String line = i.next();
			if(line.contains("&")){
				int row = 11-counter/8;
				int column = counter%8;
				int colorIndex;
				if((colorIndex = grid[row][column]) == -1)
					i.set(line.replace(" &", ""));
				else
					i.set(line.replace("&", colors[colorIndex]));
				counter++;
			}
			else
				i.set(line.replace(" &", ""));
		}
	}
}