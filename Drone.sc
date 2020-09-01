
Drone {
	classvar oscresp, atomNetAddr, atomcount = -1;
	var thisatomnumber = 0;

	var <>dying; // experimental
	var window, hub, <>point; 
	var strokeColor, fillColor, updateFunc, <rotation;
	var innersize, outersize, resonsize, <speed, <length;
	var synth, synthgroup, synths, <synthParams;
	var fundamental, nrChannels;
	var <selected = false;
	var >oppositemove = false;
	var showResonance, scala;
	var <name, <type, <tonic, <harmonics, <freq, <angle, <amp, <resonance, env, <octave, <ampMult, <note;
	var <ratios, <tuning, <ratio, <scaledegrees, <scale, <degree, <scalesize, <tuningsize, <chord, octaveRatio, scaleTonic;
	var <>autoTaskDict;
	var buffer;
	
	*new { arg hub, tuning, scale, fundamental; 
		^super.new.initDrone(hub, tuning, scale, fundamental);
	}
	
	initDrone { | arghub, argtuning, argscale, argfundamental |
		hub = arghub;
		window = hub.window;
		tuning = argtuning;
		scale = argscale;
		fundamental = argfundamental;
		nrChannels = hub.speakers.nrChannels;
		synthgroup = Group.new;
		synths = Array.fill(nrChannels, { nil });
		resonance = 1;
		autoTaskDict = ();
		showResonance = false; // the strip denoting where the resonance is
		scala = false;
		dying = false; // experimental
		freq = 55; // temp
	}

	createDrone { | argtype=\saw, argtonic=1, argharmonics=2, argamp=0.2, argspeed=100, arglength=6, argangle=0, argdegree=1, argratio=1, argenv, argoctave, argnote | 
		var step = 0.05/argenv[0];
		if(argenv[0] < 0.5, {
			ampMult = 1; // no need to make the colour appear slowly
		}, {
			ampMult = 0;
			Task({((argenv[0]/0.05).round).do({ ampMult = ampMult + step; 0.05.wait; })}).start; // slow appearance of drone colour
		});
		\deb0.postln;
		type = argtype;
		tonic = argtonic.max(1);
		harmonics = argharmonics.max(1);
		degree = argdegree.max(1); // fundamental of the scale - 1 is the lowest
		ratio = argratio.max(1); // fundamental of the scale - 1 is the lowest
		amp = argamp;
		env = argenv ? [3,3];
		octave = argoctave ? 1;
		point = Point(hub.middle, hub.middle);
		synthParams = this.initSynthParams(tonic, freq, harmonics, amp, degree, octave, env); 
		
		this.scale_(scale); // setting the scaledegrees
		this.tuning_(tuning);
		this.chord_(\minor); // minor is the default chord
		note = argnote;
		
		if(degree != 1, {
			this.degree_(degree);
			//ratio = scaledegrees.indexOf((ratio-0.2).nearestInList(scaledegrees)); // if no degree has been passed, I force the degree to be the nearest in scale
		});

		\deb1.postln;
		
		if(note.isNil.not, {
			[\hubkey, hub.key].postln;
			[\note, note].postln;
			ratio = (\A:1, \As:2, \Bb:2, \B:3, \C:4, \Cs:5, \Db:5, \D:6, \Ds:7, \Eb:7, \E:8, \F:9, \Fs:10, \Gb:10, \G:11, \Gs:12, \Ab:12).at(note.asSymbol);
			[\ratio, ratio].postln;
			ratio = (ratio - (\A:0, \As:1, \Bb:1, \B:2, \C:3, \Cs:4, \Db:4, \D:5, \Ds:6, \Eb:6, \E:7, \F:8, \Fs:9, \Gb:9, \G:10, \Gs:11, \Ab:11).at(hub.key.asSymbol)).mod(12);
			if(ratio == 0, {ratio = 12});
			[\ratio, ratio].postln;
		});
			
		if(ratio != 1, {
			// degree takes precedence over ratio in the Threnoscope (if user is passing ratio>1 AND degree>1)
			degree = degree ? scaledegrees.indexOf((ratio-0.2).nearestInList(scaledegrees)); // if no degree has been passed, I force the degree to be the nearest in scale
		});

		freq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added

		this.setDroneLook();

		speed = argspeed/1000;
		length = arglength.max(6) / (360 / (2*pi) ); // arglength
		rotation =  (argangle / (360 / (2*pi) )) - length;
		rotation = (rotation + speed)%(2*pi); // added later
		strokeColor = if( hub.threnoscopeColor == Color.white, { Color.black }, { Color.white });

		// testing whether colours are interesting as indication of type (when fixing this, do the same in the .type_ menthod)
		switch(type)
			{\saw}  		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.2), rrand(0.1, 0.2) ); this.startSynth() }
			{\sine}		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.2), rrand(0.1, 0.2) ); this.startSynth() }
			{\tri}		{ fillColor = Color.new( rrand(0.1, 0.2), rrand(0.1, 0.7), rrand(0.1, 0.2) ); this.startSynth() }
			{\cub}		{ fillColor = Color.new( rrand(0.1, 0.2), rrand(0.1, 0.2), rrand(0.1, 0.7) ); this.startSynth() }
			{\pulse}		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.2), rrand(0.1, 0.7) ); this.startSynth() }
			{\formant}	{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.7), rrand(0.1, 0.2) ); this.startSynth() }
			{\eliane} 	{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.7), rrand(0.1, 0.2) ); this.startSynth() }
			{\noise} 		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.7), rrand(0.1, 0.2) ); this.startSynth() }
			{\klank} 		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.7), rrand(0.1, 0.2) ); this.startSynth() }
			{\gendy} 		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.7), rrand(0.1, 0.2) ); this.startSynth() }
			{\atom}		{ this.initAtom; {this.startSynth()}.defer; }
			{
				buffer = Buffer.read(Server.default, DroneSynths.bufferPath(type, freq), action:{ this.startSynth() }) ; // samples
			};
		
		fillColor = Color.rand(0.1, 0.7);
	
		\deb2.postln;

	}
	
	
	initAtom { // an experimental method for Chris Kiefer's Atom software.
		var message;
		atomcount = atomcount + 1;
		thisatomnumber = atomcount;
		[\atomcount, atomcount].postln;
		
 		if(atomNetAddr.isNil, { atomNetAddr = NetAddr("127.0.0.1", 8111) });
 		
 		Synth(\atom, [ \atomnumber, thisatomnumber, \freq, freq, \amp, amp, \harmonics, harmonics], synthgroup); 
		
		if(oscresp.isNil, { // only create one responder
			oscresp = OSCFunc({ |msg| 
				var message, data;
				"ATOM SYNTH : ".post; msg.postln;
				
				switch(msg[4].asInteger)
				{0}{ message = '/freq' }
				{1}{ message = '/amp' }
				{2}{ message = '/harmonics' }
				{3}{ message = '/probability' }
				{4}{ message = '/rate' }
				{5}{ message = '/pitch' }
				{6}{ message = '/loopStart' }
				{7}{ message = '/loopEnd' }
				{8}{ message = '/snapFreqs'; msg = msg[..4] ++ msg[5..].select(_>0).flatten; }
				{9}{ message = '/snapPull' };
				 
				[\message, message].postln;
				
				msg.postln;
				data = if(msg[4].asInteger == 8, {msg[5..]}, { msg[5].asFloat });
				[\data, data].postln;
				atomNetAddr.sendMsg( message, msg[3].asInteger, *data); 
			}, '/atom');
		});
	}

	/*
		~drones.createDrone(\atom, 4, name:\ooo)
		~ooo.harmonics = 3+(0.1.rand)
		~ooo.amp = 0.3+(0.1.rand)
		~ooo.freq = 354+(0.1.rand)
		~ooo.set(\probability, 0.36+(0.1.rand) ) 
		~ooo.set(\rate, 1+(0.1.rand) ) 
		~ooo.set(\pitch, 0.35+(0.1.rand) ) 
		~ooo.set(\loopStart, 0.3+(0.1.rand) ) 
		~ooo.set(\loopEnd, 0.65+(0.1.rand) ) 
		~drones.killAll
		//	thisatomnumber goes up to 8 and stand for the book.
	*/
		
	sendAtomChannel { |out, bool|
		atomNetAddr.sendMsg( '/out', thisatomnumber, out, bool); // message, which atom, channel, on or off
	}
	
	
	setDroneLook {  
		var newouter;
		
		innersize = (freq.cpsmidi * (hub.middle/90)) - 80;
		outersize = (freq + ((harmonics-1)*fundamental)).cpsmidi * (hub.middle/90) - 80;
		resonsize = (freq + ((resonance-1)*fundamental)).cpsmidi * (hub.middle/90) - 80;
//	\dronelook_deb.postln;
		// outersize = (fundamental * ((tonic * octaveRatio.pow(octave-1) * ratios[ratio]) + (harmonics-1))).cpsmidi * (hub.middle/90) - 80; // old method - don't delete
		// resonsize = (fundamental * ((tonic * octaveRatio.pow(octave-1) *  ratios[ratio]) + (resonance-1)) ).cpsmidi * (hub.middle/90) - 80;
	
		if(type == \sine, { // the sine has no harmonics
			innersize = innersize - 1;
			outersize = innersize + 1;
		});
		synthParams[\freq] = freq;
	}
	
	getDroneLook { // called from DroneMachine for drawing
		^[innersize, outersize, rotation, length];	
	}
		
	name_ { | argname |
		name = argname.asSymbol;	
		synthParams[\name] = name;
	}		
	
	strokeColor_ { | argcolor |
		strokeColor = argcolor;
	}

	fillColor_ { | argcolor |
		fillColor = argcolor;
	}
	
	env_ { | envt |  // supporting passing in of both array [0, 1] (attack and release) and just an integer (for both A&R)
		if(envt.isArray, { env = envt }, {env = [envt, envt]});
		synthParams[\env] = env;
		^("Drone : "++ name ++ " -> env set to : " ++ env);
	}
	
	env {
		^env;
	}

	// the freq_ method is the key method that controls the changes of a drone (all other methods update their data and call this)
	freq_ { | argfreq, dur | // time-morph method - this method basically uses the tonic to set the freq
		var oldfreq, step;
		var recursiveMaxHarm;
		recursiveMaxHarm = {arg harm, xfreq; // filter blows up above nyquist
			if( (xfreq * harm) > ((Server.default.sampleRate/2)-2000) , {
				recursiveMaxHarm.(harm-1, xfreq);
			}, {
				this.harmonics_(harm);
			})
		};
		
		recursiveMaxHarm.(this.harmonics, argfreq.max(27.5));

		oldfreq = freq;
		if(dur.isNil || (dur == 0), {
			freq = argfreq.max(27.5);
//			tonic = argfreq/fundamental;
//			freq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added
//			freq = argfreq;
			this.setDroneLook();
			synthgroup.set(\freq, freq);
			synthParams[\freq] = freq;
		}, {
			step = (argfreq.max(27.5) - oldfreq) / (dur/0.05).round;
			if(autoTaskDict.freq.isNil && (argfreq.max(27.5) != oldfreq), {
				autoTaskDict.add(\freq -> 
					Task({((dur/0.05).round).do({
						{
							freq = freq + step;
							this.freq_(freq); // recursive calling but without the dur ! 
						}.defer;
						0.05.wait;
					});
					freq = freq.round(0.00000000001); // get rid of imprecision that might have been introduced
					autoTaskDict.freq = nil;
					}).start;
				);
			});
		});
		^("Drone : "++ name ++ " -> tonic set to : " ++ tonic);
	}
	
	postAuto { // xxx delete this
		autoTaskDict.postln;
	}
	
	relFreq_ { | change=0, dur | // change freq relative to current freq of the drone
		var newfreq;
		newfreq = (freq + change).clip(20, 20000);
		this.freq_( newfreq, dur ); 
	}

	tonic_ { | argtonic, dur | // tonic: can accept 3/2 ratios or 1.5 ->  time-morph method
		var newfreq;
		tonic = argtonic;
		newfreq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added
		this.freq_(newfreq, dur);
		synthParams[\tonic] = argtonic;
		^("Drone : "++ name ++ " -> tonic set to : " ++ tonic);
	}
	
	relTonic_ { | change=0, dur | // change tonic relative to current tonic of the drone
		var newtonic;
		newtonic = (tonic + change).clip(1, 20);
		[\tonic, tonic, \newtonic, newtonic ].postln;
		this.tonic_( newtonic, dur ); 
	}

