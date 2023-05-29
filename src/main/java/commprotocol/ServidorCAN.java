/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package commprotocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ppere
 */
public class ServidorCAN {
  
	
	 int puerto_de_escucha;
         int n_mensajes=0;
    SuperServer ss;
    static int ni=0;
    ArrayList<NodoCAN> NodosCAN = new ArrayList<NodoCAN>();
    ArrayList<String> msgReceived = new ArrayList<String>();
    SerTermGUI parent;
    
    
    
    ServerSocket sk;
        	
    boolean RUNSS;
    /* procesado de mensajes serie */
     String uCLineincourse="";
    
    
    public ServidorCAN(   SerTermGUI par)
    {
        parent=par;
        System.out.print("Instancia: "+ni++);
    }
    public String getIP()
    {
       InetAddress g1 = getLocalAddress();
       return g1.getHostAddress();
    }
    public void start_Superserver(int puerto)
    {
    	puerto_de_escucha=puerto;
        ss=new SuperServer();
    	ss.start();
    }
    synchronized public void ReceiveMessage(String msg){
        
        try {
            parent.setNumberMSG(++n_mensajes);
        
        /*String auxmsg=msg;
        auxmsg.replace(">R", ">")*/
        parent.sendmessageBYSERIE(msg);
           Thread.sleep(50);
        } catch (InterruptedException ex) {
                 Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
             }
// msgReceived.add(msg);
// msgReceived.add(msg);
    }

    synchronized public void ReceiveMessage(String msg,String IP){
        
             try {
                 parent.setNumberMSG(++n_mensajes);
                 /*String auxmsg=msg;
                 auxmsg.replace(">R", ">")*/
                 parent.sendmessageBYSERIE(msg,IP);
                 Thread.sleep(50);
                 // msgReceived.add(msg);
             } catch (InterruptedException ex) {
                 Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
             }
    }
    public void KillServer()
    {
        RUNSS=false;
             try {
                 sk.close();
             } catch (IOException ex) {
                 Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
             }
        
        for (int t=0;t<NodosCAN.size();t++)
        {
            NodosCAN.get(t).kill();
            if (NodosCAN.size()>0) NodosCAN.remove(t);
        }
        parent.setNumberConections(0);
             
    }
    
   /* public int getSize(){return msgReceived.size();};
    public String PeekMessage()
    {
        String s=msgReceived.get(0);
        msgReceived.remove(0);
        return s;
    }
    */
    
public void recibir(byte[] buffer)
{
    String cadenabuffer=new String(buffer);
 //   boolean interpret=false;
    
    int numBytes;
    int indbuffer=0;
    int charconsumed=0;
   
   // String uCLineincourse="";
   
                           
   
            numBytes=buffer.length;
            boolean wcr=false;
            String bufaux=new String(buffer);
            while (bufaux.length()<numBytes) bufaux="*"+bufaux;
            
            System.out.println("buffer "+bufaux+"  "+buffer.length+"   "+numBytes+"   "+bufaux.length());
            //  convert to string of size numBytes                                 
            String str= bufaux.substring(0,numBytes);
            str=str.replace('\r','\n'); //replace CR with Newline
            
          //  gui.gettextWin().append(str);
          //  gui.gettextWin().setCaretPosition(gui.gettextWin().getDocument().getLength());
                               
            if (uCLineincourse.length()!=0)
            {
                str=uCLineincourse+str;
                uCLineincourse="";
            }
                                      
            int c=0;
            int t=0;
            
            do {
                t=str.indexOf("\n",c); 
                if (t>=0) {// al menos hay una linea de fichero hex
                  
                    String cad=str.substring(c,t+1);
                    if (cad.contains(">R"))
                    {
                        String aux=cad.replace(">R",">");
                        SendMessage(aux);
                    }   
                    str=str.substring(t+1,str.length());
                 
                }
                else{
                    uCLineincourse=str.substring(c);
                }
            } while(t>=0);

}

    
    public void SendMessage(String msg)
    {

        parent.setNumberMSG(++n_mensajes);
        
        for (int t=0;t<NodosCAN.size();t++)
	{
	 NodoCAN nodo=NodosCAN.get(t);
	 nodo.salida.write(msg+"\r\n");
	 nodo.salida.flush();
        }
    }
    
    class SuperServer extends Thread
    {
    		
            public void run() {
                
                    try {
                    	InetAddress g1 = getLocalAddress();
                        System.out.println("ServerCAN "+g1.getHostAddress()+":"+puerto_de_escucha);
                        sk = new ServerSocket(puerto_de_escucha, 0, g1);
                        RUNSS=true;
                        while (RUNSS) {
  
                            
                            while (!sk.isClosed()) {
                            
                            	Socket cliente = sk.accept();
                            	if (!sk.isClosed())
                                {        System.out.println("Nueva ConexiÃ³n");
                                        NodoCAN a=new NodoCAN(cliente);
                                        NodosCAN.add(a);
                                        a.start();                                
                                        parent.setNumberConections(NodosCAN.size());
                                }
                            }   
                            Thread.sleep(100);
                        }
                    } catch (IOException e) { System.out.println(e);
                    } catch (InterruptedException e) {  e.printStackTrace();}
                try {
                    
                    sk.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
                }
            } //run
            
    }
	
    class NodoCAN extends Thread {
    	
    	Socket con;
    	BufferedReader entrada;
        PrintWriter salida;
        boolean RUNNC=true;
    	
    	public NodoCAN(Socket cliente)
      
    	{
    		System.out.println("Nuevo Nodo "+cliente.getInetAddress().getHostAddress());
    		try {
				entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
				salida = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream()), true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		con=cliente;
        }
    	public void kill()
        {
            RUNNC=false;
                try {
                   
                    con.close();
                    
                    //entrada.close();
                    
                } catch (IOException ex) {
                    Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
                }
            //salida.close();
            //salida.close();
        }
        
            @SuppressWarnings("empty-statement")
        public void run() {
        	while(RUNNC)
        		
        	{
        		String msg;
        		try {
				        msg=null;
                                        int l=1;
                                       // do 
                                        //{
                                       
                                          //      if (entrada.ready())
                                        //    {
                                                    l=1;
                                                
                                                    msg=entrada.readLine();
                                                
                                                l=3;
                                          //  }
                                            //else 
                                            //{   
                                            //    l=2;
                                            //    Thread.sleep(10);
                                            //}   
                                                                                   
                                        //System.out.println(""+l+":"+msg);
                                        //Thread.sleep(500);
                                        //} while ((RUNNC)&&(l==2)); 
                                        
					if (msg!=null)
					{
        			         System.out.println("Mensaje "+msg+ " from "+con.getInetAddress().getHostAddress());
                                        ReceiveMessage(msg,con.getInetAddress().getHostAddress());
                                        
					for (int t=0;t<NodosCAN.size();t++)
					{
						NodoCAN nodo=NodosCAN.get(t);
						if (nodo!=this)
						{
							System.out.println("Enviando "+msg+ "to "+nodo.con.getInetAddress().getHostAddress());
							nodo.salida.write(msg+"\r\n");
							nodo.salida.flush();
						}
					}
					}
					
					else
					{
						//el socket se ha cerrado
						System.out.println("La direcciÃ³n: "+con.getInetAddress().getHostAddress()+" se ha desconectado: "+NodosCAN.size());
						NodosCAN.remove(this); // lo quita de la lista y cierra el hilo, posteriormente el recolector deberÃ¡ borrar el objeto
						parent.setNumberConections(NodosCAN.size());
                                                System.out.println("La direcciÃ³n: "+con.getInetAddress().getHostAddress()+" se ha desconectado: "+NodosCAN.size());
						
						break;
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("he llegao");
                                        e.printStackTrace();}
                        catch (Exception e)
                        {
                               
                             
                                System.out.println(e);
                                   return;
                                                
                        }
            }
        }

    }
    

	  
    private static InetAddress getLocalAddress() {
        InetAddress inetAddr = null;

        /**
         * 1) If the property java.rmi.server.hostname is set and valid, use it
         */
        
        

try{
  final DatagramSocket socket = new DatagramSocket();
  socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
  String ip=socket.getLocalAddress().getHostAddress();
  System.out.println("ooo "+ip);
  return socket.getLocalAddress();
}            catch (SocketException ex) {
                 Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
             } catch (UnknownHostException ex) {
                 Logger.getLogger(ServidorCAN.class.getName()).log(Level.SEVERE, null, ex);
             }
        
        
        try {
            System.out
                    .println("Attempting to resolve java.rmi.server.hostname");
            String hostname = System.getProperty("java.rmi.server.hostname");
            System.out.println("HN "+hostname);
            if (hostname != null) {
                inetAddr = InetAddress.getByName(hostname);
                if (!inetAddr.isLoopbackAddress()) {
                    return inetAddr;
                } else {
                    System.out
                            .println("java.rmi.server.hostname is a loopback interface.");
                }

            }
        } catch (SecurityException e) {
            System.out
                    .println("Caught SecurityException when trying to resolve java.rmi.server.hostname");
        } catch (UnknownHostException e) {
            System.out
                    .println("Caught UnknownHostException when trying to resolve java.rmi.server.hostname");
        }

        /** 2) Try to use InetAddress.getLocalHost */
        try {
            System.out
                    .println("Attempting to resolve InetADdress.getLocalHost");
            InetAddress localHost = null;
            localHost = InetAddress.getLocalHost();
            System.out.println("kkkk  "+localHost);
            
            if (!localHost.isLoopbackAddress()&& (localHost.getHostAddress().contains("158."))) {
                return localHost;
            } else {
                System.out
                        .println("InetAddress.getLocalHost() is a loopback interface.");
            }

        } catch (UnknownHostException e1) {
            System.out
                    .println("Caught UnknownHostException for InetAddress.getLocalHost()");
        }

        /** 3) Enumerate all interfaces looking for a candidate */
        Enumeration ifs = null;
        try {
            System.out
                    .println("Attempting to enumerate all network interfaces");
            ifs = NetworkInterface.getNetworkInterfaces();
            
            // Iterate all interfaces
            while (ifs.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) ifs.nextElement();
                System.out.println("*"+iface.getDisplayName());
                // Fetch all IP addresses on this interface
                Enumeration ips = iface.getInetAddresses();

                // Iterate the IP addresses
                while (ips.hasMoreElements()) {
                    InetAddress ip = (InetAddress) ips.nextElement();
                    System.out.println(" "+ips+"   "+ip);
                    if ((ip instanceof Inet4Address) && !ip.isLoopbackAddress() && ip.getHostAddress().contains("158.")) {
                    //  if ((ip instanceof Inet4Address) && !ip.isLoopbackAddress()) {
                        return (InetAddress) ip;
                    }
                }
            }
        } catch (SocketException se) {
            System.out.println("Could not enumerate network interfaces");
        }

        /** 4) Epic fail */
        System.out
                .println("Failed to resolve a non-loopback ip address for this host.");
        return null;
    }

}


  

