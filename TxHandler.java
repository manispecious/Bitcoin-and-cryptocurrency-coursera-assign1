package com;

import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	 private UTXOPool utxoCollectionPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
    	utxoCollectionPool= new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction transaction) {
        // IMPLEMENT THIS
    	 double sumOfInput = 0;
         double sumOfOutput = 0;
         ArrayList<UTXO> usedUTXO = new ArrayList<>();

         for (int i=0;i<transaction.numInputs();i++) {
        	 //for each input in the transaction check for the three rules among the five
             Transaction.Input input = transaction.getInput(i);
             int outputIndex = input.outputIndex;
             byte[] prevTxHash = input.prevTxHash;
             byte[] signature = input.signature;

             UTXO utxo = new UTXO(prevTxHash, outputIndex);
             
             // (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
             if (!utxoCollectionPool.contains(utxo)) {
                 return false;
             }
             //(2) the signatures on each input of {@code tx} are valid, 
             Transaction.Output output = utxoCollectionPool.getTxOutput(utxo);
             byte[] message = transaction.getRawDataToSign(i);
             if (!Crypto.verifySignature(output.address,message,signature)) {
                 return false;
             }
             // (3)no UTXO is claimed multiple times by transaction in input of transaction
             if (usedUTXO.contains(utxo)) {
                 return false;
             }
             usedUTXO.add(utxo);
            // sumInput += output.value;
         }
         // all of {@code tx}s output values are non-negative
         for (int i=0;i<transaction.numOutputs();i++) {
             Transaction.Output output = transaction.getOutput(i);
             if (output.value < 0) {
                 return false;
             }
             sumOfOutput += output.value;
         }
         //the sum of {@code tx}s input values is greater than or equal to the sum of its output values; and false otherwise.
         for (int i=0;i<transaction.numInputs();i++) {
        	 Transaction.Input input = transaction.getInput(i);
        	 int outputIndex = input.outputIndex;
             byte[] prevTxHash = input.prevTxHash;
             UTXO utxo = new UTXO(prevTxHash, outputIndex);
             Transaction.Output output = utxoCollectionPool.getTxOutput(utxo);
             sumOfInput += output.value;
         }
         if (sumOfInput < sumOfOutput) {
             return false;
         }
         return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    	//for dynamic add of transaction that is valid to : validTxs
        ArrayList<Transaction> validTxs = new ArrayList<>();
        for (Transaction t : possibleTxs) {
            if (isValidTx(t)) {
                validTxs.add(t);

                //remove utxo
                /** 
                 * removing inputs of the transaction from UTXO collection pool and and adding outputs of the transaction
                 * to UTXO collection pool ,hence maintaining the only unspent coins in UTXO pool which is used in verification */
                for (Transaction.Input input : t.getInputs()) {
                    int outputIndex = input.outputIndex;
                    byte[] prevTxHash = input.prevTxHash;
                    UTXO utxo = new UTXO(prevTxHash, outputIndex);
                    utxoCollectionPool.removeUTXO(utxo);
                }
                //add new utxo
                byte[] hash = t.getHash();
                for (int i=0;i<t.numOutputs();i++) {
                    UTXO utxo = new UTXO(hash, i);
                    utxoCollectionPool.addUTXO(utxo, t.getOutput(i));
                }
            }
        }
      //converting array list to array(validTxsArr) by taking size of dynamic valid transactions added in: validTxs
        Transaction[] validTxsArr = new Transaction[validTxs.size()];
        validTxsArr = validTxs.toArray(validTxsArr);
        return validTxsArr;
    	
    }

}
