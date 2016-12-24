import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
// ports = 8002 onwards
public class Requester3 extends Thread {
	static DatagramSocket socket;
	static int myPort;
	static String serverport;
	
	
	public void run(){
		String receivedMsg = receivePacket();
		(new Requester3()).start();
		List<String> receivedList = seperateWrtDash(receivedMsg);
		String command=receivedList.get(2);	
		if(command.equals("DONENOTFOUND")) { //from server
			String hash=receivedList.get(5);
			cater_to_DONENOTFOUND_from_server(hash);
		}else if(command.equals("DONEFOUND")) {
			String hash=receivedList.get(5);
			String password=receivedList.get(6);
			cater_to_DONEFOUND_from_server(hash,password);
		}else if(command.equals("NOTDONE")) {
			String hash=receivedList.get(5);
			cater_to_NOTDONE_from_server(hash);
		}else if(command.equals("CANCELLEDJOB")){
			System.out.println("(REPLY FROM SERVER: job was CANCELLED!)");
		}	
	}	
	public static void main(String[] args){
		initializeGlobals();
		(new Requester3()).start();
		Scanner sc = new Scanner(System.in);
		String hash="";
		while(true){
			System.out.println("\n\nEnter 0-Exit. 1-send JOB"+
			". 2-send PING"+
			". 3-calculate hash of a word. 4-send CANCELJOB");
			try{
				int i=sc.nextInt();
				if(i==0) {
					break;
				}else if(i==1){
					System.out.println("Enter Hash:");
					hash=sc.next();
					send_JOB_to_server(hash);	
				}else if(i==2) {
					//assume: a hash is sent to the server by me previously
					if(hash.equals("")) {
						System.out.println("First send a hash to Server");
					}else{
						send_PING_to_server(hash);
					}
				}else if(i==3) {
					String s=sc.next();
					String h=findHash(s);
					PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
					writer.println(h);
					writer.close();					
					System.out.println("Word: "+s+". Hash: "+h+". Hash also written to the-file-name.txt");
				}else if(i==4) {
					if(hash.equals("")) {
						System.out.println("First send a hash to Server");
					}else{
						send_CANCELJOB_to_server(hash);
					}
				}
			}catch(Exception e) {
				System.out.println("Wrong input. Try Again");
			}
		}
	}	

	static void initializeGlobals() {
		myPort=8004;
		try {
			socket = new DatagramSocket(myPort);
		}catch(Exception e) {
			System.out.println("Error in initializeGlobals()");
		}		
		serverport="8001";
	}
	
	static String receivePacket() {
		byte[] buffer = new byte[2000];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		String received = "";
		try {
			socket.receive(packet);
			received = new String(packet.getData(),0,packet.getLength());
		} catch(Exception e) {	
			System.out.println("Error in receivePacket()");
			e.printStackTrace();
		}		
		return received;
	}	
	static List<String> seperateWrtDash(String s) {
		String t="";
		List<String> v = new ArrayList<String>();
		for(int i=0;i<s.length();i++) {
			if(s.charAt(i)=='-') {
				v.add(t);
				t="";
			}else {
				t=t+s.charAt(i);
			}
		}
		v.add(t);
		return v;
	}	
	
	static void cater_to_DONENOTFOUND_from_server(String hash) {
		System.out.println("(REPLY FROM SERVER: DONE NOT FOUND the password of hash: "+hash+")");
	}
	static void cater_to_DONEFOUND_from_server(String hash, String password) {
		System.out.println("(REPLY FROM SERVER: DONE FOUND the password of hash: "+hash+". Password: "+password+")");
	}	
	static void cater_to_NOTDONE_from_server(String hash) {
		System.out.println("(REPLY FROM SERVER: NOT DONE finding the password of hash: "+hash+")");
	}		
	
	static void sendMessage(String msg, String port) {
		try{
			byte[] buffer = msg.getBytes();
			InetAddress address = InetAddress.getByName("localhost");
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Integer.parseInt(port));
			socket.send(packet);
		}catch(Exception e) {
			System.out.println("Error in sendMessage()");
		}
	}	
	static void send_JOB_to_server(String hash){
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-JOB-").concat("start").concat("-").concat("end").concat("-").concat(hash);
		sendMessage(msgToSend,serverport);	
		System.out.println("JOB sent to server with hash:"+hash);
	}
	static void send_PING_to_server(String hash) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-PING-").concat("start").concat("-").concat("end").concat("-").concat(hash);
		sendMessage(msgToSend,serverport);	
		System.out.println("PING sent to server with hash: "+hash);
	}
	static String findHash(String s) {
 		MessageDigest md=null;
 		try{
 			md = MessageDigest.getInstance("MD5");
 		}catch(Exception e){

 		}
        md.update(s.getBytes());
 
        byte byteData[] = md.digest();
 
        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
         sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
		return sb.toString();
	}	
	
	static void send_CANCELJOB_to_server(String hash){
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-CANCELJOB-").concat("start").concat("-").concat("end").concat("-").concat(hash);
		sendMessage(msgToSend,serverport);			
	}
	
}