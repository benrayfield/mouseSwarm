package mutable.mouseswarm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PowvoxelType{
	
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
	
	/** https://github.com/multiformats/multicodec/blob/master/table.csv
	What kind of secureHash of the powvoxel bytes to calculate proofOfWork?
	For example, "dbl-sha2-256	multihash	0x56" is in that table.csv,
	and TODO it used to (in earlier versions of multihash) need a varint
	for size of the hash, but it seems in table.csv its expanded to
	possibly not need that. TODO Use the size varint only if
	theres anything in that table.csv that can have multiple sizes
	and all those possible sizes arent already in the table separately.
	<br><br>
	dbl-sha2-256 or sha3-256 are recommended for this software.
	*/
	public final Number multihashType;
	
	public final List<PowvoxelField> fields;
	
	public PowvoxelType(Number multihashType, PowvoxelField... fields){
		this.multihashType = multihashType;
		this.fields = Collections.unmodifiableList(Arrays.asList(fields.clone()));
		this.size = fields[fields.length-1].toExcl;
	}

}
