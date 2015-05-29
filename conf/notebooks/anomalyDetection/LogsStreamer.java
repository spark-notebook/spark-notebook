
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class LogsStreamer {

    public static void main(String[] args) throws IOException, InterruptedException {

        FileReader fileReader = new FileReader("/Users/radek/lab/data/kddcup.testdata.unlabeled");
        //FileReader fileReader = new FileReader("/Users/radek/lab/data/kddcup.testdata.unlabeled_second_half");
        //FileReader fileReader = new FileReader("/Users/radek/lab/data/kddcup.testdata.unlabeled_10_percent");

        BufferedReader br = new BufferedReader(fileReader);

        ServerSocket servsock = new ServerSocket(9999);
            try {
                System.out.println("creating a socket...");
                System.out.println("ready to accept a new connection...");
                Socket sock = servsock.accept();
                OutputStream os = sock.getOutputStream();
                int count = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    byte[] mybytearray = (line + "\n").getBytes();
                    os.write(mybytearray, 0, mybytearray.length);
                    System.out.println(line);
                    if (count++ % 5000 == 0) {
                        System.out.println(count + " -- waiting --");
                        Thread.sleep(1000);
                        os.flush();
                    }
                }
                os.close();
                System.out.println("closing connection...");
                sock.close();
            }catch(IOException e){
                //e.printStackTrace();
            }finally{
                System.out.println("closing server socket");
                servsock.close();
            }
    }
}