/*
	cents_ { |argcent=0, dur| 
		var newfreq;

		tonic = argtonic;
		newfreq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added
		this.freq_(newfreq, dur);
		synthParams[\tonic] = argtonic;
	}
	*/

	ratio_ { | argratio, dur | // time-morph method - this method basically uses the ratio to set the freq
		var newfreq; 
		ratio = argratio.max(1).mod(ratios.size); // wrap at the size of the ratios (interface)
		newfreq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added
		degree = scaledegrees.indexOf((ratio-0.2).nearestInList(scaledegrees)); // if no degree has been passed, I force the degree to be the nearest in scale
		this.freq_(newfreq, dur);
		synthParams[\ratio] = argratio;		
		^("Drone : "++ name ++ " -> ratio set to : " ++ ratio);
	}

	relRatio_ { | change=0, dur | // change freq relative to current freq of the drone
		var newratio;
		newratio = ratio + change;
		this.ratio_( newratio, dur ); 
	}

	degree_ { | argdegree, dur | // time-morph method - this method basically uses the tonic to set the freq
		var newfreq;
		if(scala, {
			degree = argdegree.max(1);
			this.ratio_(argdegree, dur); // no scale degrees in Scala scales (Tuning and Scale uniform)
		}, {
			degree = argdegree.max(1).mod(scaledegrees.size);
			ratio = scaledegrees[degree]; // get the tuning ratio for that degree (note)
			newfreq = (fundamental * tonic* octaveRatio.pow(octave-1) * ratios[ratio]).max(fundamental);
			this.freq_(newfreq, dur);
		});
		synthParams[\degree] = argdegree;		
		^("Drone : "++ name ++ " -> degree set to : " ++ argdegree);
	}

	relDegree_ { | change=0, dur | // change freq relative to current freq of the drone
		var newdegree;
		newdegree = degree + change;
		this.degree_( newdegree, dur ); 
	}

	note_ { | note, dur | // time-morph method - this method basically uses the ratio to set the freq
		var newfreq;
		//if(note.isNil.not, {
		[\hubkey, hub.key].postln;
		[\note, note].postln;
		ratio = (\A:1, \As:2, \Bb:2, \B:3, \C:4, \Cs:5, \Db:5, \D:6, \Ds:7, \Eb:7, \E:8, \F:9, \Fs:10, \Gb:10, \G:11, \Gs:12, \Ab:12).at(note.asSymbol);
		[\ratio, ratio].postln;
		ratio = (ratio - (\A:0, \As:1, \Bb:1, \B:2, \C:3, \Cs:4, \Db:4, \D:5, \Ds:6, \Eb:6, \E:7, \F:8, \Fs:9, \Gb:9, \G:10, \Gs:11, \Ab:11).at(hub.key.asSymbol)).mod(12);
		[\ratio, ratio].postln;
	//	});
		ratio = ratio.max(1).mod(ratios.size); // wrap at the size of the ratios (interface)
		newfreq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added
		degree = scaledegrees.indexOf((ratio-0.2).nearestInList(scaledegrees)); // if no degree has been passed, I force the degree to be the nearest in scale
		this.freq_(newfreq, dur);
		synthParams[\ratio] = ratio;		
		^("Drone : "++ name ++ " -> ratio set to : " ++ ratio);
	}

	// XXX - Should a machine represent the automation of these three methods?
	playRatios { | dur=0.25, slide=false |
		fork{
			({|i| i+1}!(tuningsize+1)).mirror.do({ | ratio | {this.ratio_(ratio, if(slide, {dur}, {nil}))}.defer; dur.wait; });
		}
	}

	playDegrees { | dur=0.25, slide=false |
		fork{
			({|i| i+1}!(scalesize+1)).mirror.do({ | ratio | {this.degree_(ratio, if(slide, {dur}, {nil}))}.defer; dur.wait; });
		}
	}

	playScale { | dur=0.25, slide=false |
		this.playDegrees(dur, slide);
	}
	
	octave_ { | argoctave, dur | // tonic: can accept 3/2 ratios or 1.5 ->  time-morph method
		var newfreq;
		octave = argoctave.max(1);
		newfreq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio]; // octave added
		this.freq_(newfreq, dur);
		synthParams[\octave] = argoctave;
		^("Drone : "++ name ++ " -> octave set to : " ++ octave);
	}
	
	relOctave_ { | change=0, dur | // change freq relative to current freq of the drone
		var newoctave;
		newoctave = octave + change;
		this.octave_( newoctave, dur ); 
	}

	transpose_ { | argtuninginterval, dur | // transposition of tuning ratios (recommended if transposing chords)
		this.ratio_( ratio + argtuninginterval, dur );
	}

	interval_ { | arginterval, dur | // transposition of scale intervals 
		degree = degree + arginterval;
		this.degree_( degree, dur );
	}

	harmonics_ { | argharmonics, dur | // time-morph method
		var step, recursiveMaxHarm;
		recursiveMaxHarm = {arg harm; // filter blows up above nyquist
			if( (this.freq*harm) > ((Server.default.sampleRate/2)-2000) , {
				recursiveMaxHarm.(harm-1);
			}, {
				argharmonics = harm;
			})
		};
		if(dur.isNil || (dur == 0), {
			harmonics = recursiveMaxHarm.(argharmonics); // make sure not to blow the filter
			harmonics = argharmonics.max(1);
			this.setDroneLook();
			synthgroup.set(\harmonics, harmonics);
			synthParams[\harmonics] = argharmonics.max(1);
		}, {
			step = (argharmonics.max(1) - harmonics) / (dur/0.05).round;
			[argharmonics, harmonics].postln;
			if(autoTaskDict.harmonics.isNil && (argharmonics.max(1) != harmonics), {
				autoTaskDict.add(\harmonics -> 
					Task({((dur/0.05).round).do({
						{
							harmonics = harmonics + step;
							this.harmonics_(harmonics); // no dur - recursion
						}.defer;
						0.05.wait;
					});
					harmonics = harmonics.round(0.001); // get rid of imprecision that might have been introduced
					autoTaskDict.harmonics = nil;
					}).start;
				);
			});
		});
		^("Drone : "++ name ++ " -> harmonics set to : " ++ harmonics);
	}
	
	resonance_ { | argres, dur=nil |
		var step;
		if(argres.isKindOf(Boolean), { // setting resonance on and off
			if(argres, {
				synthgroup.set(\resamp, 12);
				showResonance = true;
			},{
				synthgroup.set(\resamp, 0);
				showResonance = false;
			})
		}, {
			if(dur.isNil || (dur == 0), {
				resonance = argres;
				this.setDroneLook();
				synthgroup.set(\resonance, resonance);
				synthParams[\resonance] = resonance;
			}, {
				step = (argres - resonance) / (dur/0.05).round;
				if(autoTaskDict.resonance.isNil && (argres != resonance), {
					autoTaskDict.add(\resonance -> 
						Task({((dur/0.05).round).do({
							{
								resonance = resonance + step;
								this.resonance_(resonance);
							}.defer;
							0.05.wait;
						});
						resonance = resonance.round(0.001); // get rid of imprecision that might have been introduced
						autoTaskDict.resonance = nil;
						}).start;
					);
				});
			});
		});
		^("Drone : "++ name ++ " -> resonance set to : " ++ resonance);
	}
	
	amp_ { | argamp, dur |
		var step;
		if(dur.isNil || (dur == 0), {
			amp = argamp;
			synthgroup.set(\amp, amp);
			synthParams[\amp] = amp;
		}, {
			step = (argamp - amp) / (dur/0.05).round;
			if(autoTaskDict.amp.isNil && (argamp != amp), {
				autoTaskDict.add(\amp -> 
					Task({((dur/0.05).round).do({
						{
							amp = amp + step;
							this.amp_(amp);
						}.defer;
						0.05.wait;
					});
					amp = amp.round(0.001); // get rid of imprecision that might have been introduced
					autoTaskDict.amp = nil;
					}).start;
				);
			});
		});
		^("Drone : "++ name ++ " -> amp set to : " ++ argamp);
	}
	
	relAmp_ { | change=0, dur=10 | // change amp relative to current amp the drone
		this.amp_( amp + (amp * change), dur); 
	}

	speed_ { | argspeed |
		speed = argspeed.value/1000; // .value since it's possible to pass a function as a speed arg
		^("Drone : "++ name ++ " -> speed set to : " ++ speed);
	}
	
	relSpeed_ { | change=0 | // change speed relative to current speed of the drone (increase/decrease with the input amount)
		if(speed <= 0, {
			speed = speed - (change/1000);
		},{
			speed = speed + (change/1000);			
		});
	}

	angle_ { | argangle | // don't use this from GUI, use rotation_ (below)
		rotation = (argangle / (360 / (2*pi) ) ) - length;
		"rotation :  ".post; rotation.postln;
		this.freeSynths();
		this.startSynth();
		^("Drone : "++ name ++ " -> angle set to : " ++ argangle);
	}
	
	rotation_ { | argrotation | // use this rather than angle from GUI
		//"setting rotation to: ".post; argrotation.postln;
		rotation = argrotation;
	}	
	
	relRotation_ { | change=0 | // use this rather than angle from GUI
		rotation = (rotation+change)%(2*pi);
		oppositemove = true;
	}	

	length_ { | arglength |
		length = (arglength*((2*pi)/360)).max(0.01).min(2pi);
		this.freeSynths();
		this.startSynth();
		this.synthMachine(rotation);
		^("Drone : "++ name ++ " -> length set to : " ++ length);
	}

	relLength_ { | change=0 | // change length relative to current length of the drone
		length = (length + (change*((2*pi)/360))).max(0.01).min(2pi);
		this.freeSynths();
		this.startSynth();
		this.synthMachine(rotation);
		^("Drone : "++ name ++ " -> length set to : " ++ length);
	}

	selected_ { | argsel |
		selected = argsel;
		if(selected, { // only if newly selected (not deselected)
			// [\name, name, \freq, freq, \tonic, tonic, \harmonics, harmonics, \amp, amp].postln;
		});
		^("Drone : "++ name ++ " -> selected set to : " ++ true);
	}

	tuning_ { | argtuning, dur |
		var temptuningratios, tuningratios, localfreq;
		tuning = argtuning;
		[\NEWTUNING, tuning].postln;
		
		if(tuning.isArray, {
			if(tuning[0] == 0, { // the tuning array is in cents (cents start with 0)
				tuningratios = (tuning/100).midiratio;
			}, {	// the array is in rational numbers (or floating point ratios)
				tuningratios = tuning;
			});
			[\TUNING_IS_ARRAY, tuningratios].postln;
		
		}, {
			[\TUNING_IS_NOT_ARRAY, tuningratios].postln;
			temptuningratios = Tuning.newFromKey(argtuning.asSymbol); 
			[\temptuningratios, temptuningratios].postln;
			if(temptuningratios.isNil, { "IN HERE \n ++ \n".postln;temptuningratios = DroneScale.new(argtuning) }); // support of the Scala scales / tunings
						[\temptuningratios22, temptuningratios].postln;
			if(temptuningratios.isKindOf(Scale), {
				tuningratios = temptuningratios.ratios;
				octaveRatio = temptuningratios.octaveRatio;
			}, {
				tuningratios = temptuningratios.ratios;
			});
						[\temptuningratios33, temptuningratios].postln;
		});
		[\____tuningratios, tuningratios].postln;
		[\____octaveRatio, octaveRatio].postln;
		tuningsize = tuningratios.size; // needed to know how many ratios there are in the tuning system
		ratios = Array.fill(10, {|i| tuningratios*octaveRatio.pow(i) }).flatten;
		ratios = ratios.insert(0, 0); // put a zero at the beginning so I can index from 1 (and get at it as well)

		localfreq = fundamental * tonic * octaveRatio.pow(octave-1) * ratios[ratio];
		this.freq_(localfreq, dur); // freq is set in .freq_ 
		^("Drone : "++ name ++ " -> tuning set to : " ++ tuning);
	}

	scale_ { | argscale |
		var scl, semitones;
		scala = false; // by default expecting an SC scale, not Scala (Fokker) - see DroneScales class
		scale = argscale; // set the variable for the Drone instance (don't delete)
		\deb00.postln;
		if(scale.isArray, {
			semitones = scale;
			
		}, {
					\deb000.postln;

			scl = Scale.newFromKey(scale.asSymbol); // this will post a warning if it's a Scala scale
					\deb111.postln;

			if(scl.isNil, { // support of the Scala scales
									\deb222.postln;

				scala = true;
				scl = DroneScale.new(scale); 
					\deb333.postln;
				"This is a Scala scale".postln;
				[\scl, scl].postln;
				this.tuning_(scale); // the Scala scales are also tunings
				\deb444.postln;

			}); 
			octaveRatio = scl.tuning.octaveRatio; // this is for non-octave repeating scales such as Bohlen Pierce 3:1 'tritave' scale
			semitones = scl.semitones;
		});
		\deb11.postln;
		scalesize = semitones.size; // needed from DroneMachines
		scaledegrees = Array.fill(5, {|i| semitones+(i*12)+1 }).flatten; // add 1 because of indexing from 1
		scaledegrees = scaledegrees.insert(0, 0); // put a zero at the beginning so I can index from 1
		^("Drone : "++ name ++ " -> scale set to : " ++ scale);
	}

	chord_ { | argchord |
		if(argchord.isArray, {
			chord = argchord;
		},{
			chord = hub.drones.getChordDict.at(argchord);
		});
	}

	chordnote_ { | index, dur=nil |
		this.ratio_(chord.wrapAt(index-1)+1, dur); // add 1 as the nil is the first item of scale and tuning (indexing from 1)
	}
	
	type_ { | argtype = \saw, dur=0 |
		type = argtype;
		this.freeSynths(0);
		// testing whether colours are interesting as indication of type
//		switch(type)
//			{\saw}  		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.2), rrand(0.1, 0.2) ) }
//			{\sine}		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.2), rrand(0.1, 0.2) ) }
//			{\tri}		{ fillColor = Color.new( rrand(0.1, 0.2), rrand(0.1, 0.7), rrand(0.1, 0.2) ) }
//			{\cub}		{ fillColor = Color.new( rrand(0.1, 0.2), rrand(0.1, 0.2), rrand(0.1, 0.7) ) }
//			{\pulse}		{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.2), rrand(0.1, 0.7) ) }
//			{\formant}	{ fillColor = Color.new( rrand(0.1, 0.7), rrand(0.1, 0.7), rrand(0.1, 0.2) ) }
//			{\atom}		{ this.initAtom; { this.startSynth()}.defer; }
//			{
//				buffer = Buffer.read(Server.default, DroneSynths.bufferPath(type, freq), action:{ this.startSynth() }) ; // samples
//			};
		this.setDroneLook();
		this.startSynth();
		synthParams[\type] = type;
		^("Drone : "++ name ++ " -> type set to : " ++ type);
	}
	
	set { | ...args | // this method is for all commands to the synth that do NOT change the graphic drone on the GUI
		if(args.indexOf(\amp).isNil.not, { amp = args[args.indexOf(\amp)+1] }); // set the amplitude
		synthParams.putAll(args);
		synthgroup.set(*args);
		^("Drone : "++ name ++ " -> args set to : " ++ args);
	}
	
	// used for deathArray in DroneController for slow visual fadeout
	killSynths { | releasetime=0 |
		var step;
		var tempsynthgroup = synthgroup;
		
		if(releasetime > 0.5, {
			step = 0.05/releasetime;
			// fork works, but not Task (when drones are fading out in scores) - DON'T mess with this! !!! 
//	 		Task({ ((releasetime/0.05).round).do({ ampMult = ampMult - (step*1.5); "FADING OUT".postln; 0.05.wait; })}).start(TempoClock.new); // slow disappearance of drone colour
	 		{ ((releasetime/0.05).round).do({ ampMult = ampMult - (step*1.5); 0.05.wait; }); this.killDrone }.fork(TempoClock.new); // slow disappearance of drone colour
		});
		synths = synths.collect({arg synth; if(synth.isNil, {666}, {synth}) }); // remove nil from array (else drone might cross line & create a new synth)
		"killing synths for ___ ".post; this.name.postln;
		"synths : ".post; synths.postln;
		
		synthgroup.set(\doneAction, 14); // change doneAction to 14 so the synths will free the group as well
		if(synths.occurrencesOf(666) == synths.size, { synthgroup.free }); // if there is no synth playing, we still need to free the group
		synthgroup.release(releasetime); // free the synths in the group (and with doneAction 14, they'll kill the group)
		
		//synths.do({arg synth; synth.release(releasetime)});
		//{tempsynthgroup.free}.defer(releasetime+0.5); // remove the group itself from the server
		//synthgroup = Group.new; // create a new for next synths
	}

	// used in this class
	freeSynths { | releasetime=0 |
		//var tempsynthgroup = synthgroup;
		"freeing synths for ___ ".post; this.name.postln;
		"synths : ".post; synths.postln;
//	synthgroup.set(\doneAction, 14); // change doneAction to 14 so the synths will free the group as well
		synthgroup.release(releasetime); // free the synths in the group
		//synths.do({arg synth; synth.release(releasetime)});
		//{tempsynthgroup.free}.defer(releasetime+0.5); // remove the group itself from the server
//	synthgroup = Group.new; // create a new for next synths
		synths = Array.fill(nrChannels, { nil }); // remove references to synths from synths array
	}

	// this method is used when a drone is killed, as in ~name.kill 
	// it then reports to the controller which does some array sorting, and kills the drone (killDrone method below)
	kill { | releasetime | // from User -> DroneInterpreter // NOTE - don't put 0 as default arg - with nil the synth uses its envelope
		"Killing drone (Drone Class): ".post; this.name.postln;
		//this.killDrone();
		if(atomcount > -1, { oscresp.remove });
		hub.drones.killDrone(this.name, releasetime);
	}
	
	// this method is private for the DroneController class - not used by user
	killDrone { | releasetime |
		// free all tasks, synths and MIDI
		autoTaskDict.do({ | task | task.stop; "killing autoTask".postln; [\task, task].postln; }); // kill automation
		this.freeSynths( releasetime );	
		this.removeMIDI(); // in case there is a MIDI listener on the drone
	}
	
	// ---------------------------------------> automation code  ---------------------------------------------
	
	// need to create handlers/methods for all the params OF THE SYNTH DEFS I want to control 
	// these are setters for what is else this.set above and stored in the synthParams dict
	// tonic_, harmonics_, etc. are already supported, but synth params that are not exposed as methods have to be exposed

	oscamp_ { | oscamp | // XXX - will need to revisit when I've made the final synthdefs
		//synthParams.putAll(\oscamp, oscamp); // maybe no need to add moving target into an array?
		synthgroup.set(\oscamp, oscamp);
	}

	oscfreq_ { | oscfreq | // XXX - will need to revisit when I've made the final synthdefs
		synthgroup.set(\oscfreq, oscfreq);
	}

	recParam {| method, min, max, round=0 |
		var tempmethod = method.asSymbol;
		if(autoTaskDict.at(tempmethod).isNil.not, { autoTaskDict.at(tempmethod).stop }); // stop the task if it's there
		hub.drones.rec_(this, method, min, max, round); // on next mousedown the recording will start
	}

	setParam {| method, min, max, round=0 | // GUI update of parameters - no automation
		hub.drones.setParam_(this, method, min, max, round); 
	}

	stopParam { | method |
		if(method.isNil, { "You need to specify which parameter you want to stop".postln });
		autoTaskDict.at( method.asSymbol ).stop;
		autoTaskDict.removeAt( method.asSymbol );
	}

	startAuto { | method, movementarray | // method called only from DroneController
		var tempmethod = method.asSymbol;
		method = (method++"_").asSymbol;
		autoTaskDict.add(tempmethod -> 
			Task({
				inf.do({ |i|
					{ this.perform(method, movementarray.wrapAt(i)[1]) }.defer;
					movementarray.wrapAt(i)[0].wait;	
				});
			}).play(TempoClock.new);
		);
	}

	auto_ { | bool | // pause and resume task
		if( bool , {
			autoTaskDict.do({ | task | task.resume });
		}, {
			autoTaskDict.do({ | task | task.pause });
		});
	}

	clearAuto {
		autoTaskDict.do({ | task | task.stop });
		autoTaskDict = ();
	}

	// MIDI file playback (called from DroneStates)
	startMIDI { | name, movementarray | // method called only from DroneController (start and stop is there)
		autoTaskDict.add(name -> 
			Task({
				inf.do({ |i|
					var cps;
			//		if(movementarray.wrapAt(i)[1] == \on, {
						cps = movementarray.wrapAt(i)[2].midicps.nearestInList(fundamental*ratios); // for the diverse tunings
					//	cps = movementarray.wrapAt(i)[2].midicps;
					//	[\cps___, cps].postln;
						{ 
			//				this.amp_(movementarray.wrapAt(i)[3]);
							this.freq_( cps ); 
						}.defer;
			//		},{
			//			{ this.amp_(movementarray.wrapAt(i)[3]) }.defer;
			//		});
					movementarray.wrapAt(i)[0].wait;	
				});
			}).play(TempoClock.new);
		);
	}

	// Drone specific MIDI interface listener
	addMIDI { | transp=0, dur | // this drone will listen to MIDI messages
		if(hub.midi == false, { MIDIClient.init(2, 2); MIDIIn.connectAll; hub.midi = true });
		MIDIdef.noteOn(this.name, {arg ...args;
			var aratio;
			var midinote = args[1];
			octave = (midinote-9).div(12) + 1;
			aratio = (midinote-9).mod(12) + 1;
			[\octave, octave, \ratio, ratio].postln;
			{
				this.setFreqFromRatioOct_(aratio, dur);
				this.amp_(args[0]/127, dur);
			}.defer;		
		});		
	}
	
	removeMIDI {
		MIDIdef(this.name).free;
		MIDIdef("control"++this.name).free;
	}
	
	exposeMIDI { | synthargs |
		var change;
		var offset = {nil} ! 8 ;
		var offsetFlag = {false}!8;
		var defname = "control" ++ this.name;
		// set the AKAI knobs
		MIDIdef.cc(defname.asSymbol, { arg val, num, chan, src;
			"control".postln;
			[val, num, chan, src].postln;
			if(offsetFlag[num] == false, { offset[num] = val; offsetFlag[num] = true; });
			if(selected, {
				// num is the parameter (the knob)
				// val is the value
				
				if(hub.padDown, { // change value offset (reset knob)
					offset[num] = val;
					[\offset, offset[num]].postln;
				}, {
					switch(num)
						{1}{ // freq
							"val is: ".post; val.postln;
							if(val > offset[num], {change = 1},{change = -1});
							this.freq = this.freq + change;
							offset[num] = val;
							
	//						if(synthargs[num].isNil, {
	//							this.freq = this.freq + (val-offset);
	//						},{
	//							this.set(synthargs[num], 33); // need linlin here?
	//						});
							}
						{2}{ // harmonics
							"val is: ".post; val.postln;
							if(val > offset[num], {change = 0.1},{change = -0.1});
							this.harmonics = this.harmonics + change;
							offset[num] = val;
	
							}
						{3}{ // amp
							"val is: ".post; val.postln;
							if(val > offset[num], {change = 0.01},{change = -0.01});
							this.amp = this.amp + change;
							offset[num] = val;
							}
						{4}{
							"val is: ".post; val.postln;
							}
						{5}{ // freq high res
							"val is: ".post; val.postln;
							if(val > offset[num], {change = 0.1},{change = -0.1});
							this.freq = this.freq + change;
							offset[num] = val;
							}
						{6}{
							}
						{7}{
							"val is: ".post; val.postln;
							}
						{8}{
							"val is: ".post; val.postln;
							};	
				});
			});
		});		
		
	}

	addFunc { | name, func, time=1 | // if no time arg passed, then defaults to a sec
		autoTaskDict.add(name -> 
			Task({
				inf.do({ 
					{func.value(this)}.defer; 
					time.max(0.1).wait; 
				});
			}).play(TempoClock.new);
		);
	} // addFunc can be paused and restarted with auto_
	
	removeFunc { | name |
		this.stopParam(name);
	}

	interpret { |func|
		{ func.value(this) }.defer; 
	}

