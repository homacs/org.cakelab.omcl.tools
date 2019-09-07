package org.cakelab.omcl.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.cakelab.omcl.utils.log.Log;


public class Md5Sum {

	private Md5Sum() {
	}

	
	public static String getMD5Sum(File f) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(f);
			DigestInputStream dis = new DigestInputStream(is, md);
			byte[] devnull = new byte[1024];
			while (0 < dis.read(devnull));
			dis.close();
			
			byte[] digest = md.digest();		
			String hex = toHex(digest);
			return hex;
		} catch (IOException e) {
			Log.warn("the file we should check just disappeared.", e);
		} catch (NoSuchAlgorithmException e1) {
			Log.warn("MD5 algorithm not available.", e1);
			
		}
		return null;
	}
	
	public static boolean check(File f, String sum) {
		String checksum = getMD5Sum(f);
		if (checksum == null || sum == null || sum.length() == 0) {
			Log.warn("missing checksum: skipping checksum check of file " + f.getName());
			return true;
		}
		return  checksum.equals(sum.trim().toLowerCase());
	}
	
	public static void main(String [] args) {
		File f = new File("/home/homac/workspace-MCLauncher/LitWRLauncher/repository/client/hungry/1.0.0/LifeInTheWoodsRenaissanceClientHungry.zip");
		String md5sum = getMD5Sum(f);
		System.out.println("checksum: " + md5sum);
		System.out.print("check: ");
		if (check(f, "d9252bbb5d35ede6ab9ce5c402bb947b")) {
			System.out.println("successful");
		} else {
			System.out.println("failed");
		}
	}


	private static String toHex(byte[] digest) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os);
			for (byte b : digest) {
				ps.printf("%02x", b);
			}
			String output = os.toString("UTF8");
			return output;
		} catch (UnsupportedEncodingException e) {
			// impossible
			e.printStackTrace();
		}
		return null;
	}



}
