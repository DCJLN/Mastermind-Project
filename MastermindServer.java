import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.*;

public class MastermindServer {

	public static void main(String[] args){

		if(args.length != 1){
			System.out.println("Please start the server with the number of threads as argument.");
			return;
		}
		int maxThreads;
		try{
			maxThreads = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException e){
			System.out.println("Number of threads couldn't be parsed, aborting.");
			return;
		}

		Map<String, MastermindSession> sessions = new HashMap<String, MastermindSession>();

		try{
			ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
			ServerSocket ss = new ServerSocket(8019);
			System.out.println("Mastermind server started, awaiting connections on port 8019...");
			while(true){
				Socket clientSocket = ss.accept();
				MastermindWorker w = new MastermindWorker(clientSocket, sessions);
				threadPool.execute(w);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}
}