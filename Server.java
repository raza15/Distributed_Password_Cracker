// cd Desktop/NetCen- 3/
import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {
	static DatagramSocket socket;
	static int myPort;
	static List<String> free_worker_ids;
	static List<String> free_worker_ports;
	static Object lock_freeworkers=new Object();

	static List<String> ports_of_requester_currenthashes;
	static List<String> currenthashes;
	static ArrayList<ArrayList<String>> processedranges_currenthashes;
	static List<String> password_currenthashes;
	static Object lock_currenthashes=new Object();
	
	static List<String> workerAcknowlegments;
	static Object lock_workerAcknowlegments=new Object();

	static List<String> currently_working_workers_port=new ArrayList<String>(); //list of ports of workers currently working on hashes
	static List<String> currently_working_workers_startendranges=new ArrayList<String>(); //if 'i' is the index in currently_working_workers_port, then start range=currently_working_workers_startendranges[2*i] and endrange=currently_working_workers_startendranges[2*i+1]
	static List<String> currently_working_workers_hash=new ArrayList<String>();
	static Object lock_currently_working_workers=new Object();
	static List<String> currently_working_workers_acknowledgement = new ArrayList<String>();


	static void write_List_to_file(List<String> list, String file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);   
			oos.writeObject(list); // write MenuArray to ObjectOutputStream
			oos.close(); 
		} catch(Exception ex) {
			ex.printStackTrace();
		}		
	}
	
	static void exit() {
		
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

	//-----------------------------
	//called when a request from request client comes to 
	//process a hash.
	static String[] removeFreeWorker(){
		String workerid;
		String workerport;
		synchronized(lock_freeworkers) {
			while(free_worker_ids.size()==0) {
				try{
					lock_freeworkers.wait();
				}catch(Exception e) {}
			}
			workerid=free_worker_ids.remove(0);
			workerport=free_worker_ports.remove(0);
		}
		String[] _return=new String[] {workerid,workerport};
		return _return;
	}
	static void appendFreeWorker(String port) {
		synchronized(lock_freeworkers) {
			int workerid=free_worker_ids.size();
			free_worker_ids.add(Integer.toString(workerid));
			free_worker_ports.add(port);
			lock_freeworkers.notify();
		}
	}
	static void printFreeWorkers() {
		synchronized(lock_freeworkers) {
			System.out.println("Free Workers:");
			for(int i=0;i<free_worker_ids.size();i++) {
				System.out.println(free_worker_ids.get(i)+","+free_worker_ports.get(i));
			}
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
	}
	static void send_JOB_to_worker(String start,String end,String hash,String workerid,String workerport) {
		String msgToSend="1234-0000-JOB-".concat(start).concat("-").concat(end).concat("-").concat(hash);
		sendMessage(msgToSend,workerport);
		// System.out.println("JOB sent to worker. worker id:"+workerid+", worker port:"+workerport+", start range:"+start+", end range:"+end+", hash:"+hash);
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
	
	static String[] get_array_start_end_ranges() {
		// List<String> list = new ArrayList<String>();
		// int numberOfPasswordsToTryForEachThread=(int)Math.ceil((11*11*11*11*11)/5); //<- //no. of threads working on a request=5. //divide a job into 5 equal pieces
		// String start="00000";  //<-
		// String end=start;
		// for(int k=0;k<5;k++) {
		// 	for(int i=0;i<numberOfPasswordsToTryForEachThread;i++) {
		// 		System.out.println(end);
		// 		end=nextNumber(end);
		// 	}
		// 	System.out.println(start);
		// 	System.out.println(end);
		// 	list.add(start); list.add(end);
		// 	start=end;
		// 	end=start;
		// }
		// String[] array = list.toArray(new String[list.size()]);
		// System.out.println(Arrays.toString(array));
		String[] array = {"aaaaa","m9999","m9999","y9999","y9999","K9999","K9999","W9999","W9999","99999"};
		return array;	
	}
	static void sleep(int sec) {
		try {
			Thread.sleep(sec*1000);                
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}		
	}
	static void deleteFreeWorker(String workerid,String workerport){
		synchronized(lock_freeworkers) {
			boolean bool = free_worker_ids.remove(workerid);
			bool=free_worker_ports.remove(workerport);
		}
		System.out.println("Worker unresponsive. Thus Removed! Id: "+workerid+". Port: "+workerport);
	}

	static String[] removeFreeWorker_All_max5(){
		List<String> myList = new ArrayList<String>();
		String workerid;
		String workerport;
		synchronized(lock_freeworkers) {
			while(free_worker_ids.size()==0) {
				try{
					lock_freeworkers.wait();
				}catch(Exception e) {}
			}
			if(free_worker_ids.size()<=5) {
				// System.out.println("size freeworkerids: "+free_worker_ids.size());
				int s = free_worker_ids.size();
				for(int i=0;i<s;i++) {
					workerid=free_worker_ids.remove(0);
					workerport=free_worker_ports.remove(0);
					myList.add(workerid);
					myList.add(workerport);	
				}
			}else{
				for(int i=0;i<5;i++) {
					workerid=free_worker_ids.remove(0);
					workerport=free_worker_ports.remove(0);
					myList.add(workerid);
					myList.add(workerport);	
				}
			}
		}
		String[] stockArr = new String[myList.size()];
		stockArr = myList.toArray(stockArr);
		System.out.println("free workers to which job sent:"+Arrays.toString(stockArr));
		// String[] _return=new String[] {workerid,workerport};
		return stockArr;
	}

	static void processHash(String hash) {
		int index;
		synchronized(lock_currenthashes){
			index = currenthashes.indexOf(hash);
		}		
		
		String[] array_start_end_ranges=get_array_start_end_ranges();
		//since sending to max 5 workers
		String[] freeworker = removeFreeWorker_All_max5();
		if(freeworker.length==2){
			synchronized(lock_currenthashes){
				// i = currenthashes.indexOf(hash);
				for(int i=1;i<=4;i++){
					processedranges_currenthashes.get(index).add("start");
					processedranges_currenthashes.get(index).add("end");				
				}
			}	

			String[] array = {"aaaaa","99999"};
			array_start_end_ranges=array;		
		}else if(freeworker.length==4){
			synchronized(lock_currenthashes){
				// i = currenthashes.indexOf(hash);
				for(int i=1;i<=3;i++){
					processedranges_currenthashes.get(index).add("start");
					processedranges_currenthashes.get(index).add("end");				
				}
			}

			String[] array = {"aaaaa","F9999","F9999","99999"};
			array_start_end_ranges=array;
		}else if(freeworker.length==6){
			synchronized(lock_currenthashes){
				// i = currenthashes.indexOf(hash);
				for(int i=1;i<=2;i++){
					processedranges_currenthashes.get(index).add("start");
					processedranges_currenthashes.get(index).add("end");				
				}
			}

			String[] array = {"aaaaa","u9999","u9999","O9999","O9999","99999"};
			array_start_end_ranges=array;
		}else if(freeworker.length==8){
			synchronized(lock_currenthashes){
				// i = currenthashes.indexOf(hash);
				for(int i=1;i<=1;i++){
					processedranges_currenthashes.get(index).add("start");
					processedranges_currenthashes.get(index).add("end");				
				}
			}

			String[] array = {"aaaaa","p9999","p9999","E9999","E9999","59999","59999","99999"};
			array_start_end_ranges=array;
		}else if(freeworker.length==10){

		}
		// System.out.println(freeworker.length);
		for(int i=0;i<freeworker.length/2;i++) {
			String workerid=freeworker[2*i];
			String workerport=freeworker[(2*i)+1];
			String start=array_start_end_ranges[2*i];
			String end=array_start_end_ranges[(2*i)+1];
			send_JOB_to_worker(start,end,hash,workerid,workerport);
			sleep(3);
			//-----------------------------edit
			int requestsenttoworker=0;

			String acknowledgement=workerport.concat("-").concat(hash).concat("-").concat(start).concat("-").concat(end);
			if(workerAcknowlegments.indexOf(acknowledgement)==-1) {//if no acknowledgement received then send the msg 2 more times and then remove the worker from freeworkers. and then as usual select the next worker from freeworkers
				send_JOB_to_worker(start,end,hash,workerid,workerport);
				if(workerAcknowlegments.indexOf(acknowledgement)==-1) {
					send_JOB_to_worker(start,end,hash,workerid,workerport);
					if(workerAcknowlegments.indexOf(acknowledgement)==-1) {
						//meaning the worker client is dead
						deleteFreeWorker(workerid,workerport);

						String[] one_freeworker=removeFreeWorker();
						freeworker[2*i]=one_freeworker[0];
						freeworker[2*i]=one_freeworker[1];

						i--;

					}else{
						requestsenttoworker=1;
					}
				}else{
					requestsenttoworker=1;	
				}
			}else{
				requestsenttoworker=1;
			}

			synchronized(lock_currently_working_workers) {
				if(requestsenttoworker==1){
					currently_working_workers_port.add(workerport);
					currently_working_workers_hash.add(hash);
					currently_working_workers_startendranges.add(start);
					currently_working_workers_startendranges.add(end);
				}	
			}		
		}
	}

	// static void processHash(String hash) {
	// 	int index;
	// 	synchronized(lock_currenthashes){
	// 		index = currenthashes.indexOf(hash);
	// 	}		
		
	// 	//password is 3 digit
	// 	String[] array_start_end_ranges=get_array_start_end_ranges();
	// 	//since sending to max 5 workers
	// 	for(int i=0;i<5;i++) {
	// 		synchronized(lock_currenthashes) {
	// 			if(password_currenthashes.get(index)=="CANCELLED_JOB") {
	// 				System.out.println("(JOB CANCELLED. hash: "+hash+")");
	// 				break;
	// 			}
	// 		}
	// 		String[] freeworker=removeFreeWorker();
	// 		String workerid=freeworker[0];
	// 		String workerport=freeworker[1];
	// 		String start=array_start_end_ranges[2*i];
	// 		String end=array_start_end_ranges[(2*i)+1];
	// 		send_JOB_to_worker(start,end,hash,workerid,workerport);
	// 		sleep(3);
	// 		//-----------------------------edit
	// 		String acknowledgement=workerport.concat("-").concat(hash).concat("-").concat(start).concat("-").concat(end);
	// 		if(workerAcknowlegments.indexOf(acknowledgement)==-1) {//if no acknowledgement received then send the msg 2 more times and then remove the worker from freeworkers. and then as usual select the next worker from freeworkers
	// 			send_JOB_to_worker(start,end,hash,workerid,workerport);
	// 			if(workerAcknowlegments.indexOf(acknowledgement)==-1) {
	// 				send_JOB_to_worker(start,end,hash,workerid,workerport);
	// 				if(workerAcknowlegments.indexOf(acknowledgement)==-1) {
	// 					//meaning the worker client is dead
	// 					deleteFreeWorker(workerid,workerport);
	// 					i--;
	// 				}
	// 			}
	// 		}
	// 	}
	// }
	static void initializeGlobals() {
		myPort=8001;
		try {
			socket = new DatagramSocket(myPort);
		}catch(Exception e) {
			System.out.println("Error in initializeGlobals()");
		}
		free_worker_ids = new ArrayList<String>();
		free_worker_ports = new ArrayList<String>();

		ports_of_requester_currenthashes=new ArrayList<String>();
		currenthashes = new ArrayList<String>(); //currenthahes is basically all the hashes processed/being processed //element added 
		processedranges_currenthashes = new ArrayList<ArrayList<String>>(); //p.s.when a request for a hash comes, currenthashes updated, processedranges_currenthashes is also appended by adding an empty list at that point and password_current hashes is appended by "-1"
		password_currenthashes = new ArrayList<String>();
		workerAcknowlegments = new ArrayList<String>();
	}
	static void printhashes(){
		System.out.println("Hashes: {");
		for(int i=0;i<currenthashes.size();i++) {
			System.out.print("hash= "+currenthashes.get(i)+". requester client's port= "+
				ports_of_requester_currenthashes.get(i)+
				". processedranges= ");
			printlist(processedranges_currenthashes.get(i));
			System.out.print(". password= "+password_currenthashes.get(i));
			System.out.print("\n");
		}
		System.out.print("}\n");
	}
	static int cater_to_JOB_from_requester(String requesterport, String hash) {
		//assume: different hashes come each time.
		int cond;
		synchronized(lock_currenthashes) {
			if(currenthashes.indexOf(hash)==-1) {
				cond=0;
				ports_of_requester_currenthashes.add(requesterport);
				currenthashes.add(hash);
				ArrayList<String> l = new ArrayList<String>();
				processedranges_currenthashes.add(l);
				password_currenthashes.add("-1");				
			}else if(password_currenthashes.get(currenthashes.indexOf(hash))=="CANCELLED_JOB") {
				cond=1;
				ArrayList<String> l = new ArrayList<String>();
				int ind=currenthashes.indexOf(hash);
				processedranges_currenthashes.set(ind,l);
				password_currenthashes.set(ind,"-1");			
			}else {
				cond=2;
			}
		}
		if(cond==2){
			cater_to_PING_from_requester(requesterport, hash);
			return 2;
		}
		return 1;
	}

	static void cater_to_DONENOTFOUND_from_worker(String start,String end,String hash,String workerport) {
		// boolean sendMsgToRequester=false;
		int i;
		synchronized(lock_currenthashes){
			i = currenthashes.indexOf(hash);
			processedranges_currenthashes.get(i).add(start);
			processedranges_currenthashes.get(i).add(end);
		}
		appendFreeWorker(workerport);//this func is already synchronized

		//update the currently working workers
		// System.out.println("in donenotfound: "+currently_working_workers_port);
		// System.out.println("in donenotfound again: "+currently_working_workers_startendranges);
		if(currently_working_workers_port.indexOf(workerport)==-1){
			sleep(3);
		}
		synchronized(lock_currently_working_workers){
			i=currently_working_workers_port.indexOf(workerport);
			String removed=currently_working_workers_port.remove(i);
			removed=currently_working_workers_hash.remove(i);
			removed=currently_working_workers_startendranges.remove(2*i);
			removed=currently_working_workers_startendranges.remove(2*i);
		}
	}

	//nclude: send cancel job to all other workers working on the job
	static void cater_to_DONEFOUND_from_worker(String start,String end,String hash,String password,String workerport) {
		synchronized(lock_currenthashes){
			int i = currenthashes.indexOf(hash);	
			processedranges_currenthashes.get(i).add(start);
			processedranges_currenthashes.get(i).add(end);
			if(password_currenthashes.get(i)!="CANCELLED_JOB") {
				password_currenthashes.set(i,password);
			}
		}
		appendFreeWorker(workerport);

		//update the currently working workers
		// System.out.println("in donefound: "+currently_working_workers_port);
		if(currently_working_workers_port.indexOf(workerport)==-1){
			sleep(3);
		}
		synchronized(lock_currently_working_workers){
			int i=currently_working_workers_port.indexOf(workerport);
			String removed=currently_working_workers_port.remove(i);
			removed=currently_working_workers_hash.remove(i);
			removed=currently_working_workers_startendranges.remove(2*i);
			removed=currently_working_workers_startendranges.remove(2*i);
		}		
	}
	static void printlist(List<String> l){
		System.out.print("[");
		for(int i=0;i<l.size();i++){
			System.out.print(l.get(i)+", ");
		}
		System.out.print("]\n");
	}
	static void printlist(ArrayList<String> l){
		System.out.print("[");
		for(int i=0;i<l.size();i++){
			System.out.print(l.get(i)+", ");
		}
		System.out.print("]");
	}	
	static void printlistoflist(ArrayList<ArrayList<String>> l) {
		System.out.print("{");
		for(int i=0;i<l.size();i++) {
			printlist(l.get(i));
		}
		System.out.print("}\n");
	}
	static void cater_to_JOIN_from_worker(String workerport) {
		appendFreeWorker(workerport);
	}
	static void send_DONENOTFOUND_to_requester(String hash,String requesterport) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-DONENOTFOUND-").concat("start").concat("-").concat("end").concat("-").concat(hash);
		sendMessage(msgToSend,requesterport);
	}
	static void send_DONEFOUND_to_requester(String hash,String requesterport,String password) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-DONEFOUND-").concat("start").concat("-").concat("end").concat("-").concat(hash).concat("-").concat(password);
		sendMessage(msgToSend,requesterport);
	}
	static void send_NOTDONE_to_requester(String hash,String requesterport) {
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-NOTDONE-").concat("start").concat("-").concat("end").concat("-").concat(hash);
		sendMessage(msgToSend,requesterport);
	}
	
	static void send_CANCELLEDJOB_to_requester(String hash,String requesterport){
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-CANCELLEDJOB-").concat("start").concat("-").concat("end").concat("-").concat(hash);
		sendMessage(msgToSend,requesterport);		
	}

	static void send_PING_to_worker(String workerport){
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-PING-").concat("start").concat("-").concat("end").concat("-").concat("hash");
		sendMessage(msgToSend,workerport);		
	}	
	
	static void cater_to_PING_from_requester(String requesterport, String hash) {
		boolean sendDONEToRequester=false;
		boolean cancelled_job = false;
		int i;
		synchronized(lock_currenthashes) {
			i = currenthashes.indexOf(hash);
			if(password_currenthashes.get(i)=="CANCELLED_JOB") {//------------------------------------
				cancelled_job=true;
			}//else if(processedranges_currenthashes.get(i).size()==10) { // b/c there are 5 worker threads to which the request was sent
				//sendDONEToRequester=true;
			//}
			else if(password_currenthashes.get(i)!="-1" || processedranges_currenthashes.get(i).size()==10) {
				sendDONEToRequester=true;
			}
		}
		if(cancelled_job==true) {
			send_CANCELLEDJOB_to_requester(hash,requesterport);
		} else if(sendDONEToRequester==true) {
			//send either donefound or donenotfound
			//dont need synchronization here coz processedranges_current.get(i) hashes will not be updated once this conidtion is true
			if(password_currenthashes.get(i).equals("-1")) {
				//send donenotfound
				send_DONENOTFOUND_to_requester(hash,requesterport);
			}else{
				//send done found
				send_DONEFOUND_to_requester(hash,requesterport,password_currenthashes.get(i));
			}		
		}else{
			//send not done yet.
			send_NOTDONE_to_requester(hash,requesterport);
		}
	}	
	static void cater_to_ACKJOB_from_requester(String workerport,String hash,String start,String end) {
		String acknowledgement=workerport.concat("-").concat(hash).concat("-").concat(start).concat("-").concat(end);
		synchronized(lock_workerAcknowlegments) {
			workerAcknowlegments.add(acknowledgement);
		}
		// System.out.println("(RECEIVED ACKJOB. workerport:"+workerport+". hash:"+hash+". start range: "+start+". end range:"+end+")");
	}

	static void send_cancelljob_to_worker(String workerport,String hash,String start,String end){
		String msgToSend="1234-".concat(Integer.toString(myPort)).concat("-CANCELLJOB-").concat(start).concat("-").concat(end).concat("-").concat(hash);
		sendMessage(msgToSend,workerport);				
	}
	
	static void cater_to_CANCELJOB_from_requester(String requesterport, String hash){
		synchronized(lock_currenthashes){
			int i = currenthashes.indexOf(hash);
			password_currenthashes.set(i,"CANCELLED_JOB");
		}

		//send cancel job to all the workers processing this hash
		List<String> ports = new ArrayList<String>();
		List<String> starts = new ArrayList<String>();
		List<String> ends = new ArrayList<String>();
		synchronized(lock_currently_working_workers) {
			for(int i=0;i<currently_working_workers_hash.size();i++){
				if(currently_working_workers_hash.get(i).equals(hash)) {
					ports.add(currently_working_workers_port.get(i)); //later reove from the currently working hash(this is handles when worker sends donenotfound on receiving canceljob)
					starts.add(currently_working_workers_startendranges.get(2*i));
					ends.add(currently_working_workers_startendranges.get(2*i+1));
				}
			}
		}

		for(int i=0;i<ports.size();i++){
			send_cancelljob_to_worker(ports.get(i),hash,starts.get(i),ends.get(i));
		}
	}
	
	public void run() {
		String receivedMsg = receivePacket();
		// System.out.println("(REQUEST RECEIVED: "+receivedMsg+")");
		(new Server()).start();
		List<String> receivedList = seperateWrtDash(receivedMsg);
		String command=receivedList.get(2);
		if(command.equals("DONENOTFOUND")) {
			// System.out.println("DONENOTFOUND received from worker");
			String workerport=receivedList.get(1);
			String hash=receivedList.get(5);
			String start=receivedList.get(3);
			String end=receivedList.get(4);
			cater_to_DONENOTFOUND_from_worker(start,end,hash,workerport);
		}else if(command.equals("DONEFOUND")) {
			// System.out.println("DONEFOUND received from worker");
			String workerport=receivedList.get(1);
			String hash=receivedList.get(5);
			String start=receivedList.get(3);
			String end=receivedList.get(4);
			String password=receivedList.get(6);
			cater_to_DONEFOUND_from_worker(start,end,hash,password,workerport);
		}else if(command.equals("JOB")) { //i.e.frm reqst client //include requestter clients global
			String hash=receivedList.get(5);
			String requesterport=receivedList.get(1);
			int ret=cater_to_JOB_from_requester(requesterport, hash);
			if(ret!=2){
				processHash(hash);
			}
			System.out.println("currently_working_workers_port: "+currently_working_workers_port);
		}else if(command.equals("JOIN")) { //from worker
			String workerport=receivedList.get(1);
			cater_to_JOIN_from_worker(workerport);
		}else if(command.equals("PING")) { //from requester
			String hash=receivedList.get(5);
			String requesterport=receivedList.get(1);
			cater_to_PING_from_requester(requesterport, hash);			
		}else if(command.equals("ACKJOB")) { //from requester
			String workerport=receivedList.get(1);
			String hash=receivedList.get(5);
			String start=receivedList.get(3);
			String end=receivedList.get(4);
			cater_to_ACKJOB_from_requester(workerport, hash,start,end);			
		}else if(command.equals("CANCELJOB")) {
			String hash=receivedList.get(5);
			String requesterport=receivedList.get(1);
			cater_to_CANCELJOB_from_requester(requesterport, hash);
		}else if(command.equals("REPLY_PING_FROM_WORKER")) {
			String workerport=receivedList.get(1);
			currently_working_workers_acknowledgement.add(workerport);
		}else if(command.equals("REASSIGN")) { //this is sent from myself
			String hash=receivedList.get(5);
			String start=receivedList.get(3);
			String end=receivedList.get(4);
			String i=receivedList.get(1);				
			reassign_job_to_another_worker(hash,start,end,i);
		}		
	}
	static List<String> to_be_reassigned_hashes=new ArrayList<String>();
	static List<String> to_be_reassigned_startend=new ArrayList<String>();
	static Object lock_to_be_reassigned=new Object();

	static void reassign_job_to_another_worker(String hash,String start,String end,String i){
		String[] freeworker=removeFreeWorker();
		String workerid=freeworker[0];
		String workerport=freeworker[1];
		send_JOB_to_worker(start,end,hash,workerid,workerport);
		System.out.println("REASSIGNED job! hash:"+hash+". start:"+start+". end:" +end+". to worker:"+workerport);
		//remove the job from to_be_assigned
		synchronized(lock_to_be_reassigned) {
			int ind=Integer.parseInt(i);
			String removed=to_be_reassigned_hashes.remove(ind);
			removed=to_be_reassigned_startend.remove(2*ind);
			removed=to_be_reassigned_startend.remove(2*ind);
		}
	}

	//------------------------------
	public static void main(String[] args) {
		initializeGlobals();
		(new Server()).start();
		Scanner sc = new Scanner(System.in);
		while(true) {
			System.out.println("1 to view freeworkers. 2 to view hashes. 0 Exit. 3 to see if all currently working workers are alive");
			int i=sc.nextInt();
			if(i==1) {
				printFreeWorkers();
			}else if(i==2) {
				printhashes();
			}else if(i==0) {
				exit();
				break;
			}else if(i==3) {
				//sending to see if alive
				for(int ij=0;ij<currently_working_workers_port.size();ij++) {
					String workerport=currently_working_workers_port.get(ij);
					String hash=currently_working_workers_hash.get(ij);
					send_PING_to_worker(workerport);
				}
				sleep(3);
				//checking who replied
				for(int ij=0;ij<currently_working_workers_port.size();ij++) {
					String workerport=currently_working_workers_port.get(ij);

					if(currently_working_workers_acknowledgement.indexOf(workerport)==-1){
						System.out.println("Workering worker not responding. workerport: "+workerport);
						//reassign that job to another free worker
						String hash=currently_working_workers_hash.get(ij);
						String start=currently_working_workers_startendranges.get(2*ij);
						String end=currently_working_workers_startendranges.get(2*ij+1);
						
						int index_of_this_hash=to_be_reassigned_hashes.size();
						to_be_reassigned_hashes.add(hash);
						to_be_reassigned_startend.add(start);
						to_be_reassigned_startend.add(end);
						//calling the reassigned from a thread so that it dowsnt block my main if theres no freeorker right nnow
						String msgToSend="1234-".concat(Integer.toString(index_of_this_hash)).concat("-REASSIGN-").concat(start).concat("-").concat(end).concat("-").concat(hash);
						sendMessage(msgToSend,Integer.toString(myPort));	
					}
				}
				//empty the alive? acks
				currently_working_workers_acknowledgement=new ArrayList<String>();
			}
		}
	}	
}

