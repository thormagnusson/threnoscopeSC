
// the system's singleton class

DroneHub {

	var <>window, <middle;
	var <>speakers, <>drones, <>interpreter, <>states;
	var <>randomseed;
	//var <>recording;
	var <>midi, <fundamental, <midinoteoffset;
	var tester;
	var <screenbounds, <>channelOffset;
	var <pathtoscalafiles; // = "/Users/thm21/Library/Application Support/SuperCollider/scl/";
	var <pathtouserscalafiles; // = "/Users/thm21/Library/Application Support/SuperCollider/scl_user/";
	var <mode, <>post;
	var <>scale, <key;
	var <threnoscopeColor;
	var <>padDown;
	var <>appPath;
	var <>channels;

	*new { arg window, argmode, argscale, argfundamental, threnoscopeColor, key, channels, appPath;
		^super.new.initDroneHub(window, argmode, argscale, argfundamental, threnoscopeColor, key, channels, appPath);
	}
		
	initDroneHub { |argwindow, argmode, argscale, argfundamental, argThrenoscopeColor, argKey, argchannels, argappPath |
		window = argwindow;
		mode = argmode;
		midi = false;
		fundamental = argfundamental;
		midinoteoffset = fundamental.cpsmidi.mod(12); // the systems tonic
		middle = window.bounds.height/2;
		randomseed = 1000000.rand; // XXX is this used at all?
		thisThread.randSeed = randomseed; // XXX is this used at all?
		//recording = false;
		screenbounds = Window.screenBounds;
		scale = argscale;
		channelOffset = 0;
		threnoscopeColor = argThrenoscopeColor;
		padDown = false;
		key = argKey;
		appPath = argappPath;
		channels = argchannels;

		[\appPath, appPath].postln;
		// FOR STANDALONE BINARY ____________________
		//pathtoscalafiles = appPath++"/scl/";
		//pathtouserscalafiles = appPath++"/scl_user/";
		
		// FOR RUNNING Threnoscope in SC as classes_______________
		pathtoscalafiles = Platform.userAppSupportDir++"/threnoscope/scl/";
		pathtouserscalafiles = Platform.userAppSupportDir++"/threnoscope/scl_user/";
		
		
		"_______________".postln;
		[\pathtoscalafiles, pathtoscalafiles].postln;
		[\pathtouserscalafiles, pathtouserscalafiles].postln;

		switch(mode) 
			{\perform} { // regular perform mode with terminal and console
				post = true;
				}
			{\performWin} { // regular perform mode with terminal and console
				post = true;
				}
			{\dev} { // development mode
				post = true;
				}
			{\displayFS} { // full screen for display (for non-interactive performance)
				post = false;
				}
			{\displayWin} { // display in a window (for app playback)
				post = false;
			};
	}
	
	registerSpeakers { |argspeakers|
		speakers = argspeakers;
	}

	registerDrones { |argdrones|
		drones = argdrones;
	}

	registerInterpreter { |arginterpreter|
		interpreter = arginterpreter;
	}

	registerStates { |argstates|
		states = argstates;
	}
	
	
	getChordDict {
		^().put('major', [0, 4, 7]).put('minor', [0, 3, 7]).put('5', [0, 7]).put('dominant7th', [0, 4, 7, 10]).put('major7th', [0, 4, 7, 11]).put('minor7th', [0, 3, 7, 10]).put('minorMajor7th', [0, 3, 7, 11]).put('sus4', [0, 5, 7]).put('sus2', [0, 2, 7]).put('6', [0, 4, 7, 9]).put('minor6', [0, 3, 7, 9]).put('9', [0, 2, 4, 7, 10]).put('minor9', [0, 2, 3, 7, 10]).put('major9', [0, 2, 4, 7, 11]).put('minorMajor9', [0, 2, 3, 7, 11]).put('11', [0, 2, 4, 5, 7, 11]).put('minor11', [0, 2, 3, 5, 7, 10]).put('major11', [0, 2, 4, 5, 7, 11]).put('minorMajor11', [0, 2, 3, 5, 7, 11]).put('13', [0, 2, 4, 7, 9, 10]).put('minor13', [0, 2, 3, 7, 9, 10]).put('major13', [0, 2, 4, 7, 9, 11]).put('minorMajor13', [0, 2, 3, 7, 9, 11]).put('add9', [0, 2, 4, 7]).put('minorAdd9', [0, 2, 3, 7]).put('6add9', [0, 2, 4, 7, 9]).put('minor6add9', [0, 2, 3, 7, 9]).put('dominant7add11', [0, 4, 5, 7, 10]).put('major7add11', [0, 4, 5, 7, 11]).put('minor7add11', [0, 3, 5, 7, 10]).put('minorMajor7add11', [0, 3, 5, 7, 11]).put('dominant7add13', [0, 4, 7, 9, 10]).put('major7add13', [0, 4, 7, 9, 11]).put('minor7add13', [0, 3, 7, 9, 10]).put('minorMajor7thAdd13', [0, 3, 7, 9, 11]).put('7b5', [0, 4, 6, 10]).put('7s5', [0, 4, 8, 10]).put('7b9', [0, 1, 4, 7, 10]).put('7s9', [0, 3, 4, 7, 10]).put('7s5b9', [0, 1, 4, 8, 10]).put('m7b5', [0, 3, 6, 10]).put('m7s5', [0, 3, 8, 10]).put('m7b9', [0, 1, 3, 7, 10]).put('9s11', [0, 2, 4, 6, 7, 10]).put('9b13', [0, 2, 4, 7, 8, 10]).put('6sus4', [0, 5, 7, 9]).put('7sus4', [0, 5, 7, 10]).put('major7sus4', [0, 5, 7, 11]).put('9sus4', [0, 2, 5, 7, 10]).put('major9sus4', [0, 2, 5, 7, 11])
	}
	
	
	postDroneState {arg selectedName, selected;
		var string;
		Document.listener.string = ""; // clear post window
		string = "~"++selectedName++"\n"++
		"~"++selectedName++".type = \\"++drones.droneArray[selected].type++"\n"++
		"~"++selectedName++".tonic = "++drones.droneArray[selected].tonic++"\n"++
		"~"++selectedName++".freq = "++drones.droneArray[selected].freq++"\n"++
		"~"++selectedName++".harmonics = "++drones.droneArray[selected].harmonics++"\n"++
		"~"++selectedName++".amp = "++drones.droneArray[selected].amp++"\n"++
		"~"++selectedName++".speed = "++(drones.droneArray[selected].speed*1000)++"\n"++
		"~"++selectedName++".length = "++(drones.droneArray[selected].length*360/(2*pi))++"\n"++
		"~"++selectedName++".angle = "++(drones.droneArray[selected].angle*360/(2*pi))++"\n"++
		"~"++selectedName++".degree = "++drones.droneArray[selected].degree++"\n"++
		"~"++selectedName++".ratio = "++drones.droneArray[selected].ratio++"\n"++
		"~"++selectedName++".env = "++drones.droneArray[selected].env++"\n"++
		"~"++selectedName++".octave = "++drones.droneArray[selected].octave++"\n"
		"~"++selectedName++".note = "++drones.droneArray[selected].note++"\n";
		Document.listener.string = string; // add info
		if(post, { {interpreter.postview.string_(string)}.defer });
	}
}