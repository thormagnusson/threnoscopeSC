
DroneChord {

	var <>dronearray;
	var <selected;
	var hub, <name;
	var chordtype, chordratios;
	var <tuning, <scale, scaledegrees; // scale is used for chord progressions
	
	*new { | hub, name, scale |
		^super.new.initDroneChord( hub, name, scale );
	}

	initDroneChord { | arghub, argname, argscale |
		hub = arghub;
		name = argname;
		scale = argscale;
	}
	
	createChord { | type=\saw, chord=#[1,5,8], tonic=1, harmonics, amp=0.2, speed, length, angle, degree=1, ratio=1, env, octave, note |

		var drone;
		chordtype = chord; // for outside info, see chord method below
		selected = false;
		dronearray = [];
		if(chord.isArray, { // its a custom chord
			//rat = "";
			//chord.do({arg i; rat = rat++i.asString });
			chordratios = chord;
		}, {  // it's a chord from the dict
			chordratios = hub.getChordDict.at(chord.asSymbol)+1; // indexing by 1
		});
		
		if(note.isNil.not, {
			[\hubkey, hub.key].postln;
			[\note, note].postln;
			ratio = (\A:1, \As:2, \Bb:2, \B:3, \C:4, \Cs:5, \Db:5, \D:6, \Ds:7, \Eb:7, \E:8, \F:9, \Fs:10, \Gb:10, \G:11, \Gs:12, \Ab:12).at(note.asSymbol);
			[\ratio, ratio].postln;
			ratio = (ratio - (\A:0, \As:1, \Bb:1, \B:2, \C:3, \Cs:4, \Db:4, \D:5, \Ds:6, \Eb:6, \E:7, \F:8, \Fs:9, \Gb:9, \G:10, \Gs:11, \Ab:11).at(hub.key.asSymbol)).mod(12);
			[\ratio, ratio].postln;
		});
		
		// a chord can be created in different ratios (which are halftones by default)
		chordratios.do({arg chordratio, i;
			var angleArg, lengthArg, harmonicsArg, drone;
			angleArg = angle.value ? 360.rand; // angle.value allows for functions to be passed, e.g. {rrand(100,190)}
			lengthArg = length.value ? (100+(160.rand));
			harmonicsArg = harmonics.value ? (2+(3.rand));

			drone = hub.drones.createDrone( type, tonic, harmonicsArg, amp, speed, lengthArg, angleArg, degree, chordratio+ratio-1, name++"_"++chordratio, env, octave, post:false );
			drone.chord_(chordratios); // make the drone aware of which chord it is a member of
			dronearray = dronearray.add(drone);
		});
		this.scale_(scale); // set the scaledegrees (if chord is to be harmonically transposed)
	}
	
	// define a chord from existing drones
	defineChord { | array | // XXX - TODO- is this working?
		Post << array;
		dronearray = array;
		dronearray.do({ | drone | drone.name.postln });
	}
	
	selected_ { | bool |
		selected = bool;
		dronearray.do({ |drone| drone.selected = bool });
	}
	
	printDroneArray {
		dronearray.do({arg drone; drone.name.println });
	}
	
	type {
		^("Chord type is : " ++ chordtype);
	}
	
	changeChord { | chord, dur, transp=0 |
		if(chord.isArray, { // its a custom chord
			chordratios = chord;
		}, {  // it's a chord from the dict
			chordratios = hub.getChordDict.at(chord.asSymbol)+1; // indexing by 1
		});
		
		if(chordratios.size == dronearray.size, {
			"--> SAME SIZE".postln;
			chordratios.do({ | ratio, i |
				dronearray[i].ratio_(ratio+transp, dur);
			});
		}, {
			if(chordratios.size < dronearray.size, { // if there are more drones than tunings 
				dronearray.copy.do({ | drone, i |
					if(chordratios[i].isNil, {
						"--> KILLING DRONE".postln;
						if(dur.isNil.not, { // if it's a time-change chord
							drone.amp_(0, dur); // and fade out
							//drone.ratio_(chordratios[i]+transp, dur);
							{var drname;
							drname = drone.name;
							drone.kill;
							dronearray.copy.do({ | drone, i |
								if(drone.name == drname, {
									dronearray.removeAt(i)
								});
							});
							}.defer(dur);
						}, {
							var drname;
							drname = drone.name;
							drone.kill;
							dronearray.copy.do({ | drone, i |
								if(drone.name == drname, {
									dronearray.removeAt(i)
								});
							});
						});
					}, {
						drone.ratio_(chordratios[i]+transp, dur);
					});
				});				
			}, {
				"--> YES - MORE RATIOS".postln;
				chordratios.do({ | ratio, i |
					if(dronearray[i].isNil, {
						var drone;
						"--> ADDING DRONE".postln;
						[\name, name].postln;
						drone = hub.drones.createDrone(
											dronearray[0].type,
											dronearray[0].tonic,
											dronearray[0].harmonics,
											dronearray[0].amp,
											dronearray[0].speed,
											rrand(100, 300),
											360.rand,
											1, // degree
											ratio, 
											name++"_"++ratio, 
											dronearray[0].env,
											dronearray[0].octave,
											post:false
										);
						dronearray = dronearray.add( drone );
						if(dur.isNil.not, { // if it's a time change chord
							drone.amp_(0); // start new drone with 0 vol
							drone.amp_(dronearray[0].amp, dur); // and fade in
							drone.ratio_(ratio+transp, dur);
						});
					}, {
						dronearray[i].ratio_(ratio+transp, dur);
					});
				});				
			});
		});
		
		// check if the agent is a chord
		// if a chord, then move to the new tuningratios
		// if there are more or fewer notes, then add or delete
		
	}
	
	// in many of these I need to calculate from the offset
	
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
	
	ratio_ { |ratio, dur, harmonic=false| 
		var offsetratio;
		chordratios.do({ |pureratio, i|
			offsetratio = pureratio+ratio-1;
			if(harmonic, {offsetratio = (offsetratio-0.2).nearestInList(scaledegrees) });
			dronearray[i].ratio_(offsetratio, dur);
		});
	} // works
	
	relRatio_ { |change, dur, harmonic=false| 
		dronearray.do({arg drone; drone.ratio_(change+drone.ratio, dur, harmonic) }) 
	} // works
	
	note_ { |note, dur, harmonic=false| 
		var offsetratio, ratio;
			[\hubkey, hub.key].postln;
			[\note, note].postln;
			ratio = (\A:1, \As:2, \Bb:2, \B:3, \C:4, \Cs:5, \Db:5, \D:6, \Ds:7, \Eb:7, \E:8, \F:9, \Fs:10, \Gb:10, \G:11, \Gs:12, \Ab:12).at(note.asSymbol);
			[\ratio, ratio].postln;
			ratio = (ratio - (\A:0, \As:1, \Bb:1, \B:2, \C:3, \Cs:4, \Db:4, \D:5, \Ds:6, \Eb:6, \E:7, \F:8, \Fs:9, \Gb:9, \G:10, \Gs:11, \Ab:11).at(hub.key.asSymbol)).mod(12);
			if(ratio == 0, {ratio = 12});
			[\ratio, ratio].postln;
		
		chordratios.do({ |pureratio, i|
			offsetratio = pureratio+ratio-1;
			if(harmonic, {offsetratio = (offsetratio-0.2).nearestInList(scaledegrees) });
			dronearray[i].ratio_(offsetratio, dur);
		});
	} 
		
	degree_ { |argdegree, dur, harmonic=false| // harmonic is the argument for staying in scale (for chord progressions) 
		var offsetratio, ratio, degree;
		degree = argdegree.max(1).mod(scaledegrees.size);
		ratio = scaledegrees[degree]; // get the tuning ratio for that degree (note)
		chordratios.do({ |pureratio, i|
			offsetratio = pureratio+ratio-1;
			if(harmonic, {offsetratio = (offsetratio-0.2).nearestInList(scaledegrees) });
			dronearray[i].ratio_(offsetratio, dur);
		});
	} 
	relDegree_ { |change, dur, harmonic=false| dronearray.do({arg drone; drone.degree_(change+drone.degree, dur, harmonic) }) } // works

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
	tuning_ { |argtuning, dur| tuning = argtuning; dronearray.do({arg drone; drone.tuning_(tuning, dur) }) } // working
	scale_ { |scale| 
		var scala, scl, semitones, octaveRatio, scalesize;
		scala = false; // by default expecting an SC scale, not Scala (Fokker) - see DroneScales class
		if(scale.isArray, {
			semitones = scale;
		}, {
			scl = Scale.newFromKey(scale.asSymbol); // this will post a warning if it's a Scala scale
			octaveRatio = scl.tuning.octaveRatio; // this is for non-octave repeating scales such as Bohlen Pierce 3:1 'tritave' scale
			if(scl.isNil, { // support of the Scala scales
				scala = true;
				scl = DroneScale.new(scale); 
				"This is a Scala scale".postln;
				this.tuning_(scale); // the Scala scales are also tunings
			}); 
			semitones = scl.semitones;
		});
		scalesize = semitones.size; // needed from DroneMachines
		scaledegrees = Array.fill(5, {|i| semitones+(i*12)+1 }).flatten; // add 1 because of indexing from 1
		scaledegrees = scaledegrees.insert(0, 0); // put a zero at the beginning so I can index from 1
		
		dronearray.do({arg drone; drone.scale_(scale) });
	}
	chord_ { |chord, dur, transp=0| // transposition in ratios (half-notes if et12)
		this.changeChord(chord, dur, transp);
		dronearray.do({arg drone; drone.chord_(chord) }); // the chord of a drone
	}  
	type_ { |type| dronearray.do({arg drone; drone.type_(type) }) }
	set { | ...args | dronearray.do({arg drone; drone.set(*args) }) }
	freeSynths { |releasetime| dronearray.do({arg drone; drone.freeSynths(releasetime) }) } // the chord of a drone 
	kill { |releasetime| hub.drones.killChord(name); dronearray.do({arg drone; drone.kill(releasetime) }) }
	auto_ { |bool| dronearray.do({arg drone; drone.auto_(bool) }) }
	clearauto { dronearray.do({arg drone; drone.clearauto() }) }
	recParam {| method, min, max, round=0 | hub.drones.rec_(dronearray, method, min, max, round) }
	setParam {| method, min, max, round=0 | hub.drones.setParam_(dronearray, method, min, max, round) }
	stopParam { | method | dronearray.do({arg drone; drone.stopParam(method) }) }

	// Chord specific MIDI interface listener
	addMIDI { | transp=0, dur, harmonic=false | // this chord will listen to MIDI messages
		if(hub.midi == false, { MIDIIn.connectAll });
		MIDIdef.noteOn(this.name, {arg ...args;
			{
				this.ratio_(args[1]-11, dur, harmonic);
				this.amp_(args[0]/127, dur);
			}.defer;
		});
	}
	
	removeMIDI {
		MIDIdef(this.name).free;
	}

	perform { arg command;
		if(command.contains("changeChord") || command.contains("type"), { // methods of this class (not its drones)
			var combinedstring; // = ("this"++command);
			combinedstring = ("~drones.chordDict[\\"++name++"]"++command); // accessing this through the dict (since this. doesn't work)
			combinedstring.interpret;
		}, {
			dronearray.do({arg drone;
				var combinedstring, interprstr;
				combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis chord exists, but there is a syntax error".postln };
			});
		});
	}
}
