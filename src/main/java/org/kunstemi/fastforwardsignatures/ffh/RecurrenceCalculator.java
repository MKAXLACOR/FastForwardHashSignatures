package org.kunstemi.fastforwardsignatures.ffh;

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
import java.util.Map.Entry;
import java.util.TreeMap;

/*
 This class performs the calculations of multiple step ahead iterations of a 'fast forwardable one way hash function'. 
  
 Imagine we wish to calculate the next value of variables P(N+1) and Q(N+1), given P(N) and Q(N).
 We are given integer constants a,b,c,d and f,g and initial P( oldN ), Q( oldN )
 
 we state that 
 x(n)= (f * P(n-1) ) + n
 y(n)= (g * Q(n-1) ) + 1
 
 Which is to say, x(n) and y(n) are Linear congruential generators based on the state of P
 Note however, while y(n) uses a fixed increment c=1,   x(n) has a variable incrementor n - which is the current iteration of the fast forwarding hash function.
 This acts like an exogenous input that is always changing the transition of the state space.
 The upshot is that this number can ensure that the LCG never goes into a cycle, and even if P and Q repeat, if n isn't the same, the equation will move to a new state space.

 Step2: We then put x and y through a singular matrix, e.g.:
 
 http://www.wolframalpha.com/input/?i=%7Bx%2Cy%7D%7B%7Ba%2Cb%7D%2C%7Bc%2Cd%7D%7D
 
 We can then say that the next version of P and Q are calculated via the singular (non invertible) matrix.
 P(n) = a*x(n) + c*y(n) 
 Q(n) = b*x(n) + d*y(n)
 
 We assume that we are modulo'ing - thus above is really is put through modulo
 
 P(n) = ( a*x(n) + c*y(n) ) mod M
 Q(n) = ( b*x(n) + d*y(n) )  mod M

If the a,b,c,d are singular, the matrix should not be invertable, thus 'you can move forward quickly, however not backward'.
The only exception to this is if you know the cycle length of the system - then you need move forward (cycle_length-1) steps to move backward by one.
Further techniques are thus needed to ensure (parallel to exogenous variable) that the cycle does not ever repeat and cannot be found.
 */

public class RecurrenceCalculator
{
    protected final BigInteger m;
    protected final TreeMap<BigInteger, PQRelations> relationsMap = new TreeMap<>();
    protected PQRelations relations;
    
    public RecurrenceCalculator(int a, int b, int c, int d, int f, int g, int m)
    {
        this(BigInteger.valueOf(a)
                , BigInteger.valueOf(b)
                , BigInteger.valueOf(c)
                , BigInteger.valueOf(d)
                , BigInteger.valueOf(f)
                , BigInteger.valueOf(g)
                , BigInteger.valueOf(m)
                );
    }
    
    
    public RecurrenceCalculator(int a, int b, int c, int d, int f, int g, BigInteger m)    {
        this(BigInteger.valueOf(a)
                , BigInteger.valueOf(b)
                , BigInteger.valueOf(c)
                , BigInteger.valueOf(d)
                , BigInteger.valueOf(f)
                , BigInteger.valueOf(g)
                , m
                );
    }  

    public RecurrenceCalculator(BigInteger a, BigInteger b, BigInteger c, BigInteger d
            , BigInteger f, BigInteger g, BigInteger m)
    {
        this.m = m;
        
        Recurrence pRelation = new Recurrence();
        pRelation.setpCoefficient(a.multiply(f).mod(m));
        pRelation.setnCoefficient(a.mod(m));
        pRelation.setConstant(a.add(c));
        pRelation.setqCoefficient(c.multiply(g).mod(m));
        
        Recurrence qRelation = new Recurrence();
        qRelation.setpCoefficient(b.multiply(f).mod(m));
        qRelation.setnCoefficient(b.mod(m));
        qRelation.setConstant(b.add(d));
        qRelation.setqCoefficient(d.multiply(g).mod(m));
        
        this.relations = new PQRelations(BigInteger.ONE, pRelation, qRelation);
        relationsMap.put(BigInteger.ONE, relations);
    }





	public PQPair calculate(int p, int q, int n, int nPlus)
    {
        return this.calculate(BigInteger.valueOf(p)
                , BigInteger.valueOf(q)
                , BigInteger.valueOf(n)
                , BigInteger.valueOf(nPlus)
                );
    }
    
    public PQPair calculate(BigInteger p, BigInteger q, BigInteger n, BigInteger nPlus)
    {
        p = p.mod(m);
        q = q.mod(m);
        
        Entry<BigInteger, PQRelations> maxEntry = relationsMap.lastEntry();
        BigInteger maxN = maxEntry.getKey();
        PQRelations maxRelations = maxEntry.getValue();
        while(maxN.compareTo(nPlus) < 0)
        {
            maxRelations = this.multiply(maxRelations, maxRelations);
            maxN = maxN.shiftLeft(1);
            relationsMap.put(maxN, maxRelations);
        }
        
        BigInteger remainingNPlus = nPlus;
        BigInteger currentP = p;
        BigInteger currentQ = q;
        BigInteger currentN = n;
        while(true)
        {
            Entry<BigInteger, PQRelations> entry = relationsMap.floorEntry(remainingNPlus);
            if(entry == null)
            {
                break;
            }
            
            BigInteger nJump = entry.getKey();
            remainingNPlus = remainingNPlus.subtract(nJump);
            
            PQRelations relations = entry.getValue();
            BigInteger newP = this.calculate(relations.getpRelation(), currentP, currentQ, currentN).mod(m);
            BigInteger newQ = this.calculate(relations.getqRelation(), currentP, currentQ, currentN).mod(m);
            
            currentP = newP;
            currentQ = newQ;
            currentN = currentN.add(nJump);
        }
        
        PQPair pair = new PQPair();
        pair.setP(currentP);
        pair.setQ(currentQ);
        return pair;
    }
    
    private BigInteger calculate(Recurrence relation, BigInteger p, BigInteger q, BigInteger n)
    {
        BigInteger result = relation.getpCoefficient().multiply(p)
                .add(relation.getqCoefficient().multiply(q))
                .add(relation.getnCoefficient().multiply(n))
                .add(relation.getConstant());
        return result;
    }
    
    private PQRelations multiply(PQRelations relations1, PQRelations relations2)
    {
        BigInteger level = relations1.getLevel().add(relations2.getLevel());
        Recurrence pRelation = this.multiply(relations1, relations2.getpRelation());
        Recurrence qRelation = this.multiply(relations1, relations2.getqRelation());
        PQRelations relations = new PQRelations(level, pRelation, qRelation);
        return relations;
    }

    private Recurrence multiply(PQRelations relations, Recurrence connection)
    {
        Recurrence pRelation = relations.getpRelation();
        Recurrence qRelation = relations.getqRelation();
        BigInteger level = relations.getLevel();
        
        Recurrence newRelation = new Recurrence();
        BigInteger pXpp = connection.getpCoefficient().multiply(pRelation.getpCoefficient());
        BigInteger qXqp = connection.getqCoefficient().multiply(qRelation.getpCoefficient());
        newRelation.setpCoefficient(pXpp.add(qXqp).mod(m));
        BigInteger pXpq = connection.getpCoefficient().multiply(pRelation.getqCoefficient());
        BigInteger qXqq = connection.getqCoefficient().multiply(qRelation.getqCoefficient());
        newRelation.setqCoefficient(pXpq.add(qXqq).mod(m));
        BigInteger pXpn = connection.getpCoefficient().multiply(pRelation.getnCoefficient());
        BigInteger qXqn = connection.getqCoefficient().multiply(qRelation.getnCoefficient());
        BigInteger n = connection.getnCoefficient();
        newRelation.setnCoefficient(pXpn.add(qXqn).add(n).mod(m));
        BigInteger pXpc = connection.getpCoefficient().multiply(pRelation.getConstant());
        BigInteger qXqc = connection.getqCoefficient().multiply(qRelation.getConstant());
        BigInteger c = connection.getnCoefficient().multiply(level).add(connection.getConstant());
        newRelation.setConstant(pXpc.add(qXqc).add(c).mod(m));
        
        return newRelation;
    }


	public BigInteger getM() {
		return m;
	}
}