//---------------------------------- creating scales (and saving them as Scala files)
	createScale { | numRatios |
		ratios = [0]; // put a zero at the beginning so I can index from 1 (and get at it as well)
		scaleTonic = freq; // the tonic - the reference point of the scale
		^("New scale array is ready to be filled with notes")
	}
	
	addToScale { | index |
		var thisratio;
		thisratio = freq / scaleTonic;
		ratios = ratios.insert(index, thisratio);
	}
		
	saveScale { | name, description="custom ixi scale" |
		var cents, scalafilestring, scalafile;
		this.freq_(scaleTonic);
		// write the scale into the Scala folder;
		cents = ratios.ratiomidi*100.0;
		cents.removeAt(0); // remove the 0 from the scale (the dummy index trick)
		[\cents, cents].postln;
		if(cents[0] == 0, { cents.removeAt(0)}); // there is no 0 cents in scala files (but octave)
		scalafilestring = "! "++name++".scl\n!\n"++description++"\n"++cents.size++"\n!\n";
		cents.do({arg cent;
			cent = cent.asString; 
			if(cent.asString.find(".").isNil, { cent = cent ++ ".0" }); // make sure it's a float
			scalafilestring = scalafilestring ++ cent ++ "\n";
		});
		scalafile = File(hub.pathtouserscalafiles++name++".scl", "w");
		scalafile.write(scalafilestring);
		scalafile.close;
		this.scale_( name );
	}
	
	// --------------------------------------------------------------------------------------------------------
	
	state { // post the state of the drone (not the synth state which is below)
		
		var string;
		
		string = "\nname : "++ name ++
		"\ntype : " ++ type.asString ++ 
		"\ntonic : "++ tonic.asString ++ 
		"\nharmonics : " ++ harmonics.asString ++ 
		"\nfundamental : " ++ fundamental.asString ++
		"\nfreq : " ++ freq.asString ++
		"\namp : " ++ amp.asString ++
		"\ntuning : " ++ tuning.asString ++ 
		"\nratio : " ++ ratio.asString ++
		"\nscale : " ++ scale.asString ++ 
		"\ndegree : " ++ degree.asString ++
		"\nsynths : " ++ synths.asString ++
		"\n";
		
		if(hub.post, {
			hub.interpreter.postview.string_(string);
		});

		^string;
	}

	params { // same as above (state), but in a format that's easy to change
		var string;
		string = "~"++name++"\n"++
		"~"++name++".harmonics = "++harmonics++"\n"++
		"~"++name++".amp = "++amp++"\n"++
		"~"++name++".speed = "++speed++"\n"++
		"~"++name++".length = "++length++"\n"++
		"~"++name++".angle = "++angle++"\n"++
		"~"++name++".degree = "++degree++"\n"++
		"~"++name++".ratio = "++ratio++"\n"++
		"~"++name++".env = "++env++"\n"++
		"~"++name++".octave = "++octave++"\n";
		^string;
	}
	
	methods { // post what methods the drone takes
		
		^"\ntonic_ : setting the tonic from the fundamental" ++
		"\nharmonics_ : set the number of harmonics" ++ 
		"\nfreq_ : set the frequency of a drone (same as tonic, but different interface)" ++ 
		"\nratio_ : set ratio in the chosen tuning" ++ 
		"\namp_ : set the amplitude" ++ 
		"\nrelAmp_ : set the relative amplitude" ++ 
		"\nspeed_ : set the speed (0 to 100)" ++ 
		"\nangle_ : set the angle (0 to 360 - starting on right)" ++ 
		"\nlength_ : set the length of the drone (0 to 360)" ++ 
		"\nselected_ : set selection to true or false" ++ 
		"\ntuning_ : set tuning (better done on a drones level)" ++ 
		"\nratio_ : set the tuning ratio (the semitone) indexing from 1" ++ 
		"\nscale_ : set the scale (better done on a drones level)" ++ 
		"\ndegree_ : set the scale degree (the note in the scale) indexing from 1" ++ 
		"\ntype_ : set the synth type (saw, sine, tri, cub, pulse, formant)" 
	}

	synth { // get the state of the synth 
		
		^switch(type)
			{ \saw     }{ 
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
			}
			{ \sine    }{ 
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
			 }
			{ \tri     }{ 
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");

			}
			{ \cub     }{ 
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			}
			{ \pulse   }{
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			}
			{ \eliane   }{
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			}			
			{ \noise   }{
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			}		
			{ \klank   }{
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			}		
			{ \gendy   }{
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			}		
			{ \formant }{
				("\nfreq : "++ synthParams[\freq].asString ++
				"\nharmonics : " ++ synthParams[\harmonics].asString ++ 
				"\nformfreq : " ++ synthParams[\formfreq].asString ++ 
				"\nbwfreq : " ++ synthParams[\bwfreq].asString ++ 
				"\namp : "++ synthParams[\amp].asString ++ 
				"\nresonance : " ++ synthParams[\resonance].asString ++
				"\noscfreq : " ++ synthParams[\oscfreq].asString ++ 
				"\noscamp : " ++ synthParams[\oscamp].asString ++
				"\nenv : " ++ synthParams[\env].asString ++
				"\ndep : " ++ synthParams[\dep].asString ++ 
				"\narr : " ++ synthParams[\arr].asString ++
				"\ntime : " ++ synthParams[\time].asString ++
				"\nfgate : " ++ synthParams[\fgate].asString ++
				"\ndetune : " ++ synthParams[\detune].asString ++
				"\n");
				
			};
	}
	
	// ---  below is all synth creation and drawing

	update {
		rotation = (rotation + speed)%(2*pi);
		angle = rotation;
		this.synthMachine(rotation);
		oppositemove = false; // if(speed>0, {false}, {true});
	}

	draw {
	     ^{
			if(selected, { 
				Pen.width = 1.5;
				strokeColor.alpha_(amp+0.5).set; 
			},{
				Pen.width = 1;
		     	strokeColor.alpha_(amp+0.1).set;
			});
			if(dying, {Pen.width = 1; Color.red(0.5).alpha_(amp+0.5).set });
			Pen.addAnnularWedge(
				point+0.5, 
				innersize, 
				outersize, 	
				rotation, 
				length
			);
			Pen.perform(\stroke);
	
	/// TESTING RESONANCE REPRESENTATION
			if(showResonance, { // if the resonance strip is visible/audible
				Pen.addAnnularWedge(
					point+0.5, 
					resonsize, 
					resonsize+4, 	
					rotation, 
					length
				);
				Pen.perform(\stroke);
				
				fillColor.alpha_(amp).set;
				Pen.addAnnularWedge(
					point+0.5, 
					resonsize, 
					resonsize+4, 	
					rotation, 
					length
				);
				Pen.perform(\fill);
			});
	/////////////////////////////////////
	
			Pen.width = 1;
			//alpha = amp / 2;
			fillColor.alpha_((ampMult*amp)+Env.new([0,0.2,0.1,0], [0.1,0.1, 0.6],[-3,0,3]).at(amp)).set;
			Pen.addAnnularWedge(
				point+0.5, 
				innersize, 
				outersize, 	
				rotation, 
				length
			);
			Pen.perform(\fill);
	     }
	}

	initSynthParams { |argtonic, argfreq, argharmonics, argamp, argdegree, argoctave, argenv|
			// TODO: the different synths might have different args
			^switch(type)
			{\saw}{ 
				(type:\saw, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\sine}{ 
				(type:\sine, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			 }
			{\tri}{ 
				(type:\tri, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\cub}{ 
				(type:\cub, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\pulse}{
				(type:\pulse, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\formant}{
				(type:\formant, detune:0, tonic:argtonic, freq:argfreq, formfreq:3, bwfreq:2, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\eliane}{
				(type:\eliane, detune:0, tonic:argtonic, freq:argfreq, formfreq:3, bwfreq:2, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\noise}{
				(type:\noise, detune:0, tonic:argtonic, freq:argfreq, formfreq:3, bwfreq:2, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\klank}{
				(type:\klank, detune:0, tonic:argtonic, freq:argfreq, formfreq:3, bwfreq:2, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\gendy}{
				(type:\gendy, detune:0, tonic:argtonic, freq:argfreq, formfreq:3, bwfreq:2, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			
			{\atom}{
				(type:\formant, detune:0, tonic:argtonic, freq:argfreq, formfreq:3, bwfreq:2, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			}
			{\pad}{
				(type:\pad, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			} { // everything else
				(type:type, detune:0, tonic:argtonic, freq:argfreq, harmonics:argharmonics, amp:argamp, oscfreq:0.1, oscamp:0, gate:1, env:argenv, dep:1, arr:2, time:10, fgate:0, resamp:0, resonance:1, degree:argdegree, octave:argoctave);
			};
	}

	createSynth { |out=0|
		var sp = synthParams;
		out = out + hub.channelOffset;
		^switch(type)
			{\saw}{ // add detune here.
				Synth(\dronesaw, [\out, out, \freq, freq, \harmonics, harmonics, \amp, amp, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\sine}{ 
				Synth(\dronesine, [\out, out, \freq, freq, \harmonics, harmonics, \amp, amp, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance], 
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\tri}{ 
				Synth(\dronetri, [\out, out, \freq, freq, \harmonics, harmonics, \amp, amp, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance], 
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\cub}{ 
				Synth(\dronecub, [\out, out, \freq, freq, \harmonics, harmonics, \amp, amp, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance], 
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\pulse}{ 
				Synth(\dronepulse, [\out, out, \freq, freq, \harmonics, harmonics, \amp, amp, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance], 
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\atom}{ 
				// here send an OSC message with the new out:
				this.sendAtomChannel(out, 1); // in DroneAtom file
				}
			{\formant}{ 
				Synth(\droneformant, [\out, out, \freq, freq, \formfreq, sp[\formfreq], \bwfreq, sp[\bwfreq], \harmonics, harmonics, \resonance, sp[\resonance], 
				\amp, amp, \oscfreq, sp[\oscfreq], \oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], 
				\time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\eliane}{ 
				Synth(\eliane, [\out, out, \freq, freq, \formfreq, sp[\formfreq], \bwfreq, sp[\bwfreq], \harmonics, harmonics, \resonance, sp[\resonance], 
				\amp, amp, \oscfreq, sp[\oscfreq], \oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], 
				\time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\noise}{ 
				Synth(\dronenoise, [\out, out, \freq, freq, \formfreq, sp[\formfreq], \bwfreq, sp[\bwfreq], \harmonics, harmonics, \resonance, sp[\resonance], 
				\amp, amp, \oscfreq, sp[\oscfreq], \oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], 
				\time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\klank}{ 
				Synth(\droneklank, [\out, out, \freq, freq, \formfreq, sp[\formfreq], \bwfreq, sp[\bwfreq], \harmonics, harmonics, \resonance, sp[\resonance], 
				\amp, amp, \oscfreq, sp[\oscfreq], \oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], 
				\time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\gendy}{ 
				Synth(\dronegendy, [\out, out, \freq, freq, \formfreq, sp[\formfreq], \bwfreq, sp[\bwfreq], \harmonics, harmonics, \resonance, sp[\resonance], 
				\amp, amp, \oscfreq, sp[\oscfreq], \oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], 
				\time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }

			{\pad}{ 
				Synth(\dronepad, [\out, out, \freq, freq, \harmonics, harmonics, \amp, amp, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{ var nearestFreq, midinote; // else, it's a sample
				var noteData, startLoop, endLoop;
				noteData = DroneSynths.noteData(type, freq);
				midinote = noteData[0].cpsmidi; // this is a reference freq for microtuning or out of tune samples
				startLoop = noteData[1];
				endLoop = noteData[2];
				//[\startLoop_____________, startLoop, \endLoop, endLoop].postln;
				Synth(\droneinstr, [\buffer, buffer, \out, out, \freq, freq, \midinote, midinote, \harmonics, harmonics*2, \amp, amp*2, \startLoop, startLoop,
				\endLoop, endLoop, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) 
			};
				/*
			{\piano}{ 
				var nearestFreq, midinote;
				var noteData, startLoop, endLoop;
				//nearestFreq = DroneSynths.nearestFreq(\piano, freq);
				[\freq, freq].postln;
				//[\nearestFreq, nearestFreq].postln;
				// calculate rate here!
				// (freq.cpsmidi-60).midiratio
				// move
				noteData = DroneSynths.noteData(\piano, freq);
				// midinote = DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote];
				midinote = noteData[0].cpsmidi; // this is a reference freq for microtuning or out of tune samples
				startLoop = noteData[1];
				endLoop = noteData[2];
				[\startLoop_____________, startLoop, \endLoop, endLoop].postln;
				// rate = (freq.cpsmidi - DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote] ).midiratio;
				// [\rate________________, rate].postln;
				Synth(\instr, [\out, out, \buffer, buffer, \freq, freq, \midinote, midinote, \harmonics, harmonics*2, \amp, amp, \startLoop, startLoop,
				\endLoop, endLoop, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\organ}{ 
				var nearestFreq, midinote;
				var noteData, startLoop, endLoop;
				//nearestFreq = DroneSynths.nearestFreq(\piano, freq);
				[\freq, freq].postln;
				//[\nearestFreq, nearestFreq].postln;
				// calculate rate here!
				// (freq.cpsmidi-60).midiratio
				// move
				noteData = DroneSynths.noteData(\organ, freq);
				// midinote = DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote];
				midinote = noteData[0].cpsmidi; // this is a reference freq for microtuning or out of tune samples
				startLoop = noteData[1];
				endLoop = noteData[2];
				[\startLoop_____________, startLoop, \endLoop, endLoop].postln;
				// rate = (freq.cpsmidi - DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote] ).midiratio;
				// [\rate________________, rate].postln;
				Synth(\instr, [\out, out, \buffer, buffer, \freq, freq, \midinote, midinote, \harmonics, harmonics*2, \amp, amp, \startLoop, startLoop,
				\endLoop, endLoop, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\organdist}{ 
				var nearestFreq, midinote;
				var noteData, startLoop, endLoop;
				//nearestFreq = DroneSynths.nearestFreq(\piano, freq);
				[\freq, freq].postln;
				//[\nearestFreq, nearestFreq].postln;
				// calculate rate here!
				// (freq.cpsmidi-60).midiratio
				// move
				noteData = DroneSynths.noteData(\organdist, freq);
				// midinote = DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote];
				midinote = noteData[0].cpsmidi; // this is a reference freq for microtuning or out of tune samples
				startLoop = noteData[1];
				endLoop = noteData[2];
				[\startLoop_____________, startLoop, \endLoop, endLoop].postln;
				// rate = (freq.cpsmidi - DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote] ).midiratio;
				// [\rate________________, rate].postln;
				Synth(\instr, [\out, out, \buffer, buffer, \freq, freq, \midinote, midinote, \harmonics, harmonics*2, \amp, amp, \startLoop, startLoop,
				\endLoop, endLoop, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) }
			{\organharm}{ 
				var nearestFreq, midinote;
				var noteData, startLoop, endLoop;
				//nearestFreq = DroneSynths.nearestFreq(\piano, freq);
				[\freq, freq].postln;
				//[\nearestFreq, nearestFreq].postln;
				// calculate rate here!
				// (freq.cpsmidi-60).midiratio
				// move
				noteData = DroneSynths.noteData(\organharm, freq);
				// midinote = DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote];
				midinote = noteData[0].cpsmidi; // this is a reference freq for microtuning or out of tune samples
				startLoop = noteData[1];
				endLoop = noteData[2];
				[\startLoop_____________, startLoop, \endLoop, endLoop].postln;
				// rate = (freq.cpsmidi - DroneSynths.sampleDict[\piano][nearestFreq.asSymbol][\midinote] ).midiratio;
				// [\rate________________, rate].postln;
				Synth(\instr, [\out, out, \buffer, buffer, \freq, freq, \midinote, midinote, \harmonics, harmonics*2, \amp, amp, \startLoop, startLoop,
				\endLoop, endLoop, \oscfreq, sp[\oscfreq], \resonance, sp[\resonance],
				\oscamp, sp[\oscamp], \env, sp[\env], \dep, sp[\dep], \arr, sp[\arr], \time, sp[\time], \fgate, sp[\fgate], \detune, sp[\detune]], synthgroup) };
			*/			
	}
		
	startSynth { 
	//	synthgroup = Group.new; // create a new for next synths
		if(speed>0, { 
			length.raddeg.round(1).do({ |i| // a virtual rotation around the circle, to initialise the synths
				var rot;
				rot = (rotation + i.degrad - length) % (2*pi);
				this.synthMachine(rot);
			});
		}, {
			length.raddeg.round(1).do({ |i| // a virtual rotation around the circle, to initialise the synths
				var rot;
				rot = ((rotation + length) - i.degrad ) % (2*pi);
				this.synthMachine(rot);
			});			
		});
	}

	getSynths { // used for debugging
		^synths;	
	}
	
	synthMachine { |rotation| // rotation set as an argument as this method is used to create the synth in method below
		// slightly complex method, but prly necessary for the complex interactions involved 
		switch(nrChannels)
		{1} {
			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
			}, {
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						});
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						});	
					}); 
				}, { // if speed is < 0 and backmove not
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						});
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						});
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating clockwise
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						});
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						});	
					});				
				}, { // if speed is < 0 and backmove not
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						});
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						});	
					}); 
				});
			});
		}
		{2} {
			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
				if(synths[1].isNil, { synths[1] = this.createSynth(1) });
			}, {
				// left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating clockwise
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				});
				// right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 359.0.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating clockwise
					if( (((rotation%(2*pi))) < 359.9.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				});
			});
		}
		{4} {
			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
				if(synths[1].isNil, { synths[1] = this.createSynth(1) });
				if(synths[2].isNil, { synths[2] = this.createSynth(2) });
				if(synths[3].isNil, { synths[3] = this.createSynth(3) });
			}, {
				// left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				});
				// top speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				});
				// right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 359.0.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating clockwise
					if( (((rotation%(2*pi))) < 359.9.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				});
				// bottom speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 90.degrad) && (((rotation+length)%(2*pi)) < 95.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 90.degrad) && ((rotation%(2*pi)) < 95.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 90.degrad) && ((rotation%(2*pi)) > 85.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 90.degrad) && (((rotation+length)%(2*pi)) > 85.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 90.degrad) && ((rotation%(2*pi)) > 85.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 90.degrad) && (((rotation+length)%(2*pi)) > 85.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 90.degrad) && (((rotation+length)%(2*pi)) < 95.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 90.degrad) && ((rotation%(2*pi)) < 95.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				});
			});
		}
		{44} { // quadrophonic corner system
			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
				if(synths[1].isNil, { synths[1] = this.createSynth(1) });
				if(synths[2].isNil, { synths[2] = this.createSynth(2) });
				if(synths[3].isNil, { synths[3] = this.createSynth(3) });
			}, {
				// top-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 225.degrad) && (((rotation+length)%(2*pi)) < 230.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 225.degrad) && ((rotation%(2*pi)) < 230.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 225.degrad) && ((rotation%(2*pi)) > 220.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 225.degrad) && (((rotation+length)%(2*pi)) > 220.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 225.degrad) && ((rotation%(2*pi)) > 220.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 225.degrad) && (((rotation+length)%(2*pi)) > 220.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 225.degrad) && (((rotation+length)%(2*pi)) < 230.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 225.degrad) && ((rotation%(2*pi)) < 230.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				});
				// top-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 315.degrad) && (((rotation+length)%(2*pi)) < 320.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 315.degrad) && ((rotation%(2*pi)) < 320.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 315.degrad) && ((rotation%(2*pi)) > 310.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 315.degrad) && (((rotation+length)%(2*pi)) > 310.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 315.degrad) && ((rotation%(2*pi)) > 310.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 315.degrad) && (((rotation+length)%(2*pi)) > 310.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 315.degrad) && (((rotation+length)%(2*pi)) < 320.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 315.degrad) && ((rotation%(2*pi)) < 320.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				});
				// bottom-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 45.degrad) && (((rotation+length)%(2*pi)) < 50.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 45.degrad) && ((rotation%(2*pi)) < 50.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 45.degrad) && ((rotation%(2*pi)) > 40.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 45.degrad) && (((rotation+length)%(2*pi)) > 40.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 45.degrad) && ((rotation%(2*pi)) > 40.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 45.degrad) && (((rotation+length)%(2*pi)) > 40.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 45.degrad) && (((rotation+length)%(2*pi)) < 50.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 45.degrad) && ((rotation%(2*pi)) < 50.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				});
				// bottom-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 135.degrad) && (((rotation+length)%(2*pi)) < 140.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 135.degrad) && ((rotation%(2*pi)) < 140.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 135.degrad) && ((rotation%(2*pi)) > 130.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 135.degrad) && (((rotation+length)%(2*pi)) > 130.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 135.degrad) && ((rotation%(2*pi)) > 130.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 135.degrad) && (((rotation+length)%(2*pi)) > 130.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 135.degrad) && (((rotation+length)%(2*pi)) < 140.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 135.degrad) && ((rotation%(2*pi)) < 140.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					})
				})
			})
		}		
		{5} {

			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
				if(synths[1].isNil, { synths[1] = this.createSynth(1) });
				if(synths[2].isNil, { synths[2] = this.createSynth(2) });
				if(synths[3].isNil, { synths[3] = this.createSynth(3) });
				if(synths[4].isNil, { synths[4] = this.createSynth(4) });
			}, {
				// bottom-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 160.degrad) && (((rotation+length)%(2*pi)) < 165.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 160.degrad) && ((rotation%(2*pi)) < 165.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 160.degrad) && ((rotation%(2*pi)) > 155.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 160.degrad) && (((rotation+length)%(2*pi)) > 155.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 160.degrad) && ((rotation%(2*pi)) > 155.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 160.degrad) && (((rotation+length)%(2*pi)) > 155.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 160.degrad) && (((rotation+length)%(2*pi)) < 165.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 160.degrad) && ((rotation%(2*pi)) < 165.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				});
				// left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 240.degrad) && (((rotation+length)%(2*pi)) < 245.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 240.degrad) && ((rotation%(2*pi)) < 245.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 240.degrad) && ((rotation%(2*pi)) > 235.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 240.degrad) && (((rotation+length)%(2*pi)) > 235.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 240.degrad) && ((rotation%(2*pi)) > 235.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 240.degrad) && (((rotation+length)%(2*pi)) > 235.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 240.degrad) && (((rotation+length)%(2*pi)) < 245.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 240.degrad) && ((rotation%(2*pi)) < 245.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				});
				// mid speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				});
				// right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 300.degrad) && (((rotation+length)%(2*pi)) < 305.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 300.degrad) && ((rotation%(2*pi)) < 305.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 300.degrad) && ((rotation%(2*pi)) > 295.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 300.degrad) && (((rotation+length)%(2*pi)) > 295.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 300.degrad) && ((rotation%(2*pi)) > 295.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 300.degrad) && (((rotation+length)%(2*pi)) > 295.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 300.degrad) && (((rotation+length)%(2*pi)) < 305.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 300.degrad) && ((rotation%(2*pi)) < 305.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				});
				// bottom-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 20.degrad) && (((rotation+length)%(2*pi)) < 25.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					});				
					if( (((rotation%(2*pi))) > 20.degrad) && ((rotation%(2*pi)) < 25.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 20.degrad) && ((rotation%(2*pi)) > 15.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 20.degrad) && (((rotation+length)%(2*pi)) > 15.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 20.degrad) && ((rotation%(2*pi)) > 15.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 20.degrad) && (((rotation+length)%(2*pi)) > 15.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 20.degrad) && (((rotation+length)%(2*pi)) < 25.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					});				
					if( (((rotation%(2*pi))) > 20.degrad) && ((rotation%(2*pi)) < 25.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					}); 
				});
			});
		}
		{6} {
			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
				if(synths[1].isNil, { synths[1] = this.createSynth(1) });
				if(synths[2].isNil, { synths[2] = this.createSynth(2) });
				if(synths[3].isNil, { synths[3] = this.createSynth(3) });
				if(synths[4].isNil, { synths[4] = this.createSynth(4) });
				if(synths[5].isNil, { synths[5] = this.createSynth(5) });
			}, {
				// left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				});
				// top-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 240.degrad) && (((rotation+length)%(2*pi)) < 245.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 240.degrad) && ((rotation%(2*pi)) < 245.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 240.degrad) && ((rotation%(2*pi)) > 235.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 240.degrad) && (((rotation+length)%(2*pi)) > 235.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 240.degrad) && ((rotation%(2*pi)) > 235.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 240.degrad) && (((rotation+length)%(2*pi)) > 235.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 240.degrad) && (((rotation+length)%(2*pi)) < 245.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 240.degrad) && ((rotation%(2*pi)) < 245.degrad), {
						if(synths[1].isNil.not, {
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				});
				// top-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 300.degrad) && (((rotation+length)%(2*pi)) < 305.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 300.degrad) && ((rotation%(2*pi)) < 305.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 300.degrad) && ((rotation%(2*pi)) > 295.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 300.degrad) && (((rotation+length)%(2*pi)) > 295.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 300.degrad) && ((rotation%(2*pi)) > 295.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 300.degrad) && (((rotation+length)%(2*pi)) > 295.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 300.degrad) && (((rotation+length)%(2*pi)) < 305.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 300.degrad) && ((rotation%(2*pi)) < 305.degrad), {
						if(synths[2].isNil.not, {
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				});
				// right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 359.0.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating clockwise
					if( (((rotation%(2*pi))) < 359.9.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[3].isNil.not, {
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				});
				// bottom-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 60.degrad) && (((rotation+length)%(2*pi)) < 65.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					});				
					if( (((rotation%(2*pi))) > 60.degrad) && ((rotation%(2*pi)) < 65.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 60.degrad) && ((rotation%(2*pi)) > 55.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 60.degrad) && (((rotation+length)%(2*pi)) > 55.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 60.degrad) && ((rotation%(2*pi)) > 55.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 60.degrad) && (((rotation+length)%(2*pi)) > 55.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 60.degrad) && (((rotation+length)%(2*pi)) < 65.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					});				
					if( (((rotation%(2*pi))) > 60.degrad) && ((rotation%(2*pi)) < 65.degrad), {
						if(synths[4].isNil.not, {
							synths[4].release;
							synths[4] = nil;
						})	
					}); 
				});
				// bottom-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 120.degrad) && (((rotation+length)%(2*pi)) < 125.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					});				
					if( (((rotation%(2*pi))) > 120.degrad) && ((rotation%(2*pi)) < 125.degrad), {
						if(synths[5].isNil.not, {
							synths[5].release;
							synths[5] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 120.degrad) && ((rotation%(2*pi)) > 115.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 120.degrad) && (((rotation+length)%(2*pi)) > 115.degrad), {
						if(synths[5].isNil.not, {
							synths[5].release;
							synths[5] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 120.degrad) && ((rotation%(2*pi)) > 115.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 120.degrad) && (((rotation+length)%(2*pi)) > 115.degrad), {
						if(synths[5].isNil.not, {
							synths[5].release;
							synths[5] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 120.degrad) && (((rotation+length)%(2*pi)) < 125.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					});				
					if( (((rotation%(2*pi))) > 120.degrad) && ((rotation%(2*pi)) < 125.degrad), {
						if(synths[5].isNil.not, {
							synths[5].release;
							synths[5] = nil;
						})	
					})
				})
			})
		}
		{8} {
			if(length> 350.degrad, { // if the circle is nearly full
				if(synths[0].isNil, { synths[0] = this.createSynth(0) });
				if(synths[1].isNil, { synths[1] = this.createSynth(1) });
				if(synths[2].isNil, { synths[2] = this.createSynth(2) });
				if(synths[3].isNil, { synths[3] = this.createSynth(3) });
				if(synths[4].isNil, { synths[4] = this.createSynth(4) });
				if(synths[5].isNil, { synths[5] = this.createSynth(5) });
				if(synths[6].isNil, { synths[6] = this.createSynth(6) });
				if(synths[7].isNil, { synths[7] = this.createSynth(7) });
			}, {
				// left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 180.degrad) && ((rotation%(2*pi)) > 175.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 180.degrad) && (((rotation+length)%(2*pi)) > 175.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 180.degrad) && (((rotation+length)%(2*pi)) < 185.degrad), {
						if(synths[0].isNil, {
							synths[0] = this.createSynth(0);
						})
					});				
					if( (((rotation%(2*pi))) > 180.degrad) && ((rotation%(2*pi)) < 185.degrad), {
						if(synths[0].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(0, 0)});
							synths[0].release;
							synths[0] = nil;
						})	
					}); 
				});
				// top-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 225.degrad) && (((rotation+length)%(2*pi)) < 230.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 225.degrad) && ((rotation%(2*pi)) < 230.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 225.degrad) && ((rotation%(2*pi)) > 220.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 225.degrad) && (((rotation+length)%(2*pi)) > 220.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 225.degrad) && ((rotation%(2*pi)) > 220.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 225.degrad) && (((rotation+length)%(2*pi)) > 220.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 225.degrad) && (((rotation+length)%(2*pi)) < 230.degrad), {
						if(synths[1].isNil, {
							synths[1] = this.createSynth(1);
						})
					});				
					if( (((rotation%(2*pi))) > 225.degrad) && ((rotation%(2*pi)) < 230.degrad), {
						if(synths[1].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(1, 0)});
							synths[1].release;
							synths[1] = nil;
						})	
					}); 
				});
				// top speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[2].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(2, 0)});
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[2].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(2, 0)});
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 270.degrad) && ((rotation%(2*pi)) > 265.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 270.degrad) && (((rotation+length)%(2*pi)) > 265.degrad), {
						if(synths[2].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(2, 0)});
							synths[2].release;
							synths[2] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 270.degrad) && (((rotation+length)%(2*pi)) < 275.degrad), {
						if(synths[2].isNil, {
							synths[2] = this.createSynth(2);
						})
					});				
					if( (((rotation%(2*pi))) > 270.degrad) && ((rotation%(2*pi)) < 275.degrad), {
						if(synths[2].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(2, 0)});
							synths[2].release;
							synths[2] = nil;
						})	
					}); 
				});
				// top-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 315.degrad) && (((rotation+length)%(2*pi)) < 320.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 315.degrad) && ((rotation%(2*pi)) < 320.degrad), {
						if(synths[3].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(3, 0)});
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 315.degrad) && ((rotation%(2*pi)) > 310.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 315.degrad) && (((rotation+length)%(2*pi)) > 310.degrad), {
						if(synths[3].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(3, 0)});
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 315.degrad) && ((rotation%(2*pi)) > 310.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 315.degrad) && (((rotation+length)%(2*pi)) > 310.degrad), {
						if(synths[3].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(3, 0)});
							synths[3].release;
							synths[3] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 315.degrad) && (((rotation+length)%(2*pi)) < 320.degrad), {
						if(synths[3].isNil, {
							synths[3] = this.createSynth(3);
						})
					});				
					if( (((rotation%(2*pi))) > 315.degrad) && ((rotation%(2*pi)) < 320.degrad), {
						if(synths[3].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(3, 0)});
							synths[3].release;
							synths[3] = nil;
						})	
					}); 
				});
				// right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[4].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(4, 0)});
							synths[4].release;
							synths[4] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 359.0.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[4].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(4, 0)});
							synths[4].release;
							synths[4] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating clockwise
					if( (((rotation%(2*pi))) < 359.9.degrad) && ((rotation%(2*pi)) > 355.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 359.9.degrad) && (((rotation+length)%(2*pi)) > 355.degrad), {
						if(synths[4].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(4, 0)});
							synths[4].release;
							synths[4] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 0.degrad) && (((rotation+length)%(2*pi)) < 5.degrad), {
						if(synths[4].isNil, {
							synths[4] = this.createSynth(4);
						})
					});				
					if( (((rotation%(2*pi))) > 0.degrad) && ((rotation%(2*pi)) < 5.degrad), {
						if(synths[4].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(4, 0)});
							synths[4].release;
							synths[4] = nil;
						})	
					}); 
				});
				// bottom-right speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 45.degrad) && (((rotation+length)%(2*pi)) < 50.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					});				
					if( (((rotation%(2*pi))) > 45.degrad) && ((rotation%(2*pi)) < 50.degrad), {
						if(synths[5].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(5, 0)});
							synths[5].release;
							synths[5] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 45.degrad) && ((rotation%(2*pi)) > 40.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 45.degrad) && (((rotation+length)%(2*pi)) > 40.degrad), {
						if(synths[5].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(5, 0)});
							synths[5].release;
							synths[5] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 45.degrad) && ((rotation%(2*pi)) > 40.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 45.degrad) && (((rotation+length)%(2*pi)) > 40.degrad), {
						if(synths[5].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(5, 0)});
							synths[5].release;
							synths[5] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 45.degrad) && (((rotation+length)%(2*pi)) < 50.degrad), {
						if(synths[5].isNil, {
							synths[5] = this.createSynth(5);
						})
					});				
					if( (((rotation%(2*pi))) > 45.degrad) && ((rotation%(2*pi)) < 50.degrad), {
						if(synths[5].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(5, 0)});
							synths[5].release;
							synths[5] = nil;
						})	
					}); 
				});
				// bottom speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 90.degrad) && (((rotation+length)%(2*pi)) < 95.degrad), {
						if(synths[6].isNil, {
							synths[6] = this.createSynth(6);
						})
					});				
					if( (((rotation%(2*pi))) > 90.degrad) && ((rotation%(2*pi)) < 95.degrad), {
						if(synths[6].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(6, 0)});
							synths[6].release;
							synths[6] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 90.degrad) && ((rotation%(2*pi)) > 85.degrad), {
						if(synths[6].isNil, {
							synths[6] = this.createSynth(6);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 90.degrad) && (((rotation+length)%(2*pi)) > 85.degrad), {
						if(synths[6].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(6, 0)});
							synths[6].release;
							synths[6] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 90.degrad) && ((rotation%(2*pi)) > 85.degrad), {
						if(synths[6].isNil, {
							synths[6] = this.createSynth(6);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 90.degrad) && (((rotation+length)%(2*pi)) > 85.degrad), {
						if(synths[6].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(6, 0)});
							synths[6].release;
							synths[6] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 90.degrad) && (((rotation+length)%(2*pi)) < 95.degrad), {
						if(synths[6].isNil, {
							synths[6] = this.createSynth(6);
						})
					});				
					if( (((rotation%(2*pi))) > 90.degrad) && ((rotation%(2*pi)) < 95.degrad), {
						if(synths[6].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(6, 0)});
							synths[6].release;
							synths[6] = nil;
						})	
					}); 
				});
				// bottom-left speaker
				if((speed>0) && (oppositemove.not), { // if rotating clockwise
					if( ((((rotation+length)%(2*pi))) > 135.degrad) && (((rotation+length)%(2*pi)) < 140.degrad), {
						if(synths[7].isNil, {
							synths[7] = this.createSynth(7);
						})
					});				
					if( (((rotation%(2*pi))) > 135.degrad) && ((rotation%(2*pi)) < 140.degrad), {
						if(synths[7].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(7, 0)});
							synths[7].release;
							synths[7] = nil;
						})	
					}); 
				}, {
					if( (((rotation%(2*pi))) < 135.degrad) && ((rotation%(2*pi)) > 130.degrad), {
						if(synths[7].isNil, {
							synths[7] = this.createSynth(7);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 135.degrad) && (((rotation+length)%(2*pi)) > 130.degrad), {
						if(synths[7].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(7, 0)});
							synths[7].release;
							synths[7] = nil;
						})	
					});				
				});
				if((speed<0) && (oppositemove.not), { // if rotating anti clockwise
					if( (((rotation%(2*pi))) < 135.degrad) && ((rotation%(2*pi)) > 130.degrad), {
						if(synths[7].isNil, {
							synths[7] = this.createSynth(7);
						})
					}); 				
					if( ((((rotation+length)%(2*pi))) < 135.degrad) && (((rotation+length)%(2*pi)) > 130.degrad), {
						if(synths[7].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(7, 0)});
							synths[7].release;
							synths[7] = nil;
						})	
					});				
				}, {
					if( ((((rotation+length)%(2*pi))) > 135.degrad) && (((rotation+length)%(2*pi)) < 140.degrad), {
						if(synths[7].isNil, {
							synths[7] = this.createSynth(7);
						})
					});				
					if( (((rotation%(2*pi))) > 135.degrad) && ((rotation%(2*pi)) < 140.degrad), {
						if(synths[7].isNil.not, {
							if(atomcount > -1, {this.sendAtomChannel(7, 0)});
							synths[7].release;
							synths[7] = nil;
						})	
					})
				})
			})
		}
	}
}
