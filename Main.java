package com;


import java.math.BigInteger;
import java.security.*;

public class Main {

    public static void main(String[] crypto) throws  NoSuchAlgorithmException, SignatureException {

    // generating public and private keys for (address and doing signature)
        KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice   = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_chuck   = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        /*
         * This is creating a Initial transaction which says scrooge owns 20 (10*2)coins
         */
        Tx tx1 = new Tx();
        tx1.addOutput(10, pk_scrooge.getPublic());
        tx1.addOutput(10, pk_scrooge.getPublic());
        
        // This is for creating a hash since it will be used in next transaction in( tx.getRawDataToSign(0) will access it in prevTxHash)
        byte[] initialHash = BigInteger.valueOf(0).toByteArray();
        tx1.addInput(initialHash, 0);
       // scrooge need to sign this transaction since he owns the coins
        
        
        tx1.signingTransaction(pk_scrooge.getPrivate(), 0);

        /*
         * This is the creation of centralised pool(utxo pool) which contains all the utxo (hash,index)
         */
       //since scrooge has two coins of 10 so both the utxo(hash,index) to be added in pool
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx1.getHash(),0);
        utxoPool.addUTXO(utxo, tx1.getOutput(0));
        UTXO utxo2 = new UTXO(tx1.getHash(),1);
        utxoPool.addUTXO(utxo2, tx1.getOutput(1));
        /*
         * Here the basic set up completed for scrooge 
         * tx(0)->10, tx(1)->10 
         */  
        
        
        Tx tx2 = new Tx();
        // The input of this transaction is scrooge 10 coins at index=0 ,so the transaction need to be signed by scrooge with private key
        tx2.addInput(tx1.getHash(), 0);
        
        //Alice want the coins in (5,3,2)
        tx2.addOutput(5, pk_alice.getPublic());
        tx2.addOutput(3, pk_alice.getPublic());
        tx2.addOutput(2, pk_alice.getPublic());
        
        tx2.signingTransaction(pk_scrooge.getPrivate(), 0);
        
        /*
         * UPto now the UTXOpool contains two unspent coins of scrooge 
         */ 
        TxHandler txHandler = new TxHandler(utxoPool);
        System.out.println("Check whether {tx2} is Valid" + txHandler.isValidTx(tx2));
        System.out.println("txHandler.handleTxs(new Transaction[]{tx2}) returns: " +
                txHandler.handleTxs(new Transaction[]{tx2}).length + " transaction(s)");
        /*
         * Now the UTXOpool contains 10(scrooge),5(Alice),3(Alice),2(Alice)
         */ 
        
        Tx tx3 = new Tx();
        /*
         * The inputs for this transaction from 5(Alice) from tx2 and 10(Scrooge) from  tx1 so scrooge and alice need to sign for this transaction
         */ 
        tx3.addInput(tx2.getHash(), 0);
        tx3.addInput(tx1.getHash(), 1);
    
        tx3.addOutput(2, pk_chuck.getPublic());
        tx3.addOutput(2, pk_chuck.getPublic());
        tx3.addOutput(1, pk_chuck.getPublic());
        tx3.addOutput(10, pk_alice.getPublic());
    
        
        tx3.signingTransaction(pk_alice.getPrivate(), 0);
        tx3.signingTransaction(pk_scrooge.getPrivate(), 1);
     
        System.out.println("Check whether {tx3} is Valid:" + txHandler.isValidTx(tx3));
        System.out.println("txHandler.handleTxs(new Transaction[]{tx3}) returns: " +
        		txHandler.handleTxs(new Transaction[]{tx3}).length + " transaction(s)");
        /*
         * The collection pool now updates to 3(Alice),2(Alice),2(chuck),2(chuck),1(chuck),10(Alice)
         */ 
        
    }


    public static class Tx extends Transaction {
    	//throws SignatureException for addSignature
        public void signingTransaction(PrivateKey sk, int input) throws SignatureException {
            Signature sig = null;
            try {
            	//NoSuchAlgorithmException for getInstance of "SHA256withRSA"
            	//InvalidKeyException for initSign if private key fail
                sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(sk);
                sig.update(this.getRawDataToSign(input));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
            	throw new RuntimeException(e);
			}
            /*
             * updating sign for the present transaction
             */
            this.addSignature(sig.sign(),input);
            /*
             * This is the method which  uses provided getRawTx() method to generate Hash using inputs(prevhash,index of input output in prev tx ,sign)
             * and outputs(value,public key of who was being assigned to coin)
             */
            this.finalize();
        }
    }
}