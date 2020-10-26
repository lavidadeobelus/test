// Version 1.0.1
// commit pull push test 확인중 
// GUI 화면
import java.awt.Button;
import java.awt.Frame;
import java.awt.TextArea;
// 이벤트 처리
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
// 입출력
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
//improt java.neet.*; // TCP 소켓 
// TCP 소켓 
import java.net.ServerSocket;
import java.net.Socket;
// Vector 클래스
import java.util.Vector;

class GUIChatServer extends Frame implements ActionListener {
	Button btn_ext; // 종료 버튼
	TextArea txt_list; // 접속 목록 출력
	protected Vector list; // 접속한 서버 목록 저장

	// 생성자
	public GUIChatServer(String title) {
		super(title); // 타이틀바에 출력될 문자열
		list = new Vector(); // 벡터 생성
		btn_ext = new Button("서버 종료"); // 서버 종료 버튼 생성
		btn_ext.addActionListener(this); // 이벤트 등록
		txt_list = new TextArea(); // txt_list 생성
		add("Center", txt_list); // 화면 가운데 txt_list 출력
		add("South", btn_ext); // 화면 하단에 서버 종료 버튼 출력
		setSize(400, 300); // 화면 크기 설정
		setVisible(true); // 화면 출력
		ServerStart(); // 채팅 서버 시작
	}

	// 채팅 서버
	public void ServerStart() {
		final int port = 5005; // 채팅 서버 포트 상수 지정
		try {
			ServerSocket ss = new ServerSocket(port); // ServerSocket 생성
			while (true) {
				Socket client = ss.accept(); // 클라이언트 접속 기다림
				txt_list.appendText(client.getInetAddress().getHostAddress() + "\n");
				ChatHandle ch = new ChatHandle(this, client); // ChatHandle 초기화
				list.addElement(ch); // 클라이언트 관리 list 벡터에 추가
				ch.start(); // ChatHandle 스레드 시작
			}
		} catch (Exception e) { // 예외 처리
			System.out.println(e.getMessage());
		}
	}

	// 서버 종료 버튼이 눌렸을 때
	public void actionPerformed(ActionEvent e) {
		System.exit(0);
	}

	// 메시지 출력 메서드
	public void setMsg(String msg) {
		txt_list.appendText(msg + "\n");// 화면에 msg 메시지 출력
	}

	// main 함수
	public static void main(String[] args) {
		new GUIChatServer("채팅 서버 ");
	}
}

// ChatHandle 클래스 : 채팅 서버의 실질적인 역할 처리

class ChatHandle extends Thread { // 스레드 상속
	GUIChatServer server = null; // GUIChatServer 멤버 변수
	Socket client = null; // 접속한 클라이언트
	BufferedReader br = null; // 읽어오기
	PrintWriter pw = null; // 보내기

	// 생성자
	public ChatHandle(GUIChatServer server, Socket client) throws IOException {
		this.server = server;
		this.client = client;
		// 입출력 스트림 생성
		InputStream is = client.getInputStream();
		br = new BufferedReader(new InputStreamReader(is));
		OutputStream os = client.getOutputStream();
		pw = new PrintWriter(new OutputStreamWriter(os));
	}

	// 현재 서버에 접속한 모든 클라이언트에 msg전송
	public void Send_All(String msg) {
		try {
			synchronized (server.list) { // GUIChatServer 멤버 변수 list 동기화 처리
				int size = server.list.size(); // 현재 접속한 클라이언트 수
				for (int i = 0; i < size; i++) {
					ChatHandle chs = (ChatHandle) server.list.elementAt(i);
					synchronized (chs.pw) { // ChatHandle pw 인스턴스를 이용한 문자 전송
						chs.pw.println(msg);
					}
					chs.pw.flush();
				}
			}
		} catch (Exception e) { // 예외 처리
			System.out.println(e.getMessage());
		}
	}

	// Thread 클래스의 run 메서드 오버라이딩.
	public void run() {
		String name = "";
		try {
			name = br.readLine(); // 대화명 읽어오기
			Send_All(name + " 님이 새로 입장하셨습니다.");
			while (true) {
				String msg = br.readLine(); // 클라이언트 메시지 대기
				String str = client.getInetAddress().getHostName();
				synchronized (server) {
					server.setMsg(str + " : " + msg); // 접속 클라이언트 메시지 출력
				}
				if (msg.equals("@@Exit"))// @@Exit 메시지면 클라이언트 접속 해지
					break;
				else // 현재 접속한 모든 클라이언트에 메시지 전송
					Send_All(name + " >> " + msg);
			}
		} catch (Exception e) { // 예외 처리
			server.setMsg(e.getMessage());
		} finally {
			synchronized (server.list) {
				server.list.removeElement(this); // 접속 목록에서 제거
			}
			try { // 스트림 해지
				br.close();
				pw.close();
				client.close(); // 클라이언트 접속 해지
			} catch (IOException e) { // 예외 처리
				server.setMsg(e.getMessage());
			}
		}
	}
}