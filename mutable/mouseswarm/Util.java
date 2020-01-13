package mutable.mouseswarm;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mutable.time.Time;

/** long[4] is same as byte[32] */
public class Util{
	
	/** map of colorRGB (2^24 of them) to byte[32] as defined in
	https://www.reddit.com/r/mouseswarm/comments/enr9tf/a_possible_32_byte_data_structure_for_mouseswarm/
	TODO? allocate more memory to java at startup (using bat in windows and sh in linux)
	so can allocate byte[1<<29] aka 2^32 bits, 1 byte[32] for each possible colorRGB,
	but that would interfere with testing multiple MouseSwarmers (this object type)
	together, so for that keep the sparse Map.
	*/
	public static Map<Integer,long[]> state = new HashMap();
	public static long[] get(int color){
		return state.get(normColor(color));
	}
	public static void put(long[] powvoxel){
		state.put(color(powvoxel), powvoxel);
	}
	
	public static byte[] doubleSha256(byte[] in){
		MessageDigest md = null;
		try{
			md = MessageDigest.getInstance("SHA-256");
		}catch (NoSuchAlgorithmException e){ throw new Error(e); }
		byte[] sha256Output = md.digest(in);
		md.reset();
		return md.digest(sha256Output);
	}
	
	/** this is where you can change the hash function */
	public static byte[] hash(byte[] in){
		return doubleSha256(in);
	}
	
	public static byte[] longsToBytes(long[] a){
		byte[] b = new byte[32];
		int offset = 0;
		for(int i=0; i<4; i++){
			for(int j=56; j>=0; j-=8){
				b[offset++] = (byte)(a[i]>>>j);
			}
		}
		return b;
	}
	
	public static long[] bytesToLongs(byte[] b){
		long[] a = new long[4];
		int offset = 0;
		for(int i=0; i<4; i++){
			long g = 0;
			for(int j=0; j<8; j++){
				g = g<<8 | (b[offset++]&0xffL);
			}
			a[i] = g;
		}
		return a;
	}
	
	public static strictfp double bytesOfBiguintToDouble(byte[] bigUint){
		//TODO optimize
		double scale = 1;
		double sum = 0;
		for(int i=bigUint.length-1; i>=0; i--){
			scale *= 256;
			sum += scale*(bigUint[i]&0xff);
		}
		return sum;
	}
	
	/** by default this is doubleSha256 but might want to switch to
	sha3-256 or some kind of slow-hash (that runs fasterin cpu than gpu) later.
	You get the top 53 bits of precision starting at the highest 1 bit.
	TODO verify theres not roundoff in the implementation,
	even though correct double math guarantees you can get exactly those 53 bits.
	*/
	public static double hashAsDouble(long[] a){
		byte[] hash = hash(longsToBytes(a));
		return bytesOfBiguintToDouble(hash);
	}
	
	public static final int hashSizeInBits = hash(new byte[0]).length*8;
	public static final double twoExponentHashSizeInBits = Math.pow(2, hashSizeInBits);
	
	/** approx number of hash cycles needed to create it. */
	public static double proofOfWork(long[] a){
		return twoExponentHashSizeInBits/(1+hashAsDouble(a));
	}
	
	public static long intsToLong(int high, int low){
		return (((long)high)<<32)|(low&0xffffffffL);
	}
	
	public static int shortsToInt(short high, short low){
		return (((int)high)<<16)|(low&0xffff);
	}
	
	public static long shortsToLong(short a, short b, short c, short d){
		return intsToLong(shortsToInt(a,b), shortsToInt(c,d));
	}
	
	/** xVelByHalflife is x at a later time when proofOfWork is half as strong
	and defines a velocity from (x,y).
	You get 5 bytes of nonce in low5BytesAreExtraNonce
	and if you need more can put it in low bits of claimOfWork
	and low bits of utcNanoseconds.
	claimOfWork <= proofOfWork, but you can create an invalid one here
	so make sure to test it with isValid(long,long[]). 
	*/
	public static long[] create(double claimOfWork, long utcNanoseconds,
			short x, short y, short xVelByHalflife, short yVelByHalflife,
			long low5BytesAreExtraNonce, int colorRGB){
		return new long[]{
			Double.doubleToLongBits(claimOfWork),
			utcNanoseconds,
			shortsToLong(x,y,xVelByHalflife,yVelByHalflife),
			((low5BytesAreExtraNonce<<24)|(colorRGB&0xffffff))
		};
	}
	
