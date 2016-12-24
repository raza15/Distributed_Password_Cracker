// cd Desktop/NetCen- 3/
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
//port = 8000 and backwards
public class Worker3 extends Thread {
	static DatagramSocket socket;
	static int myPort;
	static String serverport;
	static int canceljob=0;

	public void run() {
		String receivedMsg = receivePacket();
		(new Worker3()).start();
		List<String> receivedList = seperateWrtDash(receivedMsg);
		String command=receivedList.get(2);
		System.out.println("(REQUEST RECEIVED: "+receivedMsg+")");
		if(command.equals("JOB")) {
			String hash=receivedList.get(5);
			String start=receivedList.get(3);
			String end=receivedList.get(4);
			send_ACKJOB_to_server(start,end,hash);
			
			String password=findPassword(start,end,hash);
			
			
			if(password.equals("-1")) {
				send_DONENOTFOUND_to_server(start,end,hash);
			}else{
				send_DONEFOUND_to_server(start,end,hash,password);
			}
			
		}else if(command.equals("PING")){
			String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-REPLY_PING_FROM_WORKER-").concat("start").concat("-").concat("end").concat("-").concat("hash");
			sendMessage(msgToSend,serverport);
		}else if(command.equals("CANCELLJOB")) {
			String start=receivedList.get(3);
			String end=receivedList.get(4);			
			String hash=receivedList.get(5);
			canceljob=1;
		}
	}
	static void sleep(int sec) {
		try {
			Thread.sleep(sec*1000);                
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}		
	}
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		initializeGlobals();
		(new Worker3()).start();
		send_JOIN_to_server();
		
		while(true){
			System.out.println("Enter 0-Exit:");
			int i=sc.nextInt();
			if(i==0) {
				break;
			}
		}		
	}
	static void initializeGlobals() {
		myPort=7998;
		try {
			socket = new DatagramSocket(myPort);
		}catch(Exception e) {
			System.out.println("Error in initializeGlobals()");
		}		
		serverport="8001";
	}	
	
	static String receivePacket() {
		byte[] buffer = new byte[2000];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);;
		String received = "";
		try {
			socket.receive(packet);
			received = new String(packet.getData(),0,packet.getLength());
		} catch(Exception e) {	
			System.out.println("Error in receivePacket()");
		}		
		return received;
	}
	 
	static String changeString(String old,char c, int i) {
		//replaces 'c' at index 'i' in 'oldString'
		String _new="";
		try{
			_new = old.substring(0,i)+c+old.substring(i+1);
		}catch(Exception e) {
			System.out.println("Error in changeString()");
		}
		return _new;
	}
	static char nextElementFromArray(char current) {
		List<Character> array = Arrays.asList('a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','0','1','2','3','4','5','6','7','8','9');
		//assume:'current' present in array
		int i = array.indexOf(current);
		if(current=='9') {
			return array.get(0);
		}else{
			return array.get(i+1);
		}
	}
	
	static String nextNumber(String number) {
		if(number.charAt(4)=='9') {
			if(number.charAt(3)=='9'){
				if(number.charAt(2)=='9') {
					if(number.charAt(1)=='9') {
						if(number.charAt(0)=='9') {
							return number;
						}else{
							number = changeString(number,
								nextElementFromArray(number.charAt(0)),0);
							number = changeString(number,'a',1);
							number = changeString(number,'a',2);
							number = changeString(number,'a',3);
							number = changeString(number,'a',4);							
						}
					}else{
						number=changeString(number,
							nextElementFromArray(number.charAt(1)),1);
						number=changeString(number,'a',2);
						number=changeString(number,'a',3);
						number=changeString(number,'a',4);
					}
				}else{
					number=changeString(number,
						nextElementFromArray(number.charAt(2)),2);
					number=changeString(number,'a',3);
					number=changeString(number,'a',4);					
				}
			}else{
				number=changeString(number,
					nextElementFromArray(number.charAt(3)),3);
				number=changeString(number,'a',4);								
			}
		}else{
			number=changeString(number,
				nextElementFromArray(number.charAt(4)),4);			
		}
		return number;
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
	static String findPassword(String start,String end,String hash) {
		String p=start;
		while(true){
			if(canceljob==1){
				canceljob=0;
				return "-1";
			}
			if(findHash(p).equals(hash)) {
				return p;
			}
			if(p.equals(end)) {
				return "-1"; //not found
			}
			p=nextNumber(p);
		}
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
	static void sendMessage(String msg, String port) {
		try{
			byte[] buffer = msg.getBytes();
			InetAddress address = InetAddress.getByName("localhost");
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Integer.parseInt(port));
			socket.send(packet);
		}catch(Exception e) {

		}
		System.out.println("(MSG SENT: "+msg+")");
	}	
	static void send_DONENOTFOUND_to_server(String start,String end,String hash) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-DONENOTFOUND-").concat(start).concat("-").concat(end).concat("-").concat(hash);
		sendMessage(msgToSend,serverport);
	}
	static void send_DONEFOUND_to_server(String start,String end,String hash,String password) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-DONEFOUND-").concat(start).concat("-").concat(end).concat("-").concat(hash).concat("-").concat(password);
		sendMessage(msgToSend,serverport);
	}		
	static void send_ACKJOB_to_server(String start,String end,String hash) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-ACKJOB-").concat(start).concat("-").concat(end).concat("-").concat(hash);
		sendMessage(msgToSend,serverport);		
	}	
	
	static void send_JOIN_to_server() {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-JOIN-").concat("start").concat("-").concat("end").concat("-").concat("hash");
		sendMessage(msgToSend,serverport);
	}
	
}