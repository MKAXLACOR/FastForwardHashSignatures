# FastForwardHashSignatures

This is experimental code for a new system for Small Post-Quantum Signatures.

This current implementation creates public keys of 32 bytes in length, and signatures of 129 bytes in length.<br>
1000-2000 signatures can be performed per second.  This code is not optimised and this can be made orders of magnitude faster.

The primary requirement is to have a hash function that can be quickly iterated multiple steps into the future.

For example, hashing an input 3 times ->  H(H(H(X))) should be simplifiable to a constant time expression H(X,3). 

Note that this is a one-time signature scheme.  Using merkle trees it may be possible to turn it into a many time signature scheme.

# Concept

Let's assume we wish to create a signature. <br>
First hash the data with a known cryptographic function, e.g. SHA256, to produce 256 bit number.

With a large fast-fowardable sequence generator, start with random-generated state A (private key), fast forward 2^256 states to get the public key. <br><br>
To sign, you would send a state in the middle, which when iterated N steps ( where N==H==the hash value of the data you're signing) forward to equal the public key. <br><br>
You would need to repeat this with a second private/public pair with N=(2^256-H) to prove the H is correct.<br>

The lovely thing about that of course is your public key could be 32 bytes long (the SHA256 hash of the all the end states).
The signature == 2\*the initial start positions to reach the public end state, say 2\*64 bytes == 128 bytes. 

# Hash Function requirements

For the system to work, the hash function:<br>

1. Needs to be cryptographically secure<br>
2. Can be fast forwarded X states, without the ability of moving backwards.  - e.g. H(H(H(X))) equals H(X,3).   <br>
3. Cycle length is unknown and reasonably unknowable - as moving forward CYCLE_LENGTH -1 is equivalent to moving backwards<br>
   Please note that standard hashes such as SHA256 may have cycles - no-one is currently sure.<br>
4. Should have some guaranteed ability not to collapse into a short cycles over the numeric range we wish to sign with<br>
5. Needs to be safe from SHORs algorithm.  SHORs algorithm can immediately find the cycle length of simple systems.  <br>
   Some mechanism needs to be in place to ensure that the sequence never repeats/never re-enters the same state-space.<br>
   For example, ensuring that eventually it will move into a small cyclical state will make it SHORs secure.<br>
   
The implemented hash function detailed below provides a 'reasonable shot' at the above - perhaps can be considered a informative reference to other future implementations.
   
# Implemented Hash

 The hash function implemented here relies on singular matricies and exogenous variables.<br>
 
 Imagine we wish to calculate the next value of variables P(N+1) and Q(N+1), given P(N) and Q(N).<br>
 We are given integer constants a,b,c,d and f,g and initial P( oldN ), Q( oldN )

 we state that <br>
 <b> x(n)= (f \* P(n-1) ) + n </b> <br>
 <b> y(n)= (g \* Q(n-1) ) + 1 </b> <br>

 Which is to say, x(n) and y(n) are Linear congruential generators based on the state of P/Q. <br><br>
 Note however, while y(n) uses a fixed increment c=1,  x(n) has a variable incrementor n - which is the current iteration of the fast forwarding hash function. <br><br>
 This acts like an exogenous input that is always changing the transition of the state space. <br>
 The upshot is that this exogonous variable can ensure that the LCG never goes into a short cycle over the required range, and even if P and Q repeat, if n isn't the same, the equation will still move to a new state space. <br>

 Step 2: We then put x and y through a singular matrix, e.g.: <br>

 http://www.wolframalpha.com/input/?i=%7Bx%2Cy%7D%7B%7Ba%2Cb%7D%2C%7Bc%2Cd%7D%7D  <br>

 We can then say that the next version of P and Q are calculated via the singular (non invertible) matrix. <br>
 <b> P(n) = a\*x(n) + c\*y(n) </b> <br>
 <b> Q(n) = b\*x(n) + d\*y(n) </b> <br>

 We assume that we are modulo'ing - thus above is really is put through modulo <br>
 <br>
 <b> P(n) = ( a\*x(n) + c\*y(n) ) mod M  </b> <br>
 <b> Q(n) = ( b\*x(n) + d\*y(n) ) mod M  </b> 

<br> If the a,b,c,d are singular, the matrix should not be invertable, thus 'you can move forward quickly, however not backward'.

The only exception to this is if you know the cycle length of the system - then you need move forward (cycle_length-1) steps to move backward by one.<br>
Further techniques are thus needed to ensure (parallel to exogenous variable) that the cycle does not ever repeat and cannot be found.

## Implementation notes

### Modulo
Note that you can supply your own modulo which will increase the size of the generated signature.  <br>
The reason this isn't default - If you think you're strengthening by making it a factor of 2 primes - this falls down with SHORs as well.  <br>
It's a false sense of security.  Thus the reason the default uses a standard prime number for M is 'we shouldn't rely on it for safety'.<br>

### Skip ahead rate

As we can skip ahead at any rate, we multiply the digest value (sha256) with a skip ahead multiplier.  Currently this is Integer.MAX_VALUE, or 0x7fffffff.
This is the equivalent of running the above recurrence 2 billion times for each single bit of the digest.

### Default values

By default, our values for the a,b,c,d,f,g are given the hardcoded values (911, 691, 2733, 2073, -1357, 2468).
The system will work with other values of a,b,c,d,f,g and modulo - it's up to the user to find their preference.
However - a,b,c,d must form a singular matrix.


# Other possible hashes ideas 

## SHORS algorithm safety

The implemented hash function does not guarantee that SHORs cannot find the cycle.  <br>
One way to ensure that SHORs cannot break the hash is to ensure that the system will descend into a closed loop cycle some time in the far future.

for example, imagine we have a base one-way function y(x). 

Imagine a simple test recurrence H(n) = n\* H(n-1) mod m

At n=0 (start state),  n=0 will cause H(0)=0 to go into a loop.<br><br>
This means at n=1  the previous state is a 'infite loop', we can override H(0) to be some constant, thus n=1 is the guaranteed start of the sequence.<br><br>
At some time in the future, we can estimate<br>
  n\* H(n-1) mod m -> 0<br>

 thus for some n and m in the future, the system will also collapse into a infinite loop.
 
The upshot is - to ensure some immunity from SHORs, you would ensure that the for the maximum expected move allowable (e.g. 2^256) the system is not stuck in a cycle, <br>
however for a larger move (e.g. 2^512) it IS stuck in a short cycle.<br>

## Nonlinear Recurrences

other recurrences can use earlier history, e.g <br>
X(n) = (a * x(n-1) + b * x( n / 2 )) mod m <br>

Another possibility is logistic equations, e.g. <br>
x(n) = a*x(n-1)* (m - x(n-1)) mod m  ;  <br>

Feeding in known cycles or endpoints is clearly possible <br>

x(n) = (n*(n - 2^256)* (x(n-1)* ( x(n-1)-1 ) ) ) mod m <br>

The issue comes down to the complexity of solving recurrences - many recurrences don't have simple solutions for arbitary n moves in the future! <br>

# Ending note:

This project is built to spark interest rather than have a final answer.   <br>
Finding other ff hash functions that are SHORs safe, cryptographicly secure and non-reversible are an exercise left to the reader. <br>
(please do - I would love a SHA-4 to accomplish this!)

Released under MIT license, see LICENSE file for details.  Use at own risk.
Thanks - Michael Kunstel.