	/** ieee754 norm */
	public static long normDoubleBits(long doubleBits){
		return Double.doubleToLongBits(Double.longBitsToDouble(doubleBits));
	}

	/** the bigger a claimOfWork, the longer this will take, so start small */
	public static long[] createStrong(Random rand, double claimOfWork, long utcNanoseconds, short x, short y, float xVelocity, float yVelocity){
		throw new Error("TODO");
	}
	
	/** Once its valid, its forever valid. It becomes valid
	when the time inside it <= now, if claimOfWork <= proofOfWor.
	*/
	public static boolean isValid(long[] a, long utcNanosecondsNow){
		//FIXME allow all 2^24 colors?
		//or use this as a small space (1/64 of the whole space) to put in other data
		//such as memory mapping of the current utcNanoseconds is 64 bits,
		//if memory mapping a size 2^32 space?
		if(log2OfHalflifeNanoseconds(a) == 63) return false; //cuz time is int64 not uint64
		if(normDoubleBits(a[0]) != a[0]) return false;
		if(claimOfWork(a) > proofOfWork(a)) return false;
		if(utcNanoseconds(a) > utcNanosecondsNow) return false;
		return true;
	}
	
	public static double claimOfWork(long[] a){
		//As optimization, use doubleToRawLongBits instead of doubleToLongBits
		//here cuz isValid is only true when those equal.
		return Double.doubleToRawLongBits(a[0]);
	}
	
	public static long utcNanoseconds(long[] a){
		return a[1];
	}
	
	/** returns a colorARGB with full opacity (not transparent),
	even though only RGB are stored.
	*/
	public static int color(long[] a){
		return normColor((int)a[3]);
	}
	
	/** converts colorRGB to colorARGB with full opacity (not transparent)
	FIXME If log2OfHalflifeNanoseconds is 63 that might not be isValid of color,
	so todo choose a design on that, and if thats not allowed
	then norm that 1/64 of the possible colors to the nearest color.
	*/
	public static int normColor(int color){
		return 0xff000000|color;
	}
	
	/** 0 to 63, but (TODO choose a design) !isValid(a) if
	its 63 cuz time is int64 not uint64.
	*/
	public static byte log2OfHalflifeNanoseconds(long[] a){
		int color = color(a);
		//<ignore14><use2><ignore6><use2><ignore6><use2>
		return (byte)(((color>>12)&0b110000)|((color>>6)&0b1100)|(color&0b11));
	}
	
	/** a powerOf2 */
	public static long halflifeNanoseconds(long[] a){
		return 1L<<log2OfHalflifeNanoseconds(a);
	}
	
	/** -1 if null. 0 if starts in future. else is nonnegative. */
	public static double claimOfWorkAtTime(long[] a, long utcNanosecondsContext){
		if(a == null) return -1;
		long timeCreated = utcNanoseconds(a);
		if(utcNanosecondsContext < timeCreated) return 0;
		long age = timeCreated-utcNanosecondsContext;
		long halflife = halflifeNanoseconds(a);
		double claimOfWorkAtStart = claimOfWork(a);
		double ageInHalflifes = (double)age/halflife;
		if(ageInHalflifes < 0) return 0; //FIXME is this aleready covered by "if(utcNanosecondsContext < timeCreated) return 0;"?
		return claimOfWorkAtStart*Math.pow(.5,ageInHalflifes);
	}
	
	/** ignore any incoming byte[32]powvoxel other than these colorRGBs */
	//TODO public Set<Integer> subscribes = new HashSet();
	
	/** returns -1 if a<b, 0 if equal, 1 if a>b. null sorts less than nonnull. */
	public static int compare(long[] a, long[] b, long utcNanosecondsNow){
		//claimOfWorkAtTime returns 0 if starts in future
		//claimOfWorkAtTime returns -1 for null.
		double aw = claimOfWorkAtTime(a, utcNanosecondsNow);
		double bw = claimOfWorkAtTime(a, utcNanosecondsNow);
		if(aw < bw) return -1;
		if(aw > bw) return 1;
		return 0;
	}
	
