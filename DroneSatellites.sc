
DroneSatellites {

	var <>dronearray;
	var <selected;
	var hub, name;
	var scale, chord, tonality, transposition, spread;
	var number, tonic, rat; 
	
	*new { | hub, name |
		^super.new.initSatellites( hub, name );
	}

	initSatellites { | arghub, argname |
		hub = arghub;
		name = argname;
	}
	
	// ratios are scale ratios, i.e., 
	createSatellites { | type=\sine, ratios=#[1,2,3,4,5,6,7,8,9,10,11,12], tonic=1, harmonics, amp=0.2, speed, length, angle, num=20, argspread, env, octave |
		var drone;
		number = num;
		selected = false;
		spread = argspread;
		if(angle == 0, { angle = nil }); // allow for the angle slider to set random angles (0=rand)
		dronearray = [];
		if(ratios.isArray, { // it's a custom scale
			scale = ratios;
		}, {  // it's a scale from the Scale class
			scale = Scale.newFromKey(ratios).degrees+1; // indexing by 1
		});		
		rat = [];
		spread.do({|i| rat = rat.add(scale*(i+1)) });
		rat = rat.flatten;
		num.do({arg i;
			var tonicArg, angleArg, lengthArg, speedArg, harmonicsArg, ratioArg;
			angleArg = angle.value ? 360.rand;
			speedArg = speed.value ? 30.0.rand2; // speed.value allow for a function to be passed as arguments
			lengthArg = length.value ? (4+(4.0.rand));
			tonicArg = tonic.value ? (2+(14.rand));
			harmonicsArg = harmonics.value ? (1+(4.rand));
			drone = hub.drones.createDrone( type, tonicArg, harmonicsArg, amp, speedArg, lengthArg, angleArg, 1, rat.choose, name++"_"++(1+i), env, octave, post:false );
			dronearray = dronearray.add(drone);
		});			
	}

	num_ { |num|
		var diff, drone;
		diff = num - number;
		number = num;
		if(diff > 0, {
			diff.do({ |i|
				var tonicArg, angleArg, lengthArg, speedArg, harmonicsArg, ratioArg, env, octave, amp, type;
				angleArg = 360.rand;
				speedArg = 30.0.rand2;
				lengthArg = (4+(4.0.rand));
				tonicArg = dronearray.choose.tonic ? (2+(14.rand));
				harmonicsArg = dronearray.choose.harmonics;
				env = dronearray.choose.env;
				amp = dronearray.choose.amp;
				octave = dronearray.choose.octave;
				type = dronearray.choose.type;
				drone = hub.drones.createDrone( type, tonicArg, harmonicsArg, amp, speedArg, lengthArg, angleArg, 1, rat.choose, name++"_"++(1+i+diff), env, octave, post:false );
				
				dronearray = dronearray.add(drone);
			});
		}, {
			dronearray[num..].do({ |drone| drone.kill});
			dronearray = dronearray[0..num-1];
		});
		
	}
	
	spread_ { |argspread|
		var rat;
		rat = [];
		spread = argspread;
		spread.do({|i| rat = rat.add(scale*(i+1)) });
		rat = rat.flatten;
		dronearray.do({ |drone| drone.ratio_(rat.choose) });
	}
	
	// satellites can be either in chords or scales
	changeScale { | argscale, dur, transp=0 |
		var thisratios, rat;
		tonality = argscale;
		transposition = transp;
		if(argscale.isArray, { // its a custom scale
			scale = argscale;
		}, {  // it's a scale from the dict
			scale = Scale.newFromKey(argscale).degrees+1; // indexing by 1
		});		
		rat = [];
		spread.do({|i| rat = rat.add(scale*(i+1)) });
		rat = rat.flatten;
		dronearray.do({ | drone | drone.ratio_( rat.choose+transp, dur ) });
	}

	// satellites can be either in chords or scales
	changeChord { | argchord, dur=nil, transp=0  |
		var thisratios, rat;
		tonality = argchord;
		transposition = transp;
		if(argchord.isArray, { // its a custom scale
			chord = argchord;
		}, {  // it's a chord from the dict
			chord = hub.getChordDict[argchord]+1; // indexing by 1
		});		
		rat = [];
		spread.do({|i| rat = rat.add(chord*(i+1)) });
		rat = rat.flatten;
		dronearray.do({ | drone | drone.ratio_( rat.choose+transp, dur ) });
	}
	
	tonality {
		^("Satellite tonality is : " ++ tonality);
	}

	selected_ { | bool |
		selected = bool;
		dronearray.do({ |drone| drone.selected = bool });
	}

	freeSynths { | releasetime |
		dronearray.do({arg drone; drone.freeSynths( releasetime ) });
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
	tuning_ { |tuning, dur | dronearray.do({arg drone; drone.tuning_(tuning, dur) }) } // working
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
	kill { |releasetime| hub.drones.killSatellite(name); dronearray.do({arg drone; drone.kill(releasetime) }) }
	auto_ { |bool| dronearray.do({arg drone; drone.auto_(bool) }) }
	clearauto { dronearray.do({arg drone; drone.clearauto() }) }
	recParam {| method, min, max, round=0 | hub.drones.rec_(dronearray, method, min, max, round) }
	setParam {| method, min, max, round=0 | hub.drones.setParam_(dronearray, method, min, max, round) }
	stopParam { | method | dronearray.do({arg drone; drone.stopParam(method) }) }

	perform { arg command;
		if(command.contains("changeScale") || command.contains("changeChord") || command.contains("type"), {
			var combinedstring; // = ("this"++command);
			combinedstring = ("~drones.satellitesDict[\\"++name++"]"++command); // accessing this through the dict (since this. doesn't work)
			combinedstring.interpret;
			// hub.states.addToScore_(combinedstring); // the below will do this
		}, {
			dronearray.do({arg drone;
				var combinedstring, interprstr;
				combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis satellite exists, but there is a syntax error".postln };
			});
		});
	}	
}
