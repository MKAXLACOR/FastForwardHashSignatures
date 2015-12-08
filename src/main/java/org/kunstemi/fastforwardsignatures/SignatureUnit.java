package org.kunstemi.fastforwardsignatures;

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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import org.kunstemi.fastforwardsignatures.ffh.PQPair;
import org.kunstemi.fastforwardsignatures.ffh.RecurrenceCalculator;

public class SignatureUnit extends Signature{

	protected RecurrenceCalculator calculator;
	protected final static SecureRandom sRandom = new SecureRandom(); 
	protected BigInteger stepsPerMove = BigInteger.valueOf(Long.MAX_VALUE);
	protected final MessageDigest hasher;
	
	protected FFSHPrivateKey privateKey;
	protected FFSHPublicKey publicKey;
	
	protected final static int HASH_BITS = 256;
	protected final static BigInteger MAX_SHIFT = BigInteger.ONE.shiftLeft(HASH_BITS);
	
	protected final static BigInteger skipLength = BigInteger.valueOf(Integer.MAX_VALUE);
	
	// Generate a signature unit given a prime modulo
	public SignatureUnit(BigInteger modulo) {
		this(new RecurrenceCalculator(911, 691, 2733, 2073, -1357, 2468, modulo));
	}

	// Generate a signature unit - calculate the modulo	
	public SignatureUnit(SecureRandom random, int probablePrimeProbability) {
		this(new RecurrenceCalculator(911, 691, 2733, 2073, -1357, 2468, genModulo(random, 374,384, probablePrimeProbability)));
	}
	
	public SignatureUnit(SecureRandom random) {
		this(random,80);
	}
	
	public SignatureUnit(int probablePrimeProbability) {
			this(sRandom,probablePrimeProbability);
	}		
	
	public SignatureUnit() {
		this(FFSHKey.DEFAULT_MODULO_384);
}			
	
	public BigInteger modulo() {
		return calculator.getM();
	}
	
	
	public static BigInteger genModulo(SecureRandom random, int lowBits, int highBits, int probability) {
		// find a probable prime modulo somewhere between lowBits and highBits in bitLength
		
		BigInteger hv = BigInteger.ONE.shiftLeft(highBits);
		BigInteger lv = BigInteger.ONE.shiftLeft(lowBits);
		BigInteger s;
		int nlen = hv.bitLength();
		// find a randomish point between low and high values
		do {
		    s = new BigInteger(nlen, random);
		    if (s.compareTo(hv)<0 && s.compareTo(lv)>0) { // find random within range
		    		break;
		    }
		} while (true); // loop until found
		int ll = lv.bitLength();
		int sl = s.bitLength();
		while (!s.isProbablePrime(probability)) {
			s = s.subtract(BigInteger.ONE);
		}
		return s;
	}

	protected SignatureUnit(RecurrenceCalculator req)  {
		super("FFHS");
		this.calculator = req;
		try {
			this.hasher = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}	

	public KeyPair createRandomKeyPair() {
		synchronized (this.sRandom) {
			return createRandomKeyPair(calculator, this, this.sRandom, false);
		}
	}
	
	
	
	public static KeyPair createRandomKeyPair(RecurrenceCalculator calc, SignatureUnit unit, SecureRandom random, boolean checkWrap) {
		BigInteger m = unit.modulo();
		int l = m.bitLength();
		while (true) {
			try {
				byte[] pk = null;
				FFSHPrivateKey pv = null;
				while (true) {
					// create random private key - in effect our start state
					BigInteger p1 = (new BigInteger(l+256,random)).mod(m);
					BigInteger q1 = (new BigInteger(l+256,random)).mod(m);
					BigInteger p2 = (new BigInteger(l+256,random)).mod(m);
					BigInteger q2 = (new BigInteger(l+256,random)).mod(m);
					
					BigInteger sl2 = skipLength.multiply(MAX_SHIFT);
					pv = new FFSHPrivateKey( m, p1, q1,  p2, q2);
					PQPair ret1 = calc.calculate(p1, q1, BigInteger.ZERO, sl2);
					PQPair ret2 = calc.calculate(p2, q2, BigInteger.ZERO, sl2);
					
					if (checkWrap) {
						//////////////
						// Simple check to make sure we don't have any obvious cycles when we clock over/overflow our exogenous counter 
						// check we don't easily move back to start state by clocking over
						// note - i've run this program many times with checkWrap on and never seen such a cycle
						PQPair check1 = calc.calculate(p1, q1, BigInteger.ZERO, m);
						PQPair check2 = calc.calculate(p2, q2, BigInteger.ZERO, m);
						if ( (check1.getP().equals(ret1.getP())) || (check2.getP().equals(ret2.getP())) ) {
							continue; // possible wrap around 
						}
					}
					
					byte[] out = FFSHKey.toSingles( m, ret1.getP(),ret1.getQ(), ret2.getP(),ret2.getQ());
					
					// now hash it
					MessageDigest hash = MessageDigest.getInstance("SHA-256");
					pk = hash.digest(out);
					break;
				} 
				
				// The public key is the hash of all end states and settings
				PublicKey pubKey = new FFSHPublicKey( pk );
				
				return new KeyPair(pubKey, pv);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}		
	}
	
	public static Signature signature() {
		return new  SignatureUnit();
	}

	@Override
	protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
		hasher.reset();
		this.publicKey = (FFSHPublicKey) publicKey;
	}

	@Override
	protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
		hasher.reset();		
		this.privateKey = (FFSHPrivateKey) privateKey;
	}