	/** either add to the Map or replace one, depending on the sorting defined
	in https://www.reddit.com/r/mouseswarm/comments/enr9tf/a_possible_32_byte_data_structure_for_mouseswarm/
	Returns true if changed state, which (TODO) peer that sent it here should
	be rewarded in the counting of titForTat.
	*/
	public boolean accept(long[] powvoxel, long utcNanosecondsNow){
		int color = color(powvoxel);
		long[] existingPowvoxel = get(color);
		int c = compare(existingPowvoxel, powvoxel, utcNanosecondsNow);
		if(c < 0){
			put(powvoxel);
			return true;
		}
		return false;
	}
	
	/** TODO use the func with Rectangle parameter. Optimizing that might
	require a quadtree instead of just a map or array of all of them.
	*/	
	public static void paint(Graphics g){
		paint(g, Time.nowNano());
	}

	public static void paint(Graphics g, long utcNanoseconds){
		for(long[] powvoxel : state.values()){
			paint(g, powvoxel, utcNanoseconds);
		}
	}

	public static short xStart(long[] powvoxel){
		return (short)(powvoxel[2]>>>48);
	}
	
	public static short yStart(long[] powvoxel){
		return (short)(powvoxel[2]>>>32);
	}
	
	public static short xAtHalflife(long[] powvoxel){
		return (short)(powvoxel[2]>>>16);
	}
	
	public static short yAtHalflife(long[] powvoxel){
		return (short)powvoxel[2];
	}
	
	public static float x(long[] powvoxel, long utcNanoseconds){
		short xStart = xStart(powvoxel);
		short xAtHalflife = xAtHalflife(powvoxel);
		long timeCreate = utcNanoseconds(powvoxel);
		long halflife = halflifeNanoseconds(powvoxel);
		//FIXME check for divideBy0?
		float xPerNanosecond = (float)(xAtHalflife-xStart)/halflife;
		long nanoDiff = utcNanoseconds-timeCreate;
		return xStart+xPerNanosecond*nanoDiff;
	}
	
	public static float y(long[] powvoxel, long utcNanoseconds){
		short yStart = yStart(powvoxel);
		short yAtHalflife = yAtHalflife(powvoxel);
		long timeCreate = utcNanoseconds(powvoxel);
		long halflife = halflifeNanoseconds(powvoxel);
		//FIXME check for divideBy0?
		float yPerNanosecond = (float)(yAtHalflife-yStart)/halflife;
		long nanoDiff = utcNanoseconds-timeCreate;
		return yStart+yPerNanosecond*nanoDiff;
	}
		
	
	public static void paint(Graphics g, long[] powvoxel, long utcNanoseconds){
		float x = x(powvoxel,utcNanoseconds);
		float y = y(powvoxel,utcNanoseconds);
		int color = color(powvoxel);
		int size = 5;
		g.drawRect((int)(x-size/2), (int)(y-size/2), size, size);
	}
	
	/*public void paint(Graphics g, Rectangle paintWhichPartOf64kBy64kBlockchainsState){
		throw new Error("TODO");
	}*/
	
	/** reuse same int24 colorRGB if have been using it recently
	and its not getting too much competition,
	else select whichever colorRGB is being streamed by peers
	and has the lowest proofOfWork averaged over the last few seconds,
	aka choose whichever powvoxel is least likely that another person
	is trying to use it.
	*/
	public void localUserMovesMouseAndPublishesToState(short mouseX, short mouseY){
		throw new Error("TODO");
	}
	
	/** observe other MouesSwarmer and modify my state, but dont modify its state */
	public void accept(long utcNanosecondsNow, Util peer){
		for(long[] powvoxel : peer.state.values()){
			accept(powvoxel, utcNanosecondsNow);
		}
	}
	
	//TODO make this work in both weupnp occamserver AND in browser
	//served by those, where browser computes doubleSha256 much slower than CPU
	//so would have less influence.
	
	
	
