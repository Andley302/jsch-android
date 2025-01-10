/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002-2016 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;

import android.util.Base64;

import net.i2p.crypto.eddsa.Utils;

import java.io.*;
import java.net.*;
import app.jsch.androidtunnel.*;
import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.logs.AppLogManager;
import app.jsch.androidtunnel.vpnutils.TunnelUtils;

public class ProxyDirect implements Proxy{
  private static int DEFAULTPORT=80;
  private String proxy_host;
  private int proxy_port;
  private InputStream in;
  private OutputStream out;
  private Socket socket;
  private String payload = null;

  private String user = null;
  private String passwd = null;

  public ProxyDirect(String proxy_host){
    int port=DEFAULTPORT;
    String host=proxy_host;
    if(proxy_host.indexOf(':')!=-1){
      try{
	host=proxy_host.substring(0, proxy_host.indexOf(':'));
	port=Integer.parseInt(proxy_host.substring(proxy_host.indexOf(':')+1));
      }
      catch(Exception e){
      }
    }
    this.proxy_host=host;
    this.proxy_port=port;
  }


  public ProxyDirect(String proxyIP, int proxy_port, String payloadKey) {
    this.proxy_host=proxyIP;
    this.proxy_port=proxy_port;
    this.payload = payloadKey;
  }


  public void setUserPasswd(String user, String passwd){
    this.user=user;
    this.passwd=passwd;
  }
  public void connect(SocketFactory socket_factory, String host, int port, int timeout) throws JSchException{
    //AppLogManager.addToLog("Direct: " + proxy_host + ":" + proxy_port);
    //AppLogManager.addToLog("Payload: " + payload);
    try{
      if(socket_factory==null){
        socket=Util.createSocket(proxy_host, proxy_port, timeout);
        in=socket.getInputStream();
        out=socket.getOutputStream();
      }
      else{
        socket=socket_factory.createSocket(proxy_host, proxy_port);
        in=socket_factory.getInputStream(socket);
        out=socket_factory.getOutputStream(socket);
      }
      socket.setSoTimeout(5000);

      String requestPayload = getRequestPayload(proxy_host, proxy_port);

      AppLogManager.addToLog("<b><font color=#49C53C>Sending direct request...</font></b>");

      // suporte a [split] na payload
      if (!TunnelUtils.injectSplitPayload(requestPayload, out)) {
        try {
          out.write(requestPayload.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e2) {
          out.write(requestPayload.getBytes());
        }
        out.flush();
      }

      //MODO ANTIGO ABAIXO (NÃO SUPORTA ALGUMAS PAYLOADS)
      /*out.write(Util.str2byte(payload));

      if(user!=null && passwd!=null){
        byte[] code=Util.str2byte(user+":"+passwd);
        code=Util.toBase64(code, 0, code.length);
        out.write(Util.str2byte("Proxy-Authorization: Basic "));
        out.write(code);
        out.write(Util.str2byte("\r\n"));
      }

      out.write(Util.str2byte("\r\n"));
      out.flush();*/


    } catch (SocketException e) {
      e.printStackTrace();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  public InputStream getInputStream(){ return in; }
  public OutputStream getOutputStream(){ return out; }
  public Socket getSocket(){ return socket; }
  public void close(){
    try{
      if(in!=null)in.close();
      if(out!=null)out.close();
      if(socket!=null)socket.close();
    }
    catch(Exception e){
    }
    in=null;
    out=null;
    socket=null;
  }
  public static int getDefaultPort(){
    return DEFAULTPORT;
  }

  private String getRequestPayload(String hostname, int port) {
    if (payload != null) {
      payload = TunnelUtils.formatCustomPayload(hostname, port, payload);
    }
    else {
      StringBuffer sb = new StringBuffer();

      sb.append("CONNECT ");
      sb.append(hostname);
      sb.append(':');
      sb.append(port);
      sb.append(" HTTP/1.0\r\n");
      if (!(user == null || passwd == null)) {
        char[] encoded;
        String credentials = user + ":" + passwd;
        /*try {
          encoded = Base64.encode(credentials.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
          encoded = Base64.encode(credentials.getBytes());
        }*/
        encoded = null;
        sb.append("Proxy-Authorization: Basic ");
        sb.append(encoded);
        sb.append("\r\n");
      }
      sb.append("\r\n");

      payload = sb.toString();
    }

    return payload;
  }
}
