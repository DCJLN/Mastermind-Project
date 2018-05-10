import java.util.*;
import java.lang.*;

public class MastermindSession {
	private int nbTries;
	private List<Byte[]> previousTries;
	private String sessionID;
	private Date creationTimestamp;
	private byte[] winningCombo;
	private Random rand = new Random();
	public MastermindSession(){
		sessionID = UUID.randomUUID().toString();
		previousTries = new ArrayList<Byte[]>();
		nbTries = 0;
		creationTimestamp = new Date();
		winningCombo = new byte[4];
		for(int i = 0; i < 4; i++)
			winningCombo[i] = (byte)rand.nextInt(6);
	}

	public String getID(){
		return sessionID;
	}

	public long getAge(){
		return (new Date().getTime() - creationTimestamp.getTime());
	}

	public int nbTries(){
		return nbTries;
	}

	public List<Byte[]> previousTries(){
		return previousTries;
	}

	public byte[] getCombo(){
		return winningCombo;
	}

	public int nextTry(){
		return ++nbTries;
	}

	public void invalidate(){
		nbTries = 12;
	}
}