/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package commprotocol;

//import java.io.FileDescriptor;
import static commprotocol.SerTermGUI.MAX_DATA;
import static commprotocol.SerTermGUI.Ventana;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;
//import javax.swing.JOptionPane;


/**
 *
 * @author ppere
 */
public class Serie {
   
    SerTermGUI gui;
    SerialPort serialPort;
    Thread thread_puertos;
    private String[] portList; //list of ports for combobox dropdown
    
    boolean CANFrame=false;
    final int MAXidxbufferCAN=400;
    byte[] bufferaux=new byte[MAXidxbufferCAN];
    int idxbufferCAN=0;
    int indbuffer;
    
    
    
   private InputStream inputStream;
   
   private int msg_monitoring_status=0;
   private int msg_monitoring_recv=0;
    
    public Serie(SerTermGUI ventana) {
        
        gui=ventana;
        gui.gettextWin().append(">>Select Port, Specify Baud Rate (default 9600), Open Port.\n");
        
        thread_puertos=new Thread (){
            public void run() {
                do
                {
                    try {
                        Thread.sleep(1000);
                        if (gui!=null) {
                            getPorts();
                            if (gui.getportBox().getItemCount()!=portList.length) gui.getportBox().setModel(new javax.swing.DefaultComboBoxModel(portList));
                        }
                    } catch (InterruptedException ex) {
                        System.out.println(ex);
                    }
                
                }
                while(true);
            }
        };
        thread_puertos.start();
        //Display some instructions upon opening
    }

void msgmonitoring(String cad)
{
    
    int t;
    
    for (t=0;t<cad.length();t++){
    switch (msg_monitoring_status)
    {
        case 0: if (cad.charAt(t)=='>') msg_monitoring_status=1;
                break;
        case 1: if (cad.charAt(t)=='R') msg_monitoring_status=2;
                break;
        case 2: if (cad.charAt(t)=='<') {
                    msg_monitoring_status=0;
                    msg_monitoring_recv++;
                    gui.jTextField_nmsg1_rec.setText(""+msg_monitoring_recv);
        }
                break;
        
    }
}
}
            
private void getPorts() {        portList = SerialPortList.getPortNames();   }
    
 public class Reader implements SerialPortEventListener {

 private String str = "";

        public void serialEvent(SerialPortEvent spe) {

                //byte[] buffer = new byte[MAX_DATA];   //create a buffer (enlarge if buffer overflow occurs)
                int numBytes=0;   //how many bytes read (smaller than buffer)
                int int16value;
            
            if(spe.isRXCHAR() || spe.isRXFLAG()){
                if(spe.getEventValue() > 0){
                    try {
                        do 
                        {
                            numBytes=serialPort.getInputBufferBytesCount();
                            if (numBytes==0) continue;                            
                            byte[] buffer=serialPort.readBytes();                           
                            if (buffer==null) continue; 
                            if (gui.motor!=null) gui.motor.recibir(buffer);
                            if (gui.sCAN!=null) gui.sCAN.recibir(buffer);
                            String cadena=new String(buffer);
                            gui.gettextWin().append(cadena);
                            msgmonitoring(cadena);
                               
                        }while(numBytes>0);
                     
                    } catch (SerialPortException ex) {
                        Logger.getLogger(SerTermGUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        
                }
            }
        }
        
    }


}