	@Override
	protected void engineUpdate(byte b) throws SignatureException {
		hasher.update(b);		
	}

	@Override
	protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
		hasher.update(b,off,len);		
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		byte[] dataSignature = hasher.digest();
		try {
			BigInteger[] vals = this.privateKey.getVals();
			BigInteger m = vals[0];
			BigInteger p1 = vals[1];
			BigInteger q1 = vals[2];			
			BigInteger p2 = vals[3];
			BigInteger q2 = vals[4];
			
			BigInteger sig = new BigInteger(1,dataSignature);
			BigInteger jump1 = skipLength.multiply(sig);

			BigInteger inverse = MAX_SHIFT.subtract(BigInteger.ONE).subtract(sig);
			BigInteger jump2 = skipLength.multiply(inverse);
			
			PQPair ret1 = calculator.calculate(p1, q1, BigInteger.ZERO, jump1);
			PQPair ret2 = calculator.calculate(p2, q2, BigInteger.ZERO, jump2);

			byte[] signature = FFSHKey.toSingles(m,ret1.getP(),ret1.getQ(),ret2.getP(),ret2.getQ());
			return signature;  
		} catch (IOException e) {
			throw new SignatureException(e.toString());
		}
	}

	@Override
	protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
		try {
			byte[] dataSignature = hasher.digest();
			
			BigInteger[] vals = FFSHKey.toArray(sigBytes);
			BigInteger m = vals[0];
			
			BigInteger sig = (new BigInteger(1,dataSignature));
			BigInteger jump1 = skipLength.multiply(sig);
			
			BigInteger inverse = MAX_SHIFT.subtract(BigInteger.ONE).subtract(sig);
			BigInteger jump2 = skipLength.multiply(inverse);
			
			BigInteger endState = skipLength.multiply(MAX_SHIFT);
			
			PQPair ret1 = calculator.calculate(vals[1], vals[2], jump1, endState.subtract(jump1));
			PQPair ret2 = calculator.calculate(vals[3], vals[4], jump2, endState.subtract(jump2));
			
			/*
			BigInteger q = vals[3];
			BigInteger[] bugfix = this.privateKey.getVals();
			PQPair xxxx = calculator.calculate(bugfix[3], bugfix[4], BigInteger.ZERO, endState);
			PQPair x1 = calculator.calculate(bugfix[3], bugfix[4], BigInteger.ZERO, jump2);
			PQPair x2 = calculator.calculate(x1.getP(), x1.getQ(), jump2, endState.subtract(jump2));
			*/
			
			byte[] out = FFSHKey.toSingles( m, ret1.getP(),ret1.getQ(), ret2.getP(),ret2.getQ());
			// now hash it
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			byte[] pk = hash.digest(out);	
			
			byte[] given = this.publicKey.getEncoded();
			return Arrays.equals(pk, given);			
		} catch (Exception e) {
			throw new SignatureException(e);
		}
	}

	@Override
	protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object engineGetParameter(String param) throws InvalidParameterException {
		// TODO Auto-generated method stub
		return null;
	}

	public static SecureRandom getRandom() {
		return sRandom;
	}	
}