	/*
	FIXME maybe put proofofwork on pairs?
			probably better to have a constant number of sparse moving points
			named by forexample an int12.
			
			FIXME need log number of powof2 aligned time sizes
			so the points dont disappear if nobody is sending them,
			and in the paint kind of game its ok for pixels to be jumpy
			but in mouse movement (econball-like) kind of game
			its more important for it to move smoother
			so I need to adjust how the proofOfWork works to make it
			smooth and maybe use all log number of them at once,
			gradually changing between them like each of them
			is a bellcurve of time range of how much it influences at that time,
			but then people would be motivated to put in a huge negative
			or huge positive number just to average it,
			so median would be better for that,
			but median isnt smooth. Maybe the claimOfWork (claimOfWork <= proofOfWork),
			which can be chosen smoothly, should be used in the positions?
			How about log(claimOfWorkA)*vecA+log(claimOfWorkB)*vecB+log(claimOfWorkC)*vecC
			would be its position where those are 3 constant unit vectors spread around
			a circle. Combining that with the bellcurves in time,
			it would be smoother, still partially jumpy,
			but maybe too hard to control since multiple bellcurves are summed into it.
			Or how about its log number of choosable vectors that each can choose radius
			up to 1 and any angle, sum those vectors, and for each timesize (log num of them)
			its a bellcurve of bigger or smaller time centered on some time
			(powof2 aligned time).
			claimOfWork is divided by time size it applies to.
			Might work. Harder to know what to add to it if you arent synced yet
			(which is fraction of a second but gamers hate any noticable lag).
			What I really want is N movable points updated 30 times per second,
			but not every point will get updated every time so you need various time sizes
			to motivate players to stay while theres fewer players doing things
			as the things they do will be visible longer.
			Maybe each movable point should have an interval of how long it stays there,
			a powOf2 aligned interval, such as some points move
			every 2^30 nanoseconds (1.07 seconds), some move every 2^25 seconds (realtime),
			some move every 2^55 seconds (about once per year), etc,
			and claimOfWork/proofOfWork is of course divided by the time size.
			Maybe those movable points should have 2 ends so move linearly,
			or maybe they should have accel to or various curve fitting...
			2 ends will be good enough for now.
			Or could interpolate between the nearest 2 positions
			2 separate proofofworks/powvoxels.
			Yes, this is likely to motivate players.
			Or maybe paths should be defined as circle with velocity and radius
			and angle at a certain time and update the circle on timeInterval
			(where theres log number of timeintervals, only 1 time interval per movablepoint).
			Somehow Id like it to keep going around that same circle if updates dont come
			instead of disappearing? Or maybe disappearing is good cuz its not in use?
			...
			Since theres constant number of movablepoints,
			the algorithm, given gamer moving mouse, would be to choose whichever
			point has lowest claimOfWork (claimOfWork <= proofOfWork) and try to
			control that one, basically whichever is least likely to be in use
			by another player.
			...
			Since each movablepoint only has 1 timesize (theres log timesizes,
			a different group of movablepoints for each, all on the same screen),
			claimOfWork at time can be defined as decaying at a rate inversely
			proportional to timesize (halflife) and its still exactly solvable
			which of any 2 conflicting data is used and spread across the network
			and the other ignored. A higher claimOfWork in the past is replaced
			by a lesser claimOfWork now depending only on their 2 strengths at now.
			2 curves decaying at the same rate never cross eachother.
			There are log number of halflifes. Each movablepoint has 1 constant halflife.
			All points are on screen all the time.
			...
			Each powvoxel (which there are sparse number of for each movablepoint,
			keeps getting new ones over time)... is a bitstring interpreted as
			these fields:
			..
			multiformats-varint (normally 1 byte) of bit size of this datastruct.
			multihash-type (TODO and size) for proofOfWork?
			//multiple "rooms" can be done with this datastruct
			//by forking the opensource to only accept which of these datastructs
			//that have certain low bits in some of the fields
			//such as the low 8 bits of utcNanoseconds could choose between 256 rooms.
			uint16 decayAndWhichMovablePoint;
			//decay rate is 2 exponent decay. decay group are the high 6 bits.
			//point within that group is the low 10 bits. There are 2^16 movablepoints.
			//These are sortable by their bits as if they were a single integer,
			//since all the positive double values if viewed as raw 64 bits.
			float claimOfWork //part of nonce can be in low bits
			long utcNanoseconds //part of nonce can be in low bits
			float x
			float y
			//part of nonce can be in low bits of x and y positions and velocities?
			float xVelocity; //TODO what encoding of velocity? relative to halflife?
			float yVelocity;
			//color?
			byte red
			byte green
			byte blue
			
			Generalize to integer key and integer value (so its useful for other things),
			where part of key bits is powOf2 decay rate and how to calculate proofOfWork?
			The key would contain decayAndWhichMovablePoint,
			and utcNanoseconds would not be part of key or value but is
			how its updated over time.
			red green blue x y xVelocity yVelocity etc would be in value.
			Key and value would be constant sizes? No.
			Key is variable size and some of its low bits tell the
			size of its value. All values for same key are same size.
			There are an infinite number of possible keys.
			The system is always timeful and always instantly syncs perfectly
			globally between all computers and all computers if they
			are able to organize among eachother where to send these powvoxels
			which is a different problem than sorting the values per key.
			
			powvoxel datastruct (this is whats sent through network):
			..
			multiformats-varint (normally 1 byte) of bit size of this datastruct
			double claimOfWork //can put nonce in low bits
			long utcNanoseconds //can put nonce in low bits
			key
			extraNonce
			value
			
			extraNonce datastruct:
			..
			//multiformats-varint howManyBytesOfExtraNonce;
			byte[key.howManyBytesOfExtraNonce] extraNonceCanBeAnything
			
			key datastruct:
			..
			multiformats-varint (normally 1 byte) of bit size of this datastruct
			multihash-type-and-hashsize for computing proofOfWork
			byte decay; //halflife is 1L<<decay nanoseconds
			multiformats-varint howManyBytesOfExtraNonce;
			multiformats-varint (normally 1 byte) of value size in bytes
			multiformats-varint keyArbitraryDataSize
			byte[keyArbitraryDataSize] //this is where movablepointId goes if its for mouseswarm, for example
			
			value datastruct:
			..
			multiformats-varint (normally 1 byte) of bit size of this datastruct
			any chosen bits of the number of bits defined in the key minus the size of the size header
			
			
			do simpler thing first...
			...
			Powvoxel is 32 bytes. There is 1 movablepoint per colorRGB, 2^24 of them.
			Halflife (64 varying time sizes ranging halflife of 1 nanosecond to
			halflife of centuries)
			is stored in low 2 bits of red, low of green, low of blue.
			ProofOfWork algorithm is either log2(rawBits(doubleSha256(powvoxel))) or
			0 if claimOfWork > proofOfWork. Sorting and sharing is by claimOfWork.
			..
			double claimOfWork
			//newKindOfFloatingPointInInt64 claimOfWork //claimOfWork <= proofOfWork,
			//where claimOfWork = (2^(2^-56))^the64BitsAsUint64,
			//or something like that (TODO fix or verify that math).
			//so the high 8 bits choose a power of 2 and the low 56 bits
			//choose fractional powers of 2 such as sqrt(2) sqrt(sqrt(2)) etc.
			//TODO verify that math.
			//That has the advantage of more space in the low bits for nonce.
			int64 utcNanoseconds
			int16 x
			int16 y
			int16 xVelocityByGivingPositionAtItsHalflife
			int16 yVelocityByGivingPositionAtItsHalflife
			uint8[5] extraNonce //other nonce goes in low bits of claimOfWork and of utcNanoseconds
			uint8 redAndLow2BitsArePartOfDecayRate //halflife is nanosecond*(1L<<uint6decay)
			uint8 greenAndLow2BitsArePartOfDecayRate
			uint8 blueAndLow2BitsArePartOfDecayRate
			...
			Each computer will give a list of the int24 colors its both publishing and subscribing to
			(symmetricly publish and subscribe are the same thing),
			either sparsely or as a dense 2 megabyte block of bits
			one for each possible colorRGB.
			
			
			//dont: use bson instead of making a new binary mini-type system?
			
			public final short size;
			
			** https://github.com/multiformats/multicodec/blob/master/table.csv
			What kind of secureHash of the powvoxel bytes to calculate proofOfWork?
			For example, "dbl-sha2-256	multihash	0x56" is in that table.csv,
			and TODO it used to (in earlier versions of multihash) need a varint
			for size of the hash, but it seems in table.csv its expanded to
			possibly not need that. TODO Use the size varint only if
			theres anything in that table.csv that can have multiple sizes
			and all those possible sizes arent already in the table separately.
			<br><br>
			dbl-sha2-256 or sha3-256 are recommended for this software.
			*
			public final Number multihashType;
			
			public final List<PowvoxelField> fields;
			
			public PowvoxelType(Number multihashType, PowvoxelField... fields){
				this.multihashType = multihashType;
				this.fields = Collections.unmodifiableList(Arrays.asList(fields.clone()));
				this.size = fields[fields.length-1].toExcl;
			}
			*/

}
