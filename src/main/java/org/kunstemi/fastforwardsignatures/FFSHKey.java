package org.kunstemi.fastforwardsignatures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.Key;
import java.security.spec.X509EncodedKeySpec;

/*
 * The MIT License

Copyright (c) 2015 Axlacor Ltd, (Michael Kunstel)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

public class FFSHKey extends X509EncodedKeySpec implements Key , Serializable {

	private static final long serialVersionUID = 694759178015857262L;
	protected transient BigInteger[] vals = null;
	public static final BigInteger DEFAULT_MODULO_384 = BigInteger.ONE.shiftLeft(383).subtract(BigInteger.valueOf(31));
	public static final BigInteger DEFAULT_MODULO_256 = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19));

	//p,q,m
	public FFSHKey(BigInteger... vals) throws IOException {
		super(toSingle(vals));
		this.vals=vals;
	}
	
	public FFSHKey(byte[] vals) throws IOException {
		super(vals);
		this.vals=toArray(vals);
	}
	

	public static byte[] toSingles(BigInteger... vals) throws IOException {
		return toSingle(vals);
	}
	
	public static byte[] toSingle(BigInteger[] vals) throws IOException {
		BigInteger p = vals[0];
		final ByteArrayOutputStream bos = new ByteArrayOutputStream(p.bitLength()/6); 
		final DataOutputStream dos = new DataOutputStream(bos);

		BigInteger m = vals[0];
		byte[] ml = m.toByteArray();		
		if (m.equals(DEFAULT_MODULO_384)) {
			dos.writeByte(-1);
		} else if (m.equals(DEFAULT_MODULO_256)) {
			dos.writeByte(-2);
		} else {
			 dos.writeByte(ml.length);
			 dos.write(ml);			 
		}
		
		int vl = vals.length;
		//dos.writeByte(vl);
		for (int ii = 1; ii < vl; ++ii) {
			byte[] bi = m.toByteArray();
			int diff = bi.length-ml.length;
			for (;diff>0;--diff) {
				dos.writeByte(0);
			}
			dos.write(bi);
		}
		dos.flush();
		return bos.toByteArray();
	}
	
	public static BigInteger[] toArray(byte[] source) throws IOException {
		final ByteArrayInputStream bos = new ByteArrayInputStream(source); 
		final DataInputStream dis = new DataInputStream(bos);
		//int vl = dis.readByte();
		BigInteger[] ret = new BigInteger[5]; 
		int bi = dis.readByte();
		if (bi==-1) {
			ret[0] = DEFAULT_MODULO_384; 
		} else 	if (bi==-2) {
			ret[0] = DEFAULT_MODULO_256; 
		} else  {
			byte[] b = new byte[bi];
			dis.read(b);
			ret[0] = new BigInteger(b);
		}
		
		byte[] buf = new byte[ret[0].toByteArray().length];
		for (int ii = 1; ii < 5; ++ii) {				
				dis.read(buf);
				ret[ii] = new BigInteger(buf);
		}
		return ret;
	}	

	@Override
	public String getAlgorithm() {
		return "FFSH1";
	}
	
	protected void setup() throws IOException {
		vals = toArray(this.getEncoded());
	}

	public BigInteger getP() throws IOException {
		if (vals==null) {
			setup();
		}
		return vals[0];
	}

	public BigInteger getQ() throws IOException {
		if (vals==null) {
			setup();
		}		
		return vals[1];
	}

	public BigInteger getM() throws IOException {
		if (vals==null) {
			setup();
		}		
		return vals[2];
	}

	public BigInteger[] getVals() {
		return vals;
	}
	

}
