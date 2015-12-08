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

import java.math.BigInteger;
import java.security.KeyPair;


/* Auth: Michael K.
 * 
 * This is a simple test program for the Fast Forward Hash Signature system.  - 
 * 
 * This is an experimental program and not considered vetted by cryptologists or production ready.
 * 
 * The underlying idea: - Create a hash function that can be easily fast forwarded but not reversed, and use for short signatures.
 * 
 * for example, if you ran SHA256 3 times on data X,   the result is H(H(H(X)))
 * 
 * with fast forwarding hash functions, you would instead say H(X,3) to accomplish the same thing.
 * 
 * Why is this useful?  Imagine we need to sign something.  First, get a digest SHA256 of the data you wish to sign.
 * This is a 256 bit number Y.  We then take the fast forward hash and hash X that many times to calculate an end state (public key state)
 * H(X, SHA256(Y))
 * 
 * In order to properly sign this, we need to run this twice with two different versions of X, once with the SHA256(Y), 
 * and once with (2^256)-SHA256(Y).   ie pair z1= H(X1, SHA256(Y)) ; z2= H(X2, (2^256)-SHA256(Y)) 
 * gives our signature pair. 
 *  
 * The upshot is - the public key becomes 32 bytes long (hashed end states), and the signature is the <z1,z2> above. 
 * (384 state size == approx 206 bytes in this implementation, 256 bit state produces a sig of 141 bytes) 
 * To verify, we simply take <z1,z2> and iterate forward to the end states.  We check if the calculated end state == public key
 * If so, we have verified the signature.
 * 
 * Note - this is a proof of concept.  The main claim to one-wayness is that it uses a singular matrix which cannot be inverted.
 * However, if the cycle length was ever known - you could simply go CYCLE_LENGTH-1 to go back a state, breaking the system.
 * The cycle_length is currently unknown due to the usage of a exogenous counter variable.
 * If we check that overflow of the exogenous counter doesn't wrap to initial state 0, we have more confidence that the cycle length is very hard to find.
 * 
 * Whether this is sufficient to overcome SHORs algorithm is to be seen.  Consider this the MD5 of the state of the art.
 * Perhaps the winner of the SHA4 competition one day will solve this problem far more securely.
 * 
 * Please also note this code is NOT optimized for speed.  It could be sped up by a large factor in production.
 */

public class SimpleTest {
	
	public static void checkPrime() {
		BigInteger DEFAULT_MODULO = BigInteger.ONE.shiftLeft(255);
		
		long x = 0;
		while(true) {
			++x;
			BigInteger test = DEFAULT_MODULO.subtract(BigInteger.valueOf(x));
			if (test.isProbablePrime(384)) {
				break;
			}
		}
		System.out.println("Best is "+x);
	}
	
	public static void main(String[] args) {
		
		// Generate keypair
		String source = "This is some user data - whatever you want to sign, generic data, etc";
		
		//SignatureUnit sig = new SignatureUnit(FFSHKey.DEFAULT_MODULO_384);
		SignatureUnit sig = new SignatureUnit(FFSHKey.DEFAULT_MODULO_256);
		sig.createRandomKeyPair(); // warm up
		
		long count = 0;
		long keyTime = 0;
		long signTime = 0;
		long verifyTime = 0;
		
		final int MAX_CHECK_ITERATIONS = 1000;
		
		for (int ii = 0; ii < MAX_CHECK_ITERATIONS; ++ii) {
			try {
				byte[] data = (source+ii).getBytes();
				
				long startTime = System.currentTimeMillis();
				
				// create a (at present) one time signature keypair
				KeyPair keyPair = sig.createRandomKeyPair();
				
				keyTime += System.currentTimeMillis() - startTime;
							
				long signStart = System.currentTimeMillis();
				
				// Now create a signature with the public/private key			
				sig.initSign(keyPair.getPrivate());			
				sig.engineUpdate(data, 0, data.length);			
				byte[] signature = sig.engineSign();
				
				signTime += System.currentTimeMillis()-signStart;
				
				long  verifyStart = System.currentTimeMillis();
				// now verify the signature with the public key
				sig.initVerify(keyPair.getPublic());
				sig.engineUpdate(data, 0, data.length);
				boolean same = sig.verify(signature);
				
				if (!same) {
					System.err.println("ERROR IN VERIFICATION! "+ii);
				}
				assert(same==true);
				
				verifyTime += System.currentTimeMillis() - verifyStart;

				if (count==0) {
					System.out.println("Size of public key in bytes:"+keyPair.getPublic().getEncoded().length);					
					System.out.println("Size of signature key in bytes:"+signature.length);
				}

				++count;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("Completed "+count+" keygen, signing and validating runs - timing in ms:");
		System.out.println("Avg Time Keypair gen ms: "+keyTime/(double)count);
		System.out.println("Avg Time Signing ms:"+signTime/(double)count);
		System.out.println("Avg Time Verifying ms:"+verifyTime/(double)count);
		
		System.out.println();
		
	}
}
