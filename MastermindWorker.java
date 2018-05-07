import java.io.*;
import java.net.*;
import java.util.*;

public class MastermindWorker extends Thread{
	private OutputStream ostream;
	private InputStream istream;
	private Socket cs;
	private byte combination[];
	private byte previousTries[][];
	private boolean gameStarted;
	private boolean playing;
	private static final byte PROTOCOL_VERSION = 1;
	private byte nbTries;
	private BufferedReader br;

	private Random rand = new Random();

	public MastermindWorker(Socket clientSocket) throws IOException{
		ostream = clientSocket.getOutputStream();
		istream = clientSocket.getInputStream();
		cs = clientSocket;
		gameStarted = false;
		playing = true;
		nbTries = 0;
		previousTries = new byte[12][6];
		combination = new byte[4];
	
}
	@Override
	public void run(){
		try {
			System.out.println("Worker started.");

			br = new BufferedReader(new InputStreamReader(istream));
			String request = br.readLine();
			System.out.println("Received request: " + request);
			PrintWriter out = new PrintWriter(ostream, true);

			String[] requestParam = request.split(" ");
			if(!requestParam[0].equals("GET") || !requestParam[2].equals("HTTP/1.1")){
				out.println("HTTP/1.1 400\r\n");
				System.out.println("Unrecognized request1, closing.");
				out.close();
				cs.close();
				return;
			}

			if(requestParam[1].equals("/")){
				out.println("HTTP/1.1 303 See Other\r\nLocation: /play.html\r\n");
				out.close();
				return;
			}

			
			String[] url = requestParam[1].split("\\?");
			System.out.println("split url length: "+url.length);
			String filename = url[0].substring(1);
			System.out.println("file name: "+filename);
			File file = new File(filename);
			if (!file.exists()) {
			     out.println("HTTP/1.1 404\r\n"); // the file does not exists
			     out.close();
			     return;
			}
			System.out.println(requestParam[1]);
			String[] nomFichier = filename.split("\\.");
			System.out.println("longueur nomFichier: "+nomFichier.length);
			String extension = nomFichier[1];
			out.println("HTTP/1.1 200 OK");
			String ctype = "";
			if(extension.equals("css"))
				ctype = "css";
			if(extension.equals("js"))
				ctype = "javascript";
			if(extension.equals("html"))
				ctype = "html";
			out.println("Content-type: text/"+ctype+"\r\n");
			FileReader fr = new FileReader(file);
			BufferedReader bfr = new BufferedReader(fr);
			String line;
			while ((line = bfr.readLine()) != null) {
			    out.println(line);
			}
			bfr.close();

			if(url.length == 2){
				String[] buttons = url[1].split("&");
				System.out.println("number of buttons: "+buttons.length);
	
				if(buttons.length != 4){
					out.println("<script>alert(\"Combinaison non-reconnue.\")</script>");
					out.close();
					System.out.println("Processed request, closing socket.");
					cs.close();
				}
	
				for(int i = 0; i < 4; i++){
					String[] keyVal = buttons[i].split("=");
					if(keyVal.length != 2){
						ostream.write("HTTP 400".getBytes());
						System.out.println("Unrecognized request3, closing.");
						cs.close();
						return;
					}
					out.println("<script> document.getElementById(\"button"+i+"\").backgroundColor = colors["+keyVal[1]+"];</script>");
				}
			}

			out.close();
			System.out.println("Processed request, closing socket.");
			cs.close();
			/*
			byte msg[] = new byte[6];
			while(playing){
				int len = istream.read(msg);
				if(len <= 0)
					break;

				// Messages with different protocol versions shouldn't be interpreted any further
				if(msg[0] != PROTOCOL_VERSION)
					sendMessage(new byte[] {PROTOCOL_VERSION,4});
					
				switch(msg[1]){
					case 0:
					if(len == 2)
						startGame();
					else
						sendMessage(new byte[] {PROTOCOL_VERSION,4});
					break;
					case 1:
					if(gameStarted)
						checkCombination(msg, len);
					else
						sendMessage(new byte[] {PROTOCOL_VERSION,4});
					break;
					case 2:
					if(len == 2)
						listTries();
					else
						sendMessage(new byte[] {PROTOCOL_VERSION,4});
					break;
					default:
					sendMessage(new byte[] {PROTOCOL_VERSION,4});
				}
			}*/
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private void sendMessage(byte msg[]) throws IOException{
		ostream.write(msg);
		ostream.flush();
	}

	private void startGame() throws IOException{
		if(gameStarted){
			// Can't start a game if the previous one hasn't been won or lost.
			sendMessage(new byte[] {PROTOCOL_VERSION,4});
			return;
		}
		gameStarted = true;
		nbTries = 0;
		previousTries = new byte[12][6];
		System.out.print("Selected combination: ");
		for(int i = 0; i < 4; i++){
			combination[i] = (byte)rand.nextInt(6);
			// Using a string as a character lookup table
			System.out.print("RBYGWK".charAt(combination[i]));
		}
		
		System.out.print("\n");
		sendMessage(new byte[] {PROTOCOL_VERSION,1});
	}

	private void checkCombination(byte msg[], int len) throws IOException{
		if(len != 6){
			sendMessage(new byte[] {PROTOCOL_VERSION,4});
			return;
		}
		nbTries++;
		byte correct = 0;
		byte misplaced = 0;
		byte cmb[] = combination.clone();

		// Copy the combination into previousTries
		for(byte i = 0; i < 4; i++)
			previousTries[nbTries-1][i] = msg[i+2];

		// Count correct guesses and remove them from both cmb and msg so they're not counted multiple times
		for(byte i = 0; i < 4; i++){
			if(msg[i+2] == cmb[i]){
				correct++;
				msg[i+2] = -1;
				cmb[i] = -1;
			}
		}
		// Count misplaced guesses, remove them after so they're not counted multiple times.
		for(byte i = 0; i < 4; i++){
			for(byte j = 0; j < 4; j++){
				if(msg[i+2] != -1 && msg[i+2] == cmb[j]){
					misplaced++;
					cmb[j] = -1;
				}
			}
		}

		// Save the correctness along the combination so that it doesn't need to be recalculated if client asks for previous attempts
		previousTries[nbTries-1][4] = correct;
		previousTries[nbTries-1][5] = misplaced;
		sendMessage(new byte[] {PROTOCOL_VERSION,2,correct,misplaced});
		if(nbTries == 12 || correct == 4){
			gameStarted = false;
		}
	}

	private void listTries() throws IOException{
		byte msg[] = new byte[3+nbTries*6];
		msg[0] = PROTOCOL_VERSION;
		msg[1] = 3;
		msg[2] = nbTries;
		// 3+6*i is the index of an attempt in the byte array
		for(byte i = 0; i < nbTries; i++)
			System.arraycopy(previousTries[i], 0, msg, 3+6*i, 6);
		sendMessage(msg);
	}
}