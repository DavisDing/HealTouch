package com.healtouch.service;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Bounded AES-GCM records, with sequence and final-record authentication. No unauthenticated reads. */
final class AuthenticatedStreams {
  private static final int CHUNK=64*1024;
  private AuthenticatedStreams() {}
  private static Cipher cipher(int mode,SecretKey key,byte[] nonce,byte[] header,long sequence,boolean last)
      throws GeneralSecurityException {
    byte[] iv=Arrays.copyOf(nonce,12);ByteBuffer.wrap(iv,4,8).putLong(sequence);
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(mode,key,new GCMParameterSpec(128,iv));
    cipher.updateAAD(header);cipher.updateAAD(ByteBuffer.allocate(9).putLong(sequence).put((byte)(last?1:0)).array());
    return cipher;
  }
  static final class Encrypt extends OutputStream {
    private final DataOutputStream out;
    private final SecretKey key;
    private final byte[] nonce,header,buffer=new byte[CHUNK];
    private int count;
    private long sequence;
    private boolean closed;
    Encrypt(OutputStream out,SecretKey key,byte[] nonce,byte[] header) {
      this.out=new DataOutputStream(out);this.key=key;this.nonce=nonce.clone();this.header=header.clone();
    }
    public void write(int value) throws IOException {byte[] one={(byte)value};write(one,0,1);}
    public void write(byte[] bytes,int offset,int length) throws IOException {
      if(closed)throw new IOException("备份流已关闭");
      while(length>0) {
        int n=Math.min(length,CHUNK-count);System.arraycopy(bytes,offset,buffer,count,n);count+=n;offset+=n;length-=n;
        if(count==CHUNK)record(false);
      }
    }
    private void record(boolean last) throws IOException {
      try {
        byte[] encrypted=cipher(Cipher.ENCRYPT_MODE,key,nonce,header,sequence,last).doFinal(buffer,0,last?0:count);
        out.writeInt(last?0:count);out.write(encrypted);sequence=Math.incrementExact(sequence);count=0;
      }catch(GeneralSecurityException | ArithmeticException e){throw new IOException("备份分块加密失败",e);}
    }
    public void flush() throws IOException {out.flush();}
    public void close() throws IOException {
      if(closed)return;closed=true;
      try {if(count>0)record(false);record(true);out.flush();}finally {Arrays.fill(buffer,(byte)0);out.close();}
    }
  }
  static final class Decrypt extends InputStream {
    private final DataInputStream in;
    private final SecretKey key;
    private final byte[] nonce,header;
    private byte[] buffer=new byte[0];
    private int offset;
    private long sequence;
    private boolean complete;
    Decrypt(InputStream in,SecretKey key,byte[] nonce,byte[] header) {
      this.in=new DataInputStream(in);this.key=key;this.nonce=nonce.clone();this.header=header.clone();
    }
    private boolean next() throws IOException {
      if(complete)return false;
      int length=in.readInt();if(length<0||length>CHUNK)throw new IOException("备份分块长度无效");
      byte[] bytes=new byte[length+16];in.readFully(bytes);
      try {
        Arrays.fill(buffer,(byte)0);
        buffer=cipher(Cipher.DECRYPT_MODE,key,nonce,header,sequence,length==0).doFinal(bytes);
        sequence=Math.incrementExact(sequence);offset=0;
      }catch(GeneralSecurityException | ArithmeticException e){throw new IOException("备份认证校验失败",e);}
      if(length==0) {if(in.read()!=-1)throw new IOException("备份结束后存在多余数据");complete=true;return false;}
      return true;
    }
    public int read() throws IOException {
      if(offset==buffer.length && !next())return -1;return buffer[offset++]&255;
    }
    public int read(byte[] bytes,int start,int length) throws IOException {
      if(length==0)return 0;
      if(offset==buffer.length && !next())return -1;
      int n=Math.min(length,buffer.length-offset);System.arraycopy(buffer,offset,bytes,start,n);offset+=n;return n;
    }
    public void close() throws IOException {Arrays.fill(buffer,(byte)0);in.close();}
  }
}
