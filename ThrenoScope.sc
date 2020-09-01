// scales: http://www.looknohands.com/chordhouse/piano/


// xCoAx -> 1400*1050


// system
// TODO: implement a parameter-to-midi feature where a specific parameter of a drone can be assigned to a slider or a knob. (-> this should be stored in states, such that a setup can be recalled with parameters etc.)
// TODO: make an UNDO function
// TODO: make the system argument save (if use writes wrong types) (eg. createDrone(\saw, \minor, 2))


// sound

// TODO: Add Lag Ugens to detune and other ~drn.set() arguments
// TODO: Cube sinewaves in the drones SinOsc.ar().cub
// TODO: oscillating amp.
// TODO: fix the perceived loudness when using resonace !!!
// TODO: create a DynKlank and DynKlang synthdefs
// TODO: Explore "harmonic stretching" as in the harmonics of a piano (8th harmonic stretched) Here I can use the Drag Handle (as in recParam) to tune the harmonic
// TODO: explore the use of lissajous curves to explore harmonic relationships
// TODO: create a noise Drone. But using BPF or FFT brick wall?
// TODO: add LFNoise2 into all frequency args.
// ToDO: Check amplitude argument for sample instruments. Also check RedFrik Phahsor for playing them
// i.e., fix the tonic_(1, 20) glitches that will be heard when the pitch is changed.
// a saw wave version
// play{{({|i|x=i+1;y=LFNoise2.ar(0.1);f=77*(x/2)+y.range(-2,2);LPF.ar(Saw.ar(f,y*(Line.ar(0,0.2,99.rand)/(x*0.3))),f*3)}!rrand(9,28)).sum}!2}
// TODO: add tremolo to synths


// drones
// TODO: user specified Chords into chorddict
// TODO: add a spectrogram view
// TODO: expose drone parameters down to a MIDI interface

// score
// TODO: make it possible to start multiple scores, and stop a unique one
// TODO: chords and satellites into the dronescore (as a line representation)
// TODO: Fix problem if one tries to swap state with the same state.
// Having removed the opInt - addToScore needs to be added for all relevant methods
// BUG: Machines cannot be created from a score (cannot be called from this process)
// TODO: solve problem with .interpret code (Try e.g., the score for Lauren, \s1Intro)
// TODO: .killAll needs to go and send a .kill message to all drones, thus ending them on the timeline
// TODO: if dronescore is playing without view, and the view is started, then start timeline at location

// TODO: 	loadsamples here : DroneSynths.new(loadsamples: false);
// TODO: ~drones.killAll should kill running scores too

// machines
// TODO: fix them all!


// TODO: allow for frequency creation argument, as in ~drones.createDrone(\saw, freq: 432)
/// TODO: Make ~shf.freq post the frequency (or any other args) into the freq window

// Display Modes:
// perform
// dev
// displayFS (full screen)
// displayWin (window)

// BUG TO FIX -> When running a score, you can't kill Satellites
// PlayBufCF & PlayBufAlt in the wslib Quark. Crossfade and back-and-forth playback.�

// TODO: Add literal arrays where appropriate

/*
Lukas:
I just had another �feature suggestion� for the ThrenoScope in mind that I forgot to ask you about earlier: I would find it very useful to be able to change the curve of interpolation between parameters. So when calling for example �~bob.harmonics_(10, 30)�, you could add something like �.exp�, �.sqr�, �.cub� to interpolate exponentially/inverted exponentially, cubicly etc.. That seems like an important compositional parameter when dealing with these kinds of forms.

*/



