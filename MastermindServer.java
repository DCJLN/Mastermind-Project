import java.io.*;
import java.net.*;

public class MastermindServer {

	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("Please start the server with the number of threads as argument.");
			return;
		}

		try{
			ServerSocket ss = new ServerSocket(8019);
			System.out.println("Mastermind server started, awaiting connections on port 8019...");
			while(true){
				Socket clientSocket = ss.accept();
				MastermindWorker w = new MastermindWorker(clientSocket);
				w.start();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}
}