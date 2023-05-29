/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package commprotocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPortException;

/**
 *
 * @author ppere
 */
public class Motor {
    SerTermGUI gui;
    
    boolean SendingFile=false;
    boolean SendingFileStopper=true;

    String uCRowProgrammed;
    String uCRowtobeProgrammed;
    String uCLineincourse="";
    
    int offset;
        Thread SendingFileThread;
        Thread stressThread;
    
   boolean uC_CRCOK=true;
    public boolean stress_msg_on;
    public boolean PID_msg_on;
    
    
      public Motor(SerTermGUI ventana) 
      {
          offset=0;
          gui=ventana;          
      }
      
public int cancelar()
{
   Thread tr=new Thread(){
   public void run() {
   
      SendingFileStopper=true;
          
      while(true)
      {
          if (!SendingFileStopper) {
              gui.gettextWin().append("\n\nThread envio terminado\n");
              return ;
          }
              
          try {
              Thread.sleep(100);
          } catch (InterruptedException ex) {
              Logger.getLogger(Motor.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
   }
        };
   tr.start();
   return 0;
}

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
                    str=str.substring(t+1,str.length());
                    System.out.println("");
                    System.out.println("************* Analizando "+cad+ "  Resto:"+str);
                    
                    if (cad.contains("<<PgRow")) 
                    {
                        
                        System.out.println("detectado programando fila");
                        uCRowtobeProgrammed=str;
                    }
                    if (cad.contains("<<okRow")) {
                        System.out.println("detectado fila correcta");
                        uCRowtobeProgrammed=null;
                    }
                    if (cad.contains("<<CRCOK")) uC_CRCOK=true;
                    uCLineincourse="";
                }
                else{
                    uCLineincourse=str.substring(c);
                }
            } while(t>=0);

}

public void enviarfichero(final File HexFile)
{
        gui.gettextWin().append("Resetting state machine\n");
        uCLineincourse="";            
        gui.gettextField2().setText(""+gui.Totalsize());
            
            SendingFileThread=new Thread(){
              public void run() {
                  int sizeact=0;
                  int ct;
                  int reint=0;
                   byte[] lineabin;
                      String line="";
                   try{
                       BufferedReader br=new BufferedReader(new FileReader(HexFile));
                      
                      
                      while (!SendingFileStopper)
                      {
                       
                          
                         ct=0;
                                   
                         while ((uCRowtobeProgrammed!=null)&&(!SendingFileStopper)) // esperando que la pagina se programe
                          {
                            if (ct==0) System.out.println("Esperando que se programe la fila");
                         
                             if ((ct%1000)==0) System.out.println("parado");
                              if (ct>5000){
                                            System.out.println("parando desde primer nivel");
                                            SendingFileStopper=true;
                                            continue;
                                          }
                              ct++;
                              try {

                                      Thread.sleep(1);
                                  } catch (InterruptedException ex) {
                                      Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                                 }
                              
                          }
                          ct=0;
                          
                          while ((!uC_CRCOK)&&(!SendingFileStopper))
                          {
                              if (ct==0) System.out.println("Esperando que CRCOK");
                         
                              if ((ct%50)==0) System.out.println("wait CRCOK  "+uC_CRCOK);
                              if ((ct==10)||(ct==100)||(ct==150)){
                                    System.out.println("Reintento transmitir");
                                     
                                  
                              
                                     lineabin=line.getBytes();
                                     gui.Comm.serialPort.writeBytes(lineabin);
                              }
                              if (ct>600){
                                            SendingFileStopper=true;
                                            continue;
                                          }
                              
                              ct++;
                              try {
                                    Thread.sleep(10);
                                  } catch (InterruptedException ex) {
                                      Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                                 }
                          }
                           
                   
                          if (!SendingFileStopper)
                          {
                          line=br.readLine();
                          if (line!=null)
                          {
                              String sizeline=line.substring(1,3);
                              sizeact=sizeact+1+2+4+2+Integer.parseInt(sizeline, 16)*2+2+1; // trama hex     
                              gui.gettextField1().setText(""+sizeact);
                          }
                             
                          //System.out.println("Nueva linea "+line);
                          if ((line)!=null){
                              if (gui.Comm.serialPort!=null)
                              {
                               if (gui.Comm.serialPort.isOpened())
                               {
                                 
                                  uC_CRCOK=false;
                                  
                                   
                                    line=line+"\n";
                                    //System.out.println(line);
                                    lineabin=line.getBytes();
                                    System.out.println("Primer intento de transmisión");
                         
                                    gui.Comm.serialPort.writeBytes(lineabin);
                                  
                                /*  try {
                                      Thread.sleep(1);
                                  } catch (InterruptedException ex) {
                                      Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                                  }*/
                               }
                              }
                              else
                                  SendingFileStopper=true;
                              
                          }
                          else
                              SendingFileStopper=true;
                          }
                      }
                      
                  
              
                      
                   }catch (FileNotFoundException ex) {
                      Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                  } catch (IOException ex) {
                      Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                  } catch (SerialPortException ex) {
                      Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                  }
                 SendingFile=false;
                 gui.gettextWin().append("Sending Thread stopped\n");
                 System.out.println("finishing thread for send file");
                 gui.dereferencemotor();
              }
          };
        
        SendingFileStopper=false;
        SendingFile=true;
        SendingFileThread.start();
   }

String inttostring(int value,int length){
   
    String cad=Integer.toHexString(value);
    
    for (int t=0;t<length;t++){
        if (cad.length()==length) break;
        cad="0"+cad;        
    }
    
    return cad.toUpperCase();
        
        
    
}

public void stress_msg(int msg_s)
{
  
        gui.gettextWin().append("Sensding stress msg\n");
        
     
        
            
            stress_msg_on=true;
            stressThread=new Thread(){
                
                int ct=0;
                String msg;
                int ID;
                int Length;
                int Data[]=new int[8];
                Random rand = new Random(); //instance of random class        
           
                
                
              public void run() {
              
                  
                  
                  while(stress_msg_on){
                     
                      ID=rand.nextInt(2048); 
                      Length=rand.nextInt(8);
                      
                      ct=ct+1;
                      
                      Data[0]=((ct&0xFFFFFFFF)>>24)&0xFF;
                      Data[1]=((ct&0xFFFFFFFF)>>16)&0xFF;
                      Data[2]=((ct&0xFFFFFFFF)>>8)&0xFF;
                      Data[3]=((ct&0xFFFFFFFF))&0xFF;                              
                      
                      if (Length>4)
                           for (int t=4;t<Length;t++)
                              Data[t]=rand.nextInt(255);
                      else
                          Length=4;
                      msg=">";
                      msg=msg+inttostring(ID,4);
                      msg=msg+inttostring(Length,2);
                      for (int t=0;t<Length;t++)
                           msg=msg+inttostring(Data[t],2);
                      msg=msg+"<";
                      
                      gui.sendmessageBYSERIE(msg);
                      
                      gui.jTextField_nmsg.setText(""+ct);
                      try {
                          Thread.sleep(1000/msg_s);
                      } catch (InterruptedException ex) {
                          Logger.getLogger(Motor.class.getName()).log(Level.SEVERE, null, ex);
                      }
                  }
                      
                  }
                  
          };
        
        stressThread.start();
        gui.gettextWin().append("Motor stress thread started\n");

   }
    


public void PID_msg(int msg_start, int msg_end)
{
        gui.gettextWin().append("Sending PID msg\n");
        
            PID_msg_on=true;
            stressThread=new Thread(){
                
                int ct=0;
                String msg;
                int ID;
                int Length;
                int Data[]=new int[8];
                Random rand = new Random(); //instance of random class        
          int pidn;
         
              public void run() {
                  while(PID_msg_on){
                     
                      ID=0x7DF;
                      Length=8;
                      ct=ct+1;
                      
                      
                      Data[0]=0x02;
                      Data[1]=0X01;
                      Data[2]=pidn;
                      pidn++;
                      if (pidn>msg_end) 
                          pidn=msg_start;
                          
                      Data[3]=0x00;                              
                      Data[4]=0x00;                              
                      Data[5]=0x00;                              
                      Data[6]=0x00;                              
                      Data[7]=0x00;                              
                      
                      msg=">";
                      msg=msg+inttostring(ID,4);
                      msg=msg+inttostring(Length,2);
                      for (int t=0;t<Length;t++)
                           msg=msg+inttostring(Data[t],2);
                      msg=msg+"<";
                      
                      gui.sendmessageBYSERIE(msg);
                      
                      gui.jTextField_nmsg.setText(""+ct);
                      try {
                          Thread.sleep(1000);
                      } catch (InterruptedException ex) {
                          Logger.getLogger(Motor.class.getName()).log(Level.SEVERE, null, ex);
                      }
                  }
                      
                  }
                  
          };
        
        stressThread.start();
        gui.gettextWin().append("Motor stress thread started\n");

   }
    
}
