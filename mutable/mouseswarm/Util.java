package mutable.mouseswarm;

public class Util{
	
	/** This is an optimization of proofOfWork so you dont have
	to verify it every time and can instead disconnect from those
	in the network that send you too many invalid proofOfWorks,
	and its a way to cache the proofOfWork inside the data its about.
	<br><br>
	claimOfWork <= proofOfWork.
	While creating a data to do proofOfWork on,
	a claimOfWork is automatically chosen by how much
	computing power you have, and its hashed to get proofOfWork,
	and if claimOfWork > proofOfWork then its an invalid proofOfWork.
	the claimOfWork's low bits are part of the nonce and  
	*/
	public static double claimOfWork(byte[] powvoxel){
		TODO should it be byte[] or int[] or float[] or double[]?
	}

}
