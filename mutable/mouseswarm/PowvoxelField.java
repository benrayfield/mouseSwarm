package mutable.mouseswarm;

/** a type of field in a subrange of a bitstring.
This is part of a class, not the bitstrings themself.
*/
public final class PowvoxelField{
	
	public final String name;
	
	/** bit indexs */
	public final short from, toExcl;
	
	public PowvoxelField(String name, short from, short toExcl){
		this.name = name;
		this.from = from;
		this.toExcl = toExcl;
	}

}
