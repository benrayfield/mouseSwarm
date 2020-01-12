package mutable.mouseswarm;

import java.util.HashMap;
import java.util.Map;

public class MouseSwarmer{
	
	/** map of colorRGB (2^24 of them) to byte[32] as defined in
	https://www.reddit.com/r/mouseswarm/comments/enr9tf/a_possible_32_byte_data_structure_for_mouseswarm/
	TODO? allocate more memory to java at startup (using bat in windows and sh in linux)
	so can allocate byte[1<<29] aka 2^32 bits, 1 byte[32] for each possible colorRGB,
	but that would interfere with testing multiple MouseSwarmers (this object type)
	together, so for that keep the sparse Map.
	*/
	public Map<Integer,byte[]> state = new HashMap();
	
	/** ignore any incoming byte[32]powvoxel other than these colorRGBs */
	public Set<Integer> subscribes = new HashSet();
	
	/** either add to the Map or replace one, depending on the sorting defined
	in https://www.reddit.com/r/mouseswarm/comments/enr9tf/a_possible_32_byte_data_structure_for_mouseswarm/
	*/
	public void accept(long utcNanosecondsNow, byte[] newPowvoxel){
		TODO get Integer (intRGB) color from newPowvoxel,
		get key from Map state, compare them in context of utcNanosecondsNow,
		and keep the max.
	}
	
	public void paint(Graphics g, Rectangle paintWhichPartOf64kBy64kBlockchainsState){
		TODO
	}
	
	/** reuse same int24 colorRGB if have been using it recently
	and its not getting too much competition,
	else select whichever colorRGB is being streamed by peers
	and has the lowest proofOfWork averaged over the last few seconds,
	aka choose whichever powvoxel is least likely that another person
	is trying to use it.
	*/
	public void localUserMovesMouseAndPublishesToState(short mouseX, short mouseY){
		TODO
	}
	
	/** observe other MouesSwarmer and modify my state, but dont modify its state */
	public void accept(long utcNanosecondsNow, MouseSwarmer peer){
		for(byte[] powvoxel : peer.state.values()){
			accept(utcNanosecondsNow, powvoxel);
		}
	}
	
	TODO make this work in both weupnp occamserver AND in browser
	served by those, where browser computes doubleSha256 much slower than CPU
	so would have less influence.

}