
DroneGroup {

	var <>dronearray;
	var <selected;
	var hub, name;
	
	*new { | hub, name |
		^super.new.initDroneGroup( hub, name );
	}

	initDroneGroup { | arghub, argname |
		name = argname;
		hub = arghub;
	}

	createGroup { | argname, type, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave  | // instantiate a new group (not define it) - as in createDrone, createChord
		var grp, drone;
		var tonicoffset, degreeoffset, ratiooffset, octaveoffset; // pitch offsets (os_)
		dronearray = [];
		selected = false;	
		grp = hub.states.storedGroupsDict.at(argname); // a dict with drones (name + synthParams)
		if(tonic.isNil.not, {
			tonicoffset = tonic - grp.keys.asArray.collect({ | key | grp[key].tonic }).minItem;
		});
		\dev1.postln;
		if(degree.isNil.not, {
			degreeoffset = degree - grp.keys.asArray.collect({ | key | grp[key].degree }).minItem;
		});
		\dev2.postln;
		if(ratio.isNil.not, {
			ratiooffset = ratio - grp.keys.asArray.collect({ | key | grp[key].ratio }).minItem;
		});
		\dev3.postln;
		if(octave.isNil.not, {
			octaveoffset = octave - grp.keys.asArray.collect({ | key | grp[key].octave }).minItem;
		});
		[\KEYS, grp.keys].postln;
		grp.keys.do({ | key , i|
			[\key, key].postln;
			drone = hub.drones.createDrone(
				if(type.isNil, {grp[key].type}, {type}), 
				if(tonic.isNil, {grp[key].tonic}, {grp[key].tonic+tonicoffset}), 
				if(harmonics.isNil, {grp[key].harmonics}, {harmonics.value}),  // the value here allows for passing functions, like in Drone, Chord, etc.
				if(amp.isNil, {grp[key].amp}, {amp.value}), 
				if(speed.isNil, {grp[key].speed}, {speed.value}), 
				if(length.isNil, {grp[key].length}, {length.value}), 
				if(angle.isNil, {grp[key].angle}, {angle.value}), 
				if(degree.isNil, {grp[key].degree}, {grp[key].degree+degreeoffset}), 
				if(ratio.isNil, {grp[key].ratio}, {grp[key].ratio+ratiooffset}), 
				if(true, {name++"_"++grp[key].name}, {name}), // XXX FINISH
				if(env.isNil, {grp[key].env}, {env}), 
				if(octave.isNil, {grp[key].octave}, {grp[key].octave+octaveoffset}),
				post:false
				);
				\dev4_44.postln;
			dronearray = dronearray.add( drone );
		});
		\dev5.postln;
	}

	// define a group from existing drones
	defineGroup { | ...args |
		var argsDict;
	
		dronearray = [];
		argsDict = ();
		args = args.flatten;
		args.do({ | dronename | 
			var droneState;			
			droneState = hub.drones.droneDict[dronename.asSymbol].synthParams;
			droneState.add('speed' -> (hub.drones.droneDict[dronename.asSymbol].speed*1000));
			droneState.add('length' -> (hub.drones.droneDict[dronename.asSymbol].length * 360 / (2*pi) ));
			droneState.add('angle' -> ((hub.drones.droneDict[dronename.asSymbol].rotation * 360 / (2*pi)) + (hub.drones.droneDict[dronename.asSymbol].length* 360 / (2*pi)) ));
			droneState.add('ratio' -> hub.drones.droneDict[dronename.asSymbol].ratio);
			dronearray = dronearray.add( hub.drones.droneDict[dronename.asSymbol] );
			argsDict.add( dronename.asSymbol -> droneState ) 
		});
		hub.states.storeGroup( name, argsDict );
	}

	selected_ { | bool |
		selected = bool;
		dronearray.do({ |drone| drone.selected = bool });
	}

	changeScale { | argscale, dur, transp=0 |
		var scale;
		if(argscale.isArray, { // its a custom scale
			scale = argscale;
		}, {  // it's a scale from the dict
			scale = Scale.newFromKey(argscale).degrees+1; // indexing by 1
		});		
		dronearray.do({ | drone | 
			drone.ratio_( drone.ratio.nearestInScale(scale)+transp, dur ) });
	}

	// groups can be either in chords or scales
	changeChord { | argchord, dur, transp=0  |
		var chord;
		if(argchord.isArray, { // its a custom scale
			chord = argchord;
		}, {  // it's a chord from the dict
			chord = hub.getChordDict[argchord]+1; // indexing by 1
		});		
		dronearray.do({ | drone |
			drone.ratio_( drone.ratio.nearestInScale(chord)+transp, dur ) });
	}

	env_ { |envt| dronearray.do({arg drone; drone.env = envt }) }
	tonic_ { |tonic,dur| dronearray.do({arg drone; drone.tonic_(tonic, dur) }) }
	relTonic_ { |change, dur| dronearray.do({arg drone; drone.relTonic_(change, dur) }) }
	freq_ { |freq, dur| 
		var fundamental, ratio;
		fundamental = dronearray.collect({|drone| drone.freq }).minItem;
		dronearray.do({arg drone; 
			ratio = drone.freq / fundamental;
			drone.freq_(freq*ratio, dur);
		}) 
	}// not working on a chord
	relFreq_ { |change, dur| dronearray.do({arg drone; drone.relFreq_(change, dur) }) }
	ratio_ { |ratio, dur| 
		var fundamental, offset;
		fundamental = dronearray.collect({|drone| drone.ratio }).minItem;
		dronearray.do({arg drone; 
			offset = drone.ratio - fundamental;
			drone.ratio_(ratio+offset, dur) }) 
	} // works
	relRatio_ { |change, dur| dronearray.do({arg drone; drone.ratio_(change+drone.ratio, dur) }) } // works
	degree_ { |degree, dur| 
		var fundamental, offset;
		fundamental = dronearray.collect({|drone| drone.degree }).minItem;
		dronearray.do({arg drone; 
			offset = drone.degree - fundamental;
			drone.degree_(degree+offset, dur) }) 
	} 
	relDegree_ { |change, dur| dronearray.do({arg drone; drone.degree_(change+drone.degree, dur) }) } // works

	// playRatios { |dur, slide| dronearray.do({arg drone; drone.playRatios(dur, slide) }) }
	// playDegrees { |dur, slide| dronearray.do({arg drone; drone.playDegrees(dur, slide) }) }
	// playScale { |dur, slide| dronearray.do({arg drone; drone.playScale(dur, slide) }) }
	octave_ { |octave, dur| dronearray.do({arg drone; drone.octave_(octave, dur) }) }
	relOctave_ { |change, dur| dronearray.do({arg drone; drone.relOctave_(change, dur) }) }
	transpose_ { |interval, dur| dronearray.do({arg drone; drone.transpose_(interval, dur) }) }
	interval_ { |interval, dur| dronearray.do({arg drone; drone.interval_(interval, dur) }) }
	harmonics_ { |harmonics, dur| dronearray.do({arg drone; drone.harmonics_(harmonics, dur) }) }
	resonance_ { |res, dur| dronearray.do({arg drone; drone.resonance_(res, dur) }) }
	amp_ { |amp, dur| dronearray.do({arg drone; drone.amp_(amp, dur) }) } // working
	amp { ^dronearray[0].amp } 
	relAmp_ { |change, dur| dronearray.do({arg drone; drone.relAmp_(change, dur) }) } // working
	speed_ { |speed| dronearray.do({arg drone; drone.speed_(speed) }) } // working
	relSpeed_ { |change| dronearray.do({arg drone; drone.relSpeed_(change) }) } // working
	angle_ { |angle| dronearray.do({arg drone; drone.angle_(angle) }) } // working
	length_ { |length| dronearray.do({arg drone; drone.length_(length) }) } // working
	relLength_ { |change| dronearray.do({arg drone; drone.relLength_(change) }) } // working
	tuning_ { |tuning, dur| dronearray.do({arg drone; drone.tuning_(tuning, dur) }) } // working
	scale_ { |scale, dur, transp=0| 
		this.changeScale(scale, dur, transp);
		dronearray.do({arg drone; drone.scale_(scale) }); 
	}
	chord_ { |chord, dur, transp=0| // transposition in ratios (half-notes if et12)
		this.changeChord(chord, dur, transp);
		dronearray.do({arg drone; drone.chord_(chord) }); // the chord of a drone
	}  
	type_ { |type| dronearray.do({arg drone; drone.type_(type) }) }
	set { | ...args | dronearray.do({arg drone; drone.set(*args) }) }
	freeSynths { |releasetime| dronearray.do({arg drone; drone.freeSynths(releasetime) }) } // the chord of a drone 
	kill { |releasetime| hub.drones.killGroup(name); dronearray.do({arg drone; drone.kill(releasetime) }) }
	auto_ { |bool| dronearray.do({arg drone; drone.auto_(bool) }) }
	clearauto { dronearray.do({arg drone; drone.clearauto() }) }
	recParam {| method, min, max, round=0 | hub.drones.rec_(dronearray, method, min, max, round) }
	setParam {| method, min, max, round=0 | hub.drones.setParam_(dronearray, method, min, max, round) }
	stopParam { | method | dronearray.do({arg drone; drone.stopParam(method) }) }

	perform { arg command;
		if(command.contains("tonic"), { // an exeption - bad idea - should simply use relative tonic
			var combinedstring, group, tonicoffset, tonicshift, tonicmove, interprstr;
			tonicoffset = command[8..command.size].asInteger;
			tonicshift = dronearray.collect({arg drone, i; drone.tonic});
			tonicmove = tonicoffset-tonicshift.minItem;
			tonicshift = tonicshift+tonicmove;  // (the logic -> 4-[4, 2, 7].minItem; [4,2,7]+2)
			dronearray.do({arg drone, i;
				combinedstring = ("~drones.droneDict[\\"++drone.name++"].tonic_("++tonicshift[i]++")");
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis group exists, but there is a syntax error".postln };
			});
		}, {
			dronearray.do({arg drone;
				var combinedstring, interprstr;
				combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis chord exists, but there is a syntax error".postln };
			})
		})
	}
}
