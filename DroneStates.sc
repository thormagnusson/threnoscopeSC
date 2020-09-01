
// This class will be useful in installation context where machines
// can move between sets

DroneStates {
	
	var hub;
	var recScoreArray, scoreArray;
	var randomseed;
	var recording;
	var <storedGroupsDict;
	var <>codescore; // a GUI code score
	var speed, <>playing, clock, refclock, counter;
	//var starttime;
	
	*new { |hub|
		^super.new.initDroneStates( hub);
	}

	initDroneStates { |arghub| 
		hub = arghub;
		randomseed = hub.randomseed;
		// standalone
		// storedGroupsDict = Object.readArchive(hub.appPath++"/groups/groups.grp");
		// classes
		storedGroupsDict = Object.readArchive(Platform.userAppSupportDir++"/threnoscope/groups/groups.grp");
		if(storedGroupsDict.isNil, { storedGroupsDict = () });
	}
	
	// if you want to create a new state, use this method, as you might have been recording for a while
	startRecord {
		// record a performance to be played back in playScore
		// store thisThread.randSeed...
		"STARTED RECORDING".postln;
		recScoreArray = [];
		recording = true;
	}
	
	stopRecord {
		// stop recording
		"STOPPED RECORDING".postln;
		recording = false;
	}
	
	// XXX - this needs to be updated
	addToScore_ { |string| // method called from the interpreter
		if( ( 	string.contains("playScore") 	|| 
				string.contains("playSubScore") 	|| 
				string.contains("saveState") 	|| 
				string.contains("startRecord") 	|| 
				string.contains("saveScore") 	|| 
				string.contains("swapState") 	|| 
				string.contains("viewScope") 	|| 
				string.contains("playCodeScore")	|| 
				string.contains("states") 		||  // from DroneController
				string.contains("tunings") 		||  // from DroneController
				string.contains("scales") 		||  // from DroneController
				string.contains("killAll")
			).not, { // don't record the first playscore
			if(recording, {
				//this.preProcessor = { |code| code.postln;};
				"Adding this to score: ".post; string.postln;
				recScoreArray = recScoreArray.add([Main.elapsedTime, string]); // recording the performance
			});
		});
	}
	
	// NOTE: States have the same formats as scores, except they are atemporal (thus a one dimensional array)
	loadState { |name, recording| // one might want to record a loadstate
		var score;
		"LOADING STATE: ".post; name.postln;
		hub.drones.killAll; // remove all playing drones
		// if(recording.not, { this.startRecord() }); // clear scoreArray (dafault)
		#randomseed, score = Object.readArchive(hub.appPath++"/states/"++name++".drnST");
		thisThread.randSeed = randomseed;
		score.do({ arg event;
			// hub.interpreter.opInterpreter("> " ++ event); // was like this
			event[1].interpret;
		//	hub.interpreter.opInterpreter("> " ++ event[1]); // event[0] is time, but that's not used here
		});
	}

	addState { |name, recording=false| // one might want to record a loadstate
		var score;
		// standalone
		// #randomseed, score = Object.readArchive(hub.appPath++"/states/"++name++".drnST");
		// classes 
		#randomseed, score = Object.readArchive(Platform.userAppSupportDir++"/threnoscope/states/"++name++".drnST");
		thisThread.randSeed = randomseed;
		score.do({ arg event;
			event[1].interpret;
			// hub.interpreter.opInterpreter("> " ++ event); // was like this
			//hub.interpreter.opInterpreter("> " ++ event[1]); // event[0] is time, but that's not used here
		});
	}
	
	// human readable saveState
	saveState { | name |
		var file, offsettime, savedScoreArray, score;
		
		if(codescore.isNil, { // if there is no code score window open
			offsettime = recScoreArray[0][0];
			savedScoreArray = recScoreArray.copy.collect({arg event; [0, event[1]]}); // all events happening at 0 sec
		}, { // if code score window is open, we want to save that version of the score
			// savedScoreArray = codescore.score; // original version
			savedScoreArray = codescore.score.copy.collect({arg event; [0, event[1]]}); // all events happening at 0 sec
		});
		score = [randomseed, savedScoreArray];
		score = score.asCompileString;
		score = score.replace("],", "],\n").replace("[ [", "[\n ["); // Make the file human readable (one event per line)
		
		// standalone
		// file = File(hub.appPath++"/states/"++name++".drnST", "w");
		// classes 
		file = File(Platform.userAppSupportDir++"/threnoscope/states/"++name++".drnST", "w");
		file.write(score);
		file.close;	
	}
	
	swapState { |name, time=10|
		var score, olddrones;		
		"SWAPPING STATE: ".post; name.postln;
		// standalone
		// #randomseed, score = Object.readArchive(hub.appPath++"/states/"++name++".drnST");
		// classes
		#randomseed, score = Object.readArchive(Platform.userAppSupportDir++"/threnoscope/states/"++name++".drnST");
		thisThread.randSeed = randomseed;
		olddrones = hub.drones.droneDict.keys.deepCopy;
		// get rid of old drones
		{ olddrones.do({ |name|
				{hub.drones.createMachine(\amp, name, 0.2)}.defer; // XXX - todo: make a dedicated "swapmachine"
				(time/olddrones.size).wait;
				//drone.kill;
				//hub.drones.droneDict.at(name).kill;
				//{hub.interpreter.opInterpreter("> " ++ name.asString++".kill")}.defer;
				{name.asString++".kill"}.defer;

			}) }.fork(TempoClock.new);
		// clean the dictionaries:
	//	hub.drones.initDicts();
//		hub.drones.chordDict = ();
//		hub.drones.satelliteDict = ();
//		hub.drones.machineDict = ();
//		hub.drones.interDict = ();
//		// create the new drones
		{ score.do({ arg event;
			"STATES EVENT: ".post; event.postln;
				{hub.drones.createMachine(\neutral, time: 0.1)}.defer;
				//{hub.interpreter.opInterpreter("> " ++ event[1])}.defer;
				{event[1].interpret}.defer;
				(time/score.size).wait;
			}) }.fork(TempoClock.new);
	}
	
	saveScore { | name |
		var file, offsettime, savedScoreArray, score;
		"saving score : ".post; name.postln;
		
		if(codescore.isNil, { // if there is no code score window open
			offsettime = recScoreArray[0][0];
			savedScoreArray = recScoreArray.copy.collect({arg event; [event[0]-offsettime, event[1]]});
		}, { // if code score window is open, we want to save that version of the score
			savedScoreArray = codescore.score;
		});
		score = [randomseed, savedScoreArray];
		score = score.asCompileString;
		score = score.replace("],", "],\n").replace("[ [", "[\n ["); // Make the file human readable (one event per line)

		// standalone
		// file = File(hub.appPath++"/scores/"++name++".drnSC", "w");
		// classes
		file = File(Platform.userAppSupportDir++"/threnoscope/scores/"++name++".drnSC", "w");
		file.write(score);
		file.close;	
	}
		
	playScore { | name, aspeed=1 | // This is human coded score
		// var offsettime, lastoffsettime, score;
		var zerotimeindex, schedulemainclockat;
		
		//hub.post = false; // turn drone creation posting off
		//this.startRecord(); // or should it stop recording?
		speed = aspeed;
		//if(name.isNil.not, {
		// standalone
		// #randomseed, scoreArray = (hub.appPath++"/scores/"++name++".drnSC").load;
		// classes 
		#randomseed, scoreArray = (Platform.userAppSupportDir++"/threnoscope/scores/"++name++".drnSC").load;
		//});
		scoreArray = scoreArray.sort({arg a, b; a[0] <= b[0] }); // home-made sort algorithm as there are subarrays
		
		// the code below does the zero time events before clocks are scheduled
		zerotimeindex = 0;
		while({scoreArray[zerotimeindex][0] == 0}, {
			//hub.interpreter.opInterpreter("> " ++ scoreArray[zerotimeindex][1]);
	//		scoreArray[zerotimeindex][1].interpret; // when the score item was a string 
			scoreArray[zerotimeindex][1].value; // scores items are stored as functions
			zerotimeindex = zerotimeindex+1; 
		});
		// and then we find the next event
		schedulemainclockat = scoreArray[zerotimeindex][0];
		
		refclock = TempoClock.new.schedAbs( 0, { | time |
			[\clocktime, time].postln;
			//[\nextevent, scoreArray[scoreArray.collect({ |event| event[0] }).indexOfGreaterThan(refclock.nextTimeOnGrid)]].postln;
			//time.asInteger.asString.speak;
			// metro tick -> 
			// {SinOsc.ar(8000)*XLine.ar(0.25,0.001,0.11, doneAction:2)!2}.play;
			1;
		});

		counter = 0;
		this.createClock(schedulemainclockat, speed); // was 1.0
		
		if(codescore.isNil.not, {codescore.startTimeline( speed )});
		playing = true;
	}
		
	// NOTE: This create clock is slightly strange and is recalculated on every move
	// in the Code Score. If code was not moved in time, this would not be needed, but since
	// it is dynamic code score, the clocks need to be rescheduled.
	createClock { | schedtime, speed |
		var event, lastdur;
		// var nextindex, nexttime, thistime;
		
		clock.stop;
		// counter recalculated to know where in the drone score the clock is (using the refclock)
		[\counterA, counter].postln;
		counter = scoreArray.collect({ |event| event[0] }).indexOfGreaterThan(refclock.nextTimeOnGrid);
		//schedtime = schedtime + aschedtime;
		[\counterB, counter].postln;
		[\schedtime, schedtime].postln;
		"_______________________________ NEW CLOCK ____________________________________sched time :  ".post; schedtime.postln; 
		clock = TempoClock.new.schedAbs( schedtime, { | time |

			event = scoreArray[counter];
			lastdur = event[0];
			
			event[1].value;
			
			{if(hub.post, {
			//	hub.interpreter.postview.string_("time: "++ time ++ " : " ++ event[1].asCompileString)
				hub.interpreter.postview.string_(hub.interpreter.postview.string ++ "\ntime: "++ time ++ " : " ++ event[1].asCompileString)
			}) }.defer;
			
			counter = counter + 1;
			
			// need to defer the killing of clocks here, or else drones won't fade out
			if(scoreArray[counter].isNil, { {clock.stop; refclock.stop }.defer; nil }, { (scoreArray[counter][0]-lastdur) * speed.reciprocal});
			//nexttime-thistime;
		});	
	}


	// the reason for this method is to allow for a (variable) scoped score playing
	// the general method playScore is not scoped as it's interacted with from the graphical score
	// playSubScore is used in "playScore", i.e. you can play scores within scores.
	playSubScore { | name, aspeed=1 | // This is human coded score
		var speed, randomseed, scoreArray, counter, playtask, event, subinfoclock;

		speed = aspeed;
		// STANDALONE
		#randomseed, scoreArray = (hub.appPath++"/scores/"++name++".drnSC").load;
		// RUNNING TS IN SC AS CLASSES
		#randomseed, scoreArray = (Platform.userAppSupportDir++"/threnoscope/scores/"++name++".drnSC").load;

		scoreArray = scoreArray.sort({ | a, b | a[0] <= b[0] }); // home made sort algorithm as there are subarrays
		
		counter = 0;
		
		subinfoclock = TempoClock.new.schedAbs( 0, { | time |
				[\____________subscoretime, time].postln;
			1;
		});

		playtask = TempoClock.new.sched(0.0, { | time |
			event = scoreArray[counter];
			[\subScore_event_______NOW_RUNNING_, event ].postln;
			// hub.interpreter.opInterpreter("> " ++ event[1]); 
	//		event[1].interpret; // when score is a string
			event[1].value; // when score is a function
			counter = counter + 1;
			if(scoreArray[counter].isNil, { {playtask.stop; subinfoclock.stop}.defer; nil }, { scoreArray[counter][0]-event[0] });
		});
	}

	stopScore {
		clock.stop;
		refclock.stop;
		if(codescore.isNil.not, {codescore.stopTimeline });
		playing = false;	
		// hub.drones.killAll; // we might not want to kill all the drones
	}
	
	updateScore { | score | // from realtime manipulations of the graphical code score
		var nextevent;
		"-----------------------UPDATING SCORE".postln;
		scoreArray = score.sort({arg a, b; a[0] <= b[0] }); // home made sort algorithm as there are subarrays // WORKS
		nextevent = scoreArray[scoreArray.collect({ |event| event[0] }).indexOfGreaterThan(refclock.nextTimeOnGrid)][0]; // WORKS
		
		[\nextevent, nextevent].postln;
		[\clock_beats, refclock.beats].postln;
				
		this.createClock( nextevent-refclock.beats, speed ); // might have to absolute .beats (beats.ceil)?
		
		[\scoreArray________________________________].postln;
		Post << scoreArray;
		//scoreArray = scoreArray.collect({|event| event[0] = event[0]/speed });
		
	}
	
	showScore { | name, scale=30, speed=1 |
		// the code timeline
		
		if(name.isNil.not, { // then we use the playscore score
			// STANDALONE
			//#randomseed, scoreArray = (hub.appPath++"/scores/"++name++".drnSC").load;
			// TS AS CLASSES IN SC
			#randomseed, scoreArray = (Platform.userAppSupportDir++"/threnoscope/scores/"++name++".drnSC").load;
			// scoreArray = scoreArray.sort({arg a, b; a[0] <= b[0] }); // home made sort algorithm as there are subarrays
			scoreArray = scoreArray.collect({|event| event[0] = event[0]/speed });
		});
		
		Post << scoreArray;
		codescore = DroneCodeScore.new( hub, scale );
		codescore.parseScore( scoreArray );
		hub.interpreter.codescore(true);
		^codescore;
	}

	removeScore {
		codescore.remove;
		codescore = nil;
		hub.interpreter.codescore(false);
	}
	
	playMIDI { | filepath |
		var file, tracks;
		file = SimpleMIDIFile.read(filepath);
		file.timeMode = \seconds;
		tracks = 0;
		file.noteEvents.do({ |event| if(event[0]>tracks, {tracks = event[0]}) }); // I make my own tracks counter
		tracks.do({arg i;
			var movementarray;
			var delta = 0;
			hub.drones.createDrone(\saw, 2, amp:0.3, name:("midi_"++i).asSymbol);			
			movementarray = [];
			file.noteEvents.do({arg event;
				var time, realtime;
				time = event[1];
				realtime = time - delta;
				//[\time, time, \realtime, realtime].postln;
				if(event[0] == (i+1), {
					if((event[5] == 0) || (event[2] == \noteOff), {
						//movementarray = movementarray.add([realtime, \off, event[4].midicps, 0]); // time, on/off, freq, amp
					},{
						movementarray = movementarray.add([realtime, \on, event[4], event[5]/127]); // time, on/off, freq, amp
						delta = time;
					});
				});
				// delta = time;
			});
			hub.drones.droneArray.last.startMIDI(("midi_"++i).asSymbol, movementarray);
		})
	}
	
	states {
		var states, statestring;
		statestring = "";
		// standalone
		// states = hub.appPath++"/states/*.drnST".pathMatch;
		// classes
		states = Platform.userAppSupportDir++"/threnoscope/states/*.drnST".pathMatch;
		states.do({arg path; statestring = statestring ++ " | " ++ path.basename.splitext[0] });
		statestring = statestring ++ " | ";
		^statestring
	}

	scores {
		var scores, scorestring;
		scorestring = "";
		// standalone
		// scores = hub.appPath++"scores/*.drnSC".pathMatch;
		// classes
		scores = Platform.userAppSupportDir++"/threnoscope/scores/*.drnSC".pathMatch;
		scores.do({arg path; scorestring = scorestring ++ " | " ++ path.basename.splitext[0] });
		scorestring = scorestring ++ " | ";
		^scorestring
	}
	
	chords {
		var chords, chordsstring;
		chordsstring = "";
		chords = hub.getChordDict.keys.asArray;
		chords.do({ |chord| chordsstring = chordsstring ++ " | " ++ chord.asString });
		chordsstring = chordsstring ++ " | ";
		{if(hub.post, {
			hub.interpreter.postview.string_(chordsstring)
		})}.defer;
		^chordsstring
	}
	
	scales {
		var scales, scalesstring;
		scalesstring = "";
		scales = ScaleInfo.scales.keys.asArray;
		scales.do({ |scale| scalesstring = scalesstring ++ " | " ++ scale.asString });
		scalesstring = scalesstring ++ " | ";
		{if(hub.post, {hub.interpreter.postview.string_(scalesstring)})}.defer;
		^scalesstring
	}
	
	post {
	 	Post << scoreArray;	
	}
	
	storeGroup { | name, argsDict |
		storedGroupsDict.add(name.asSymbol -> argsDict);
		// standalone
		storedGroupsDict.writeArchive(hub.appPath++"/groups/groups.grp");
		// classes
		storedGroupsDict.writeArchive(Platform.userAppSupportDir++"/threnoscope/groups/groups.grp");
	
	}	
}

