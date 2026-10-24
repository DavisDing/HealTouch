package com.healtouch.service;

import static org.junit.Assert.*;
import java.io.*;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;

public class AuthenticatedStreamsTest {
  private final SecretKeySpec key=new SecretKeySpec(new byte[16],"AES");
  private final byte[] nonce=new byte[12],header={1,2,3};
  private byte[] encrypt(byte[] plain) throws Exception {
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    try(OutputStream out=new AuthenticatedStreams.Encrypt(bytes,key,nonce,header)){out.write(plain);}
    return bytes.toByteArray();
  }
  private byte[] decrypt(byte[] bytes) throws Exception {
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    try(InputStream in=new AuthenticatedStreams.Decrypt(new ByteArrayInputStream(bytes),key,nonce,header)) {
      byte[] block=new byte[8192];int count;while((count=in.read(block))!=-1)out.write(block,0,count);
    }return out.toByteArray();
  }
  @Test public void multipleRecordsAndEmptyPayloadRoundTrip() throws Exception {
    byte[] plain=new byte[200000];for(int i=0;i<plain.length;i++)plain[i]=(byte)i;
    assertArrayEquals(plain,decrypt(encrypt(plain)));assertArrayEquals(new byte[0],decrypt(encrypt(new byte[0])));
  }
  @Test public void truncatedAndModifiedRecordsCannotBeAccepted() throws Exception {
    byte[] original=encrypt(new byte[100000]);
    assertThrows(IOException.class,()->decrypt(Arrays.copyOf(original,original.length-1)));
    byte[] changed=original.clone();changed[40]^=1;
    assertThrows(IOException.class,()->decrypt(changed));
    byte[] extended=Arrays.copyOf(original,original.length+1);
    assertThrows(IOException.class,()->decrypt(extended));
  }
  @Test public void recordReorderingAndHeaderModificationAreDetected() throws Exception {
    byte[] original=encrypt(new byte[140000]);int frame=65536+20;
    byte[] changed=original.clone();System.arraycopy(original,frame,changed,0,frame);System.arraycopy(original,0,changed,frame,frame);
    assertThrows(IOException.class,()->decrypt(changed));
    assertThrows(IOException.class,()->{
      try(InputStream in=new AuthenticatedStreams.Decrypt(new ByteArrayInputStream(original),key,nonce,new byte[]{9})){while(in.read()!=-1){}}
    });
  }
}