ThrenoScope {
	var window, screendimension;
	var hub;

	*new { arg channels=2, mode=\perform, key="A", appPath;
		^super.new.initThrenoScope(channels, mode, key, appPath);
	}

	initThrenoScope { | channels, mode, key, appPath |
		var fundamental;
		var speakers, interpreter, machines, states;
		var tuning = \et12;
		var scale = \minor;
		// var mode = argmode;
		//var channels = argchannels;
		var threnoscopeColor;
		var border, fullScreen, bgcolor;

		//GUI.cocoa; // use this if on SC3.6-
		//GUI.qt; // use this if on SC3.7+
		
		if((Main.scVersionMajor<=3) && (Main.scVersionMinor<=6) && (thisProcess.platform.name == \osx), {
			"USING COCOA GUI on old versions of SuperCollider".postln;
			GUI.cocoa;
		});
		

		screendimension = Window.screenBounds.height;

		switch(mode)
			{\perform} { // regular perform mode with terminal and console
				border = false;
				fullScreen = true;
				bgcolor = Color.white;
				}
			{\performWin} { // regular perform mode with terminal and console but in a Window
				border = true;
				fullScreen = false;
				bgcolor = Color.white;
				}
			{\dev} { // development mode
				border = true;
				fullScreen = false;
				bgcolor = Color.white;
				}
			{\displayFS} { // full screen for display (for non-interactive performance)
				border = false;
				fullScreen = true;
				bgcolor = Color.black;
				}
			{\displayWin} { // display in a window (for app playback)
				border = true;
				fullScreen = false;
				bgcolor = Color.black;
			};

		window = Window("ThrenoScope", Rect(0, 0, screendimension, screendimension), resizable:false, border:border).front;

		if(fullScreen, { window.fullScreen });

		window.view.background = bgcolor;
		threnoscopeColor = Color.white;
		
		if(key.isNumber,{
			fundamental = key;
		},{
			fundamental = (\C:24, \Cs:25, \Db:25, \D:26, \Ds:27, \Eb:27, \E:28, \F:29, \Fs:30, \Gb:30, \G:31, \Gs:32, \A:33, \As:34, \Bb:34, \B:35, \C2:36, \Cs2:37, \Db2:37, \D2:38, \Ds2:39, \Eb2:39).at(key.asSymbol).midicps;
		});
//		fundamental = (\C:24, \Cs:25, \Db:25, \D:26, \Ds:27, \Eb:27, \E:28, \F:29, \Fs:30, \Gb:30, \G:31, \Gs:32, \A:33, \As:34, \Bb:34, \B:35, \C2:36, \Cs2:37, \Db2:37, \D2:38, \Ds2:39, \Eb2:39).at(key.asSymbol).midicps;

		// fundamental = 36.midicps; // in C
		// fundamental = 40.midicps; // in E
		// fundamental = (49-12).midicps; // in Es for bass sax (w. Inigo Ibaibarriaga)
		// fundamental = 34.midicps; // in Bb for bass clarinet (w. Pete Furniss)
		// fundamental = 58.2; // in Bb for bass clarinet (w. Pete Furniss)
		// fundamental = 55; // + (20.rand); // remove rand (just here to remove stress from ears)
		// fundamental = 53.6 + (20.rand); // remove rand (just here to remove stress from ears)
	 	// fundamental = 54; // this is if A = 432 Hz. A popular 19th century tuning.
		// fundamental = 30.midicps; // in F# (with Adriana Sa)
		// fundamental = 28.midicps;
		
		//fundamental = 55;


		// DroneLimiter.activate;
		// Drone - the key class of each drone
		// DroneMachine - the class containing the diverse machines
		// DroneGUI - an experiment with GUI
		// DroneChord / DroneSatellites / DroneGroup
		// DroneCodeScore - an experiment with Score

		hub = DroneHub.new( window, mode, scale, fundamental, threnoscopeColor, key, channels, appPath ); // - all key data accessible to other classes
		~singlehub = hub;
		
		DroneSynths.new(false, hub);

		states = DroneStates.new( hub ); // - storing states and recording/playing scores

		hub.registerStates( states );
		speakers = DroneSpeakers.new( hub, channels, fundamental ); // drawing background
		hub.registerSpeakers( speakers );
		~drones = DroneController.new( hub, tuning, scale, fundamental); // - main interface
		hub.registerDrones( ~drones );

		if((mode == \perform) || (mode == \performWin) || (mode == \dev), {
			interpreter = DroneInterpreter.new( hub, mode:mode); // - the 'console' for live coding
			hub.registerInterpreter( interpreter );
		});
	}

/*
	mode {arg mode;
		this.quit;
		this.initThrenoScope(hub.channels, mode, hub.key, hub.appPath);
	}
*/

	quit {
		~drones.killAll;
		window.close;
	}
}
