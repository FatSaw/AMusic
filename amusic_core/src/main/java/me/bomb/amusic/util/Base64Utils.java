package me.bomb.amusic.util;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class Base64Utils {
	
	private final static Encoder base64encoder, base64urlencoder;
	private final static Decoder base64decoder, base64urldecoder;
	
	static {
		base64urlencoder = Base64.getUrlEncoder();
		base64urldecoder = Base64.getUrlDecoder();
		base64encoder = Base64.getEncoder();
		base64decoder = Base64.getDecoder();
	}
	
	private Base64Utils() {
	}
	
	public static String toBase64Url(String name) {
		return new String(base64urlencoder.encode(name.getBytes(UTF_8)), US_ASCII);
	}
	
	public static String fromBase64Url(String name) throws IllegalArgumentException {
		return new String(base64urldecoder.decode(name.getBytes(US_ASCII)), UTF_8);
	}
	
	public static String toBase64(String name) {
		return new String(base64encoder.encode(name.getBytes(UTF_8)), US_ASCII);
	}
	
	public static String fromBase64(String name) throws IllegalArgumentException {
		return new String(base64decoder.decode(name.getBytes(US_ASCII)), UTF_8);
	}

}
