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
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class FFSHPublicKey extends X509EncodedKeySpec implements PublicKey {
	private static final long serialVersionUID = -6665082068277954818L;

	public FFSHPublicKey(BigInteger finalStateHash) throws IOException {
		super(finalStateHash.toByteArray());
	}

	public FFSHPublicKey(byte[] finalStateHash) throws IOException {
		super(finalStateHash);
	}

	@Override
	public String getAlgorithm() {		
		return "FFSH1";
	}
	
}
