
DroneController {

	var deathArray, osc; // experimental
	var <drawView, drawTask, <scp, dronegui, speakercircles, codescore;
	var <>droneArray, <>machineArray; // arrays are used for drawing
	var <droneDict, <chordDict, <satellitesDict, <machineDict, <interDict, <groupDict;
	var <>hub, window, screendimension;
	var drone, <>selected, <>selectedName, interDroneArray, <nameArray;
	var speakers, <tuning, <scale, <fundamental;
	var interpreter, states, interpret, oldy;
	var <globalenv;
	var drawFunc, mouseDownAct, mouseMoveAct;
	var dronecount, leftoffset, speakercirlesview, midipadNames, midiout;
// var testwin;

	*new { arg hub, tuning, scale, fundamental;
		^super.new.initDroneController(hub, tuning, scale, fundamental);
	}

	initDroneController { |arghub, argtuning, argscale, argfundamental|
		hub = arghub;
		window = hub.window;
		interpreter = hub.interpreter;
		states = hub.states;
		dronecount = 0;
		droneArray = [];

		deathArray = [];
		machineArray = [];
		droneDict = ();
		chordDict = ();
		satellitesDict = ();
		groupDict = ();
		machineDict = ();
		interDict = (); // storing interpreted clusters
		interDroneArray = [];
		nameArray = [];
	//	this.initDicts();
		speakers = hub.speakers;
		selected = 0;
		selectedName = "o_o";
		globalenv = [3,3];
		tuning = argtuning;
		scale = argscale;
		fundamental = argfundamental;
		interpret = false;
		midipadNames = {nil} ! 8; // the AKAI pad for exposingMIDI to drones
		screendimension = Window.screenBounds.height;
		leftoffset = if(hub.mode == \displayFS, {(Window.screenBounds.width-screendimension)/2}, {0});
		//Pen.font = Font( "Helvetica-Bold", 14 );
		thisThread.randSeed = hub.randomseed;

		speakercircles = speakers.drawImg;
		[\speakercircles, speakercircles].postln;
	//	speakercircles.plot;
	//	speakercircles.dump;

		mouseDownAct = { |me, x, y, mod|
				var seldrone, string;
				var cursorDistCenter = sqrt(([0, 0] - [(hub.middle - x).abs, (hub.middle - y).abs]).squared.sum);
				var cursorTheta = (Point((hub.middle - x),(hub.middle - y)).theta+pi);
				oldy = y; // the offset for calculating mouse move rotation
				droneArray.do({ |drone, i| drone.selected = false }); // deselect all drones
				droneArray.do({ |drone, i|
					var zeroCrossing = false;
					var loc = drone.getDroneLook;
					if(loc[2]+loc[3] > (2*pi), { // a drone crossing the zero line
						if(
						(cursorDistCenter>loc[0]) &&
						(cursorDistCenter<loc[1]) &&
						(( cursorTheta > loc[2] ) || ( cursorTheta < ((loc[2] + loc[3]) % (2*pi)))), {
							selected = i;
							drone.selected = true;
							selectedName = drone.name.asString;
							seldrone = drone;
						});
					}, {
					if(
						(cursorDistCenter>loc[0]) &&
						(cursorDistCenter<loc[1]) &&
						( cursorTheta > loc[2] ) &&
						( cursorTheta < (loc[2] + loc[3])), {
							selected = i;
							drone.selected = true;
							selectedName = drone.name.asString;
							seldrone = drone;
						});
					});
				});
//				[\selectedspeed, seldrone.speed].postln;
//				[\selectedName, selectedName].postln;


				if(hub.post && seldrone.isNil.not, {
					hub.postDroneState(selectedName, selected);

//					Document.listener.string = ""; // clear post window
//					string = "~"++selectedName++"\n"++
//						"~"++selectedName++".type = \\"++seldrone.type++"\n"++
//					"~"++selectedName++".harmonics = "++seldrone.harmonics++"\n"++
//					"~"++selectedName++".amp = "++seldrone.amp++"\n"++
//					"~"++selectedName++".degree = "++seldrone.degree++"\n"++
//					"~"++selectedName++".ratio = "++seldrone.ratio++"\n"++
//					"~"++selectedName++".env = "++seldrone.env++"\n"++
//					"~"++selectedName++".octave = "++seldrone.octave++"\n";
//					Document.listener.string = string; // add info
//					hub.interpreter.postview.string_(string);
				});

				if(codescore.isNil.not, {
					//"in here________________".postln;
					codescore.removeTextScore; // remove the textual score view
				});
			};

		mouseMoveAct = { |me, x, y, mod|
				var change = (oldy-y)/300;
				droneArray[selected].relRotation_(change);
				oldy = y;
			};


		drawFunc = {
				//speakers.draw;

				droneArray.do({arg drone; drone.update; drone.draw.value});

				deathArray.do({arg drone; drone.update; drone.draw.value}); // drones scheduled for death ... fading out
				// machineArray.do({arg machine; machine.update; }); // machines have their own update
				machineArray.do({arg machine; machine.draw.value }); // machines have their own update
	 			//try{ Pen.stringAtPoint(droneArray[selected].name.asString, Point(20, screendimension-100)) }; // if there is no drone
				Color.black.alpha_(0.5).set;
	// 			try{ Pen.stringAtPoint("ooo", Point(20, 100)) }; // if there is no drone
	 			try{ Pen.stringAtPoint(selectedName, Point(20, screendimension-100)) }; // if there is no drone
	 			//try{ Pen.stringAtPoint("RIUFIF", Point(20, screendimension-130)) }; // if there is no drone
			};

//		window.view.backgroundImage = speakercircles; // WSLib

		speakercirlesview = UserView.new(window, Rect(leftoffset, 0, screendimension, screendimension)).backgroundImage_(speakercircles);

		// the circles drawn into a bitmap once
//		UserView.new(window, Rect(0, 0, screendimension, screendimension)).drawFunc_({ speakers.drawImg });

		// the drone wedges drawn repeatedly
		drawView = UserView.new(window, Rect(leftoffset, 0, screendimension, screendimension))
			.mouseDownAction_( mouseDownAct )
			.mouseMoveAction_( mouseMoveAct )
			.keyDownAction_({arg view, char, modifiers, unicode, keycode;
				//[char, modifiers, unicode, keycode].postln;
				switch(keycode)
				{126}{ // up arrow
					if(modifiers == 11010368, {
						var name, collectivename, string;
						selected = (selected-1).clip(0, droneArray.size-1);
						name = droneArray[selected].name.asString;
						droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							if(name.contains("chd_"), {
								chordDict.keys.do({ |key|
									if(key.asString == collectivename, {
										chordDict[key].selected = true;
									});
								});
							});
							if(name.contains("sat_"), {
								satellitesDict.keys.do({ |key|
									if(key.asString == collectivename, {
										satellitesDict[key].selected = true;
									});
								});
								//satellitesDict[name[0..6].asSymbol].selected = true;
							});
							if(name.contains("grp_"), {
								//var grpname = key.asString[0..key.asString.findAll("_")[1]-1];
								groupDict.keys.do({ |key|
									if(key.asString == collectivename, {
										groupDict[key].selected = true;
									});
								});
							});
							selectedName = collectivename; // only the name of the group
							block{ arg break;
								droneArray.do({ arg drone, i;
									if(drone.name.asString.contains("_"), {
										if((drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename), {
											selected = i;
											break.value();
										});
									});
								});											};
						}, {
							selectedName = droneArray[selected].name.asString;
							droneArray[selected].selected = true;
						});

						if(hub.post, {

							hub.postDroneState(selectedName, selected);

//							Document.listener.string = ""; // clear post window
//							string = "~"++selectedName++"\n"++
//								"~"++selectedName++".type = \\"++droneArray[selected].type++"\n"++
//							"~"++selectedName++".harmonics = "++droneArray[selected].harmonics++"\n"++
//							"~"++selectedName++".amp = "++droneArray[selected].amp++"\n"++
//							"~"++selectedName++".degree = "++droneArray[selected].degree++"\n"++
//							"~"++selectedName++".ratio = "++droneArray[selected].ratio++"\n"++
//							"~"++selectedName++".env = "++droneArray[selected].env++"\n"++
//							"~"++selectedName++".octave = "++droneArray[selected].octave++"\n";
//
//							Document.listener.string = string; // add info
//
//							hub.interpreter.postview.string_(string);

						});

					}, { // increase amplitude
						if(selectedName.contains("chd_"), {
							chordDict.keys.do({ |key|
								if(key.asString == selectedName, {
									chordDict[key].amp_((chordDict[key].amp+0.01).clip(0, 2));
								});
							});
						}, {
							if(selectedName.contains("sat_"), {
								satellitesDict.keys.do({ |key|
									if(key.asString == selectedName, {
										satellitesDict[key].amp_((satellitesDict[key].amp+0.01).clip(0, 2));
									});
								});
							}, {
								if(selectedName.contains("grp_"), {
									groupDict.keys.do({ |key|
										if(key.asString == selectedName, {
											groupDict[key].amp_((groupDict[key].amp+0.01).clip(0, 2));
										});
									});
								}, {
									droneArray[selected].set(\amp, (droneArray[selected].amp + 0.01).clip(0, 2));
								});
							});
						});
					});
					"up".postln;


				}
				{125}{
					if(modifiers == 11010368, {
						var name, collectivename, string;
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = (selected+1).clip(0, droneArray.size-1);
						name = droneArray[selected].name.asString;
						droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							if(name.contains("chd_"), {
								chordDict.keys.do({ |key|
									if(key.asString == collectivename, {
										chordDict[key].selected = true;
									});
								});
							});
							if(name.contains("sat_"), {
								satellitesDict.keys.do({ |key|
									if(key.asString == collectivename, {
										satellitesDict[key].selected = true;
									});
								});
							});
							if(name.contains("grp_"), {
								groupDict.keys.do({ |key|
									if(key.asString == collectivename, {
										groupDict[key].selected = true;
									});
								});
							});
							selectedName = collectivename; // only the name of the group/sat/chord
							block{ arg break;
							droneArray[selected .. droneArray.size].do({ arg drone, i;
								oldfoundFlag = foundFlag;
								if(drone.name.asString.contains("_"), {
									if(drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename, {
										foundFlag = true;
									},{
										foundFlag = false;
									});
								}, {
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
								    selected = (selected+i-1).clip(0, droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							selectedName = droneArray[selected].name.asString;
							droneArray[selected].selected = true;
						});
						if(hub.post, {
							hub.postDroneState(selectedName, selected);

//							Document.listener.string = ""; // clear post window
//							string = "~"++selectedName++"\n"++
//								"~"++selectedName++".type = \\"++droneArray[selected].type++"\n"++
//							"~"++selectedName++".harmonics = "++droneArray[selected].harmonics++"\n"++
//							"~"++selectedName++".amp = "++droneArray[selected].amp++"\n"++
//							"~"++selectedName++".degree = "++droneArray[selected].degree++"\n"++
//							"~"++selectedName++".ratio = "++droneArray[selected].ratio++"\n"++
//							"~"++selectedName++".env = "++droneArray[selected].env++"\n"++
//							"~"++selectedName++".octave = "++droneArray[selected].octave++"\n";
//							Document.listener.string = string; // add info
//							hub.interpreter.postview.string_(string);
						});

					}, {
						// decrease amplitude
						if(selectedName.contains("chd_"), {
							chordDict.keys.do({ |key|
								if(key.asString == selectedName, {
									chordDict[key].amp_((chordDict[key].amp-0.01).clip(0, 2));
								});
							});
						}, {
							if(selectedName.contains("sat_"), {
								satellitesDict.keys.do({ |key|
									if(key.asString == selectedName, {
										satellitesDict[key].amp_((satellitesDict[key].amp-0.01).clip(0, 2));
									});
								});
							}, {
								if(selectedName.contains("grp_"), {
									groupDict.keys.do({ |key|
										if(key.asString == selectedName, {
											groupDict[key].amp_((groupDict[key].amp-0.01).clip(0, 2));
										});
									});
								}, {
									droneArray[selected].set(\amp, (droneArray[selected].amp - 0.01).clip(0, 1));
								});
							});
						});
					});
					"down".postln;
				}
				{123}{
					// move drone around (fine movement if alt mod is down)
					droneArray[selected].oppositemove = true;
					if(modifiers == 11010368, {
						droneArray[selected].rotation_(droneArray[selected].rotation-0.001);
					},{
						droneArray[selected].rotation_(droneArray[selected].rotation-0.01);
					});
					"left".postln;
				}
				{124}{
					// move drone around (fine movement if alt mod is down)
					droneArray[selected].oppositemove = true;
					if(modifiers == 11010368, {
						droneArray[selected].rotation_(droneArray[selected].rotation+0.001);
					},{
						droneArray[selected].rotation_(droneArray[selected].rotation+0.01);
					});
					"right".postln;
				}
				{51}{ // delete button
					"delete drone/chord/satellite/group".postln;
					//if(droneArray.size > 0, {
						this.killDrone(); // only kill a unique drone when using delete button (not chord or sat)
					//});
					[\selectedName, selectedName].postln;

// the below would need the same treatment as amp up and down above, but I decided not to kill drone with delete button
//					if(selectedName.contains("chd_"), {
//						chordDict.keys.do({ |key|
//							[\key, key.asString].postln;
//							if(key.asString == selectedName, {
//								chordDict[key].perform(".kill");
//								chordDict.removeAt(key);
//							});
//						});
//					}, {
//						if(selectedName.contains("sat_"), {
//							satellitesDict.keys.do({ |key|
//								if(key.asString == selectedName, {
//									satellitesDict[key].perform(".kill");
//									satellitesDict.removeAt(key);
//								});
//							});
//						}, {
//							if(selectedName.contains("grp_"), {
//								groupDict.keys.do({ |key|
//									if(key.asString == selectedName, {
//										groupDict[key].perform(".kill");
//										groupDict.removeAt(key);
//									});
//								});
//							}, {
//								this.killDrone();
//							});
//						});
//					});

				}
				{36}{"enter - NOT USED ATM ! ".postln;};
				// play the drone in the chosen scale using numerical keys from 1 to 9
				if((unicode > 48) && (unicode < 58), { // using unicode - keycode is not in right numerical order
					var harm = false;
					if(modifiers == 8388864, { harm = true });
					if(selectedName.contains("chd_"), {
						chordDict.keys.do({ |key|
							if(key.asString == selectedName, {
								chordDict[key].degree_(unicode-48);
							});
						});
					}, {
						if(selectedName.contains("sat_"), {
							satellitesDict.keys.do({ |key|
								if(key.asString == selectedName, {
									satellitesDict[key].degree_(unicode-48);
								});
							});
						}, {
							if(selectedName.contains("grp_"), {
								groupDict.keys.do({ |key|
									if(key.asString == selectedName, {
										groupDict[key].degree_(unicode-48);
									});
								});
							}, {
								droneArray[selected].degree_(unicode-48);
							});
						});
					});
				});
			})
			.drawFunc_( drawFunc );
			this.start();
	}

	initDicts { // not used ATM // XXX
		droneDict = ();
		chordDict = ();
		satellitesDict = ();
		groupDict = ();
		machineDict = ();
		interDict = ();
		interDroneArray = [];
		nameArray = [];
	}

	start {
		hub.states.startRecord();
		drawTask = Task({
			inf.do({ |i|
	//	if(droneArray.size>0, {droneArray[selected].getSynths.postln});  // XXX DEBUGGING CODE
//				{testwin.isClosed.not.if({ testwin.refresh })}.defer;

				{window.isClosed.not.if({ window.refresh })}.defer;
				0.05.wait;
			});
		}).play;
	}

	stop { // this should probably stop the sound as well
		 drawTask.stop;
	}

	interpret { |function, name|
		interpret = true;
		interDroneArray = [];
		function.value; // evaluate the function
		interpret = false;
		[\interpretNAME, name].postln;
		if(interDroneArray.size > 0, {
			name = name ?? {this.generateName("inter")};
			interDict[name] = interDroneArray; // for accessing individual drones (not indexed in an array but a dict)
			[\interDict, interDict].postln;
		});
		^("New interpretation created : " ++ name);
	}

	eval { |function|
		function.value; // evaluate the function
		^("SuperCollider code evaluated");
	}

	scope_ { | bool=true | // needs internal server
		if(bool, {
			"creating scope".postln;
			if(Server.default != Server.internal, {"Internal server needed for the scope".postln});
			switch(hub.speakers.nrChannels)
				{ 1 }{ Synth.tail(RootNode(Server.default), \dronemixer2, [\out, 100]) }
				{ 2 }{ Synth.tail(RootNode(Server.default), \dronemixer2, [\out, 100]) }
				{ 4 }{ Synth.tail(RootNode(Server.default), \dronemixer4, [\out, 100]) }
				{ 5 }{ Synth.tail(RootNode(Server.default), \dronemixer5, [\out, 100]) }
				{ 8 }{ Synth.tail(RootNode(Server.default), \dronemixer8, [\out, 100]) };

			scp = PlusFreqScope.new(window, window.bounds);
			// scp = PlusFreqScope.new(SCWindow.new.front, Rect(0, 0, 400, 800));
			scp.scope.background_(Color.clear);
			scp.scope.waveColors_([Color.black]);
			scp.inBus_(100); // make a synth that mixes to there
			scp.active_(true);
			scp.xZoom_(0.55);
			scp.yZoom_(0.3);
		}, {
			"______________________ >>>         killing scope".postln;
			scp.kill;
			scp.remove;
		});
	}

	env_ {arg envt; // supporting passing in of both array [0, 1] (attack and release) and just an integer (for both A&R)
		"Setting ENV".postln;
		if(envt.isArray, { globalenv = envt }, {globalenv = [envt, envt]});
	}

	pushEnv_ {arg envt; // same as above, but pushing this env into all active Drones
		if(envt.isArray, { globalenv = envt }, {globalenv = [envt, envt]});
		droneArray.do({arg drone; drone.env_(globalenv)});
	}

	env {
		^("Env time is : " ++ globalenv);
	}


	createDrone { | type=\saw, tonic=1, harmonics=3, amp=0.3, speed, length, angle, degree=1, ratio=1, name, env, octave, note |
		var envelopearr, string;
//		Document.listener.string_("");
//		hub.interpreter.postview.string_("");
		if(type==\args, {^("type, tonic, harmonics, amp, speed, length, angle, degree, ratio, name, env, octave, note") });
//		if((type.isKindOf(Symbol) && tonic.isKindOf(SimpleNumber) && harmonics.isKindOf(SimpleNumber) && amp.isKindOf(SimpleNumber) && (speed.isKindOf(SimpleNumber) || speed.isKindOf(Nil) ) && (length.isKindOf(SimpleNumber) || length.isKindOf(Nil)) && (angle.isKindOf(SimpleNumber) || angle.isKindOf(Nil)) && degree.isKindOf(SimpleNumber) && ratio.isKindOf(SimpleNumber) && (name.isKindOf(Symbol) || name.isKindOf(Nil)) && (env.isKindOf(SimpleNumber) || env.isKindOf(Array) || env.isKindOf(Nil)) && (octave.isKindOf(SimpleNumber) || octave.isKindOf(Nil)) && post.isKindOf(Boolean)).not, {^"--->   No drone created. Wrong argument types"});

		dronecount = dronecount + 1;
		name = this.generateName(name:name); // if name is nil, a new name is generated, if name, we check if it's used already
		speed = speed ? 1.0.rand2; // get variations in speed
		angle = angle ? 360.rand;
		length = length ? (200+(160.rand));
		if(env.isNil, {
			envelopearr = globalenv;
		}, {
			if(env.isArray, { envelopearr = env }, { envelopearr = [env, env] });
		});
		drone = Drone.new(hub, tuning, scale, fundamental)
					.createDrone(
						type,
						tonic,
						harmonics,
						amp,
						speed, //1.rand2,
						length,
						angle,
						degree,
						ratio,
						envelopearr,
						octave,
						note
					).name_(name);
		droneArray = droneArray.add(drone); // an array that takes care of drawing
		droneDict[drone.name] = drone; // for accessing individual drones (not indexed in an array but a dict)
		currentEnvironment.put(drone.name, drone); // this allows me to access drones through ~
		if(interpret, { interDroneArray = interDroneArray.add(drone) });
		//interDroneArray = interDroneArray.add(drone);
		selectedName = drone.name.asString; // draw the latest drone name in the bottom left corner
		if(drawTask.isPlaying.not, { drawTask.play }); // if cmd + period, then start the drawtask again
		if(hub.post, {
//			hub.postDroneState(drone.name, droneArray.size-1);
			"------- New Drone Created: ".postln;
			string = "~"++name++"\n"++
			"~"++name++".type = "++drone.type++"\n"++
			"~"++name++".tonic = "++drone.tonic++"\n"++
			"~"++name++".freq = "++drone.freq++"\n"++
			"~"++name++".harmonics = "++drone.harmonics++"\n"++
			"~"++name++".amp = "++drone.amp++"\n"++
			"~"++name++".speed = "++speed++"\n"++
			"~"++name++".length = "++length++"\n"++
			"~"++name++".angle = "++angle++"\n"++
			"~"++name++".degree = "++drone.degree++"\n"++
			"~"++name++".ratio = "++drone.ratio++"\n"++
			"~"++name++".env = "++drone.env++"\n"++
			"~"++name++".octave = "++drone.octave++"\n" ++
			"~"++name++".note = "++drone.note++"\n";
			//string.postln;
			{hub.interpreter.postview.string_(string)}.defer;
		});
		^drone;
	}

// working original: 1)
//	killDrone { | name, releasetime |
//		"KILLING DRONE (DroneController class): ".post; name.postln;
//		// name = name ? droneArray[selected].name; // either the drone passed in (from Drone) or the selected one
//		name = name ? if(droneArray.size>0, {droneArray[selected].name}); // either the drone passed in (from Drone) or the selected one
//		droneArray.copy.do({arg drone, i;
//			if(drone.name == name, {
//				releasetime = releasetime ? drone.env[1]; // if no arg, use drone env
//				droneArray[i].killDrone(releasetime);
//				droneArray.removeAt(i);
//			});
//		});
//		selected = (selected - 1).clip(0, droneArray.size-1);
//		droneDict.removeAt(name);
//		nameArray.removeAll(name);
//	}


	killDrone { | name, releasetime |
		"KILLING DRONE (DroneController class): ".post; name.postln;
		// name = name ? droneArray[selected].name; // either the drone passed in (from Drone) or the selected one
		// this method is split into two times:
		// (slightly complex as the user might create a drone with the same name before the former drone is dead)
		{
			// Time 1) - release the synths and start the fading out of the drone
			[\selected, selected].postln;
			[\droneArray, droneArray].postln;
//			[\selectedname, droneArray[selected].name].postln;

			name = name ? if(droneArray.size>0, {droneArray[selected].name}); // either the drone passed in (from Drone) or the selected one

			droneArray.copy.do({arg drone, i;
				if(drone.name == name, {
					if(drone.dying == false, { // if it's alive
						releasetime = releasetime ? drone.env[1]; // if no arg, use drone env
						droneArray[i].killSynths(releasetime); // specific method created for the deathArray fadeout
						drone.dying = true;

						deathArray = deathArray.add(drone);
						droneArray.removeAt(i);
						droneDict.removeAt(name);
						nameArray.removeAll(name);

//						if(drone.selected, {
//							//drone.selected = false; // unselect this drone
//				//			selected = (selected - 1).max(0);
//				//			selected = (selected - 1)%(droneArray.size);
// 							selected = ((selected - 1)%dronecount).clip(0, droneArray.size-2);
//							dronecount = dronecount - 1;
//							[\dronecount, dronecount].postln;
//							[\selected, selected].postln;
//							droneArray[selected].selected = true; // select the drone below
//							selectedName = droneArray[selected].name; /// XXX - test
//						});
					});
				});
			});
			if(droneArray.size>0, {
 				selected = ((selected - 1)%dronecount).clip(0, droneArray.size-1);

 				dronecount = (dronecount - 1).max(0);
				[\dronecount, dronecount].postln;
				[\selected, selected].postln;
				droneArray[selected].selected = true; // select the drone below
				selectedName = droneArray[selected].name; /// XXX - test
			});


//			droneArray[selected].selected = false;
//			selected = (selected - 1).clip(0, droneArray.size-1);
//			droneArray[selected].selected = true;
			// Time 2) - kill the drone and remove completely it from arrays and dicts
			{
				deathArray.copy.do({arg drone, i;
					if((drone.name == name) && drone.dying, {
						deathArray.removeAt(i);
//						droneArray.removeAt(i);
					});
				});
//				droneDict.removeAt(name);
//				nameArray.removeAll(name);
			}.defer(releasetime);
		}.value;
	}

	// themes
	createChord { | type=\saw, chord=#[1,5,8], tonic=1, harmonics, amp=0.2, speed, length, angle, degree=1, ratio=1, env, octave, name, note |
		var thischord, shortname, string;
		//hub.interpreter.postview.string_("");
		if(type==\args, {^("type, chord, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave, name, note") });
//		if((type.isKindOf(Symbol) && (chord.isKindOf(Symbol) || chord.isKindOf(Array)) && tonic.isKindOf(SimpleNumber) && (harmonics.isKindOf(SimpleNumber) || harmonics.isKindOf(Nil) ) && amp.isKindOf(SimpleNumber) && (speed.isKindOf(SimpleNumber) || speed.isKindOf(Nil) ) && (length.isKindOf(SimpleNumber) || length.isKindOf(Nil)) && (angle.isKindOf(SimpleNumber) || angle.isKindOf(Nil)) && degree.isKindOf(SimpleNumber) && ratio.isKindOf(SimpleNumber) && (name.isKindOf(Symbol) || name.isKindOf(Nil)) && (env.isKindOf(SimpleNumber) || env.isKindOf(Array) || env.isKindOf(Nil)) && (octave.isKindOf(SimpleNumber) || octave.isKindOf(Nil)) ).not, {^"--->   No chord created. Wrong argument types"});
		shortname = name; // get the user generated name for access through ~ global vars
		name = this.generateName("chd", name); // create a unique system name which can't be duplicated
		thischord = DroneChord.new(hub, name, scale).createChord(type, chord, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave, note);
	//	thischord.tuning_(tuning);
		chordDict[name] = thischord;
		currentEnvironment.put(name, thischord); // this allows me to access drones through ~
		if(shortname.isNil, {shortname = name});
		currentEnvironment.put(shortname, thischord); // this allows me to access drones through ~
		selectedName = name.asString; // draw the latest drone name in the bottom left corner

		if(hub.post, {
			"------- New Chord Created: ".postln;
			string = 	"~"++shortname++"\n"++
			"~"++shortname++".harmonics = "++thischord.dronearray[0].harmonics++"\n"++
			"~"++shortname++".amp = "++amp++"\n"++
			"~"++shortname++".speed = "++speed++"\n"++
			"~"++shortname++".length = "++length++"\n"++
			"~"++shortname++".angle = "++angle++"\n"++
			"~"++shortname++".degree = "++degree++"\n"++
			"~"++shortname++".ratio = "++ratio++"\n"++
			"~"++shortname++".env = "++thischord.dronearray[0].env++"\n"++
			"~"++shortname++".octave = "++thischord.dronearray[0].octave++"\n" ++
			"~"++shortname++".note = "++note++"\n";
			string.postln;
			hub.interpreter.postview.string_(string);
		});




//		("~"++shortname).postln;
//		("~"++shortname++".harmonics = "++thischord.dronearray[0].harmonics).postln;
//		("~"++shortname++".amp = "++amp).postln;
//		("~"++shortname++".speed = "++speed).postln;
//		("~"++shortname++".length = "++length).postln;
//		("~"++shortname++".angle = "++angle).postln;
//		("~"++shortname++".degree = "++degree).postln;
//		("~"++shortname++".ratio = "++ratio).postln;
//		("~"++shortname++".env = "++thischord.dronearray[0].env).postln;
//		("~"++shortname++".octave = "++thischord.dronearray[0].octave).postln;
//		"\n".postln;
//		{hub.interpreter.textview.string = "\n New drone created : ~" ++ drone.name++ "\n"}.defer;
//		{hub.interpreter.postview.string = "\n New drone created : ~" ++ drone.name++ "\n"}.defer;
//		{hub.interpreter.textview.string = hub.interpreter.textview.string ++ "\n~" ++ thischord.name}.defer;
		^("New chord created : " ++ name);
	}

	defineChord { | ...args |
		var name, tempname, dronearr, thischord;
		args = args.flatten;
		tempname = args.removeAt(0); // the first item is the name
		name = this.generateName("chd", tempname);
		dronearr = droneArray.reject({|item| args.includes(item.name).not });
		thischord = DroneChord.new(hub, name).defineChord( dronearr );
		//thischord.tuning_(tuning);
		chordDict[name] = thischord;
		currentEnvironment.put(name, thischord); // this allows me to access drones through ~
		currentEnvironment.put(tempname, thischord); // this allows me to access drones through ~
		^("New chord defined : " ++ name);
	}

	killChord { |name| // called from DroneChord:kill
		chordDict.removeAt(name);
		nameArray.removeAll(name);
	}

	killChords { // all chords
		chordDict.do({arg chord; chord.dronearray.do({arg drone; drone.kill }) });
		chordDict = ();
	}

	createSatellites { | type=\sine, ratios=#[1,2,3,4,5,6,7,8], tonic=2, harmonics, amp=0.2, speed, length, angle, num=10, spread=1, env, octave, name |
		var thissatellites, shortname, string;
		//{hub.interpreter.postview.string_("")}.defer; // needs to be deferred as it's called from .playScore
		if(type==\args, {^("type, ratios, tonic, harmonics, amp, speed, length, angle, num, spread, env, octave, name") });
		shortname = name; // the short name created by the user
		name = this.generateName("sat", name); // long system name - both can be used
		thissatellites = DroneSatellites.new(hub, name).createSatellites(type, ratios, tonic, harmonics, amp, speed, length, angle, num, spread, env, octave);
	//	thissatellites.tuning_(tuning);
		satellitesDict[name] = thissatellites;
		currentEnvironment.put(name, thissatellites); // this allows me to access drones through ~
	//	if(shortname.isNil, {shortname = name}); // xxx
		if(shortname.isNil, {shortname = name.asString[4..6] }); // if the user didn't pass a name, it was generated and we're only interested in the pure name
		[\shortname, shortname].postln;
		currentEnvironment.put(shortname.asSymbol, thissatellites); // this allows me to access drones through ~
		selectedName = name.asString; // draw the latest drone name in the bottom left corner

		if(hub.post, {
			"------- New Satellites Created: ".postln;
			string = 	"~"++shortname++"\n"++
			"~"++shortname++".harmonics = "++thissatellites.dronearray[0].harmonics++"\n"++
			"~"++shortname++".amp = "++amp++"\n"++
			"~"++shortname++".speed = "++speed++"\n"++
			"~"++shortname++".length = "++length++"\n"++
			"~"++shortname++".angle = "++angle++"\n"++
			"~"++shortname++".env = "++thissatellites.dronearray[0].env++"\n"++
			"~"++shortname++".octave = "++thissatellites.dronearray[0].octave++"\n";
			string.postln;
			hub.interpreter.postview.string_(string);
		});

		^("Satellites created : " ++ name);
	}

	killSatellite { |name| // called from Satellite:kill
		satellitesDict.removeAt(name);
		nameArray.removeAll(name);
	}

	killSatellites {
		satellitesDict.do({arg satellites; satellites.dronearray.do({arg drone; drone.kill }) });
		satellitesDict = ();
	}

	// create Group needs some work !
	createGroup { | name, type, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave | // create a new group
		var thisgroup, groupname, refname, postname, string;
		if(type==\args, {^("name, type, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave") });
	//	hub.interpreter.postview.string_("");
		groupname = name; // the name as stored in the dict
		//name = name.asString;
		//name = this.generateName("grp", name[4 .. (name.size-1)]); // the new name (new groups will add to the name)
		[\name1, name].postln;
		name = this.generateName("grp");
		name = name++"_"++groupname; // special case, as I want to be able to run many groups e.g. \grp_oso_loki (where loki is the group template)
		[\name2, name].postln;
		thisgroup = DroneGroup.new(hub, name).createGroup( groupname, type, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave );
	//	thisgroup.tuning_(tuning);
		groupDict[name.asSymbol] = thisgroup;
		currentEnvironment.put(name.asSymbol, thisgroup); // this allows me to access drones through ~
		currentEnvironment.put((name[name.findAll("_")[0]+1..name.findAll("_")[1]-1]).asSymbol, thisgroup); // this allows me to access drones through ~
		selectedName = name.asString; // draw the latest drone name in the bottom left corner

		if(hub.post, {
			"------- New Group Created, of type:".post; (groupname).postln;
			postname = name[name.findAll("_")[0]+1..name.findAll("_")[1]-1];
			string = "~"++postname++"\n"++
			"~"++postname++".harmonics = "++thisgroup.dronearray[0].harmonics++"\n"++
			"~"++postname++".amp = "++amp++"\n"++
			"~"++postname++".speed = "++speed++"\n"++
			"~"++postname++".length = "++length++"\n"++
			"~"++postname++".angle = "++angle++"\n"++
			"~"++postname++".degree = "++degree++"\n"++
			"~"++postname++".ratio = "++ratio++"\n"++
			"~"++postname++".env = "++thisgroup.dronearray[0].env++"\n"++
			"~"++postname++".octave = "++thisgroup.dronearray[0].octave++"\n";
			string.postln;
			hub.interpreter.postview.string_(string)
		});
		^("New group created : " ++ name);
	}

	defineGroup { | ...args |
		var thisgroup, name, tempname;
		args = args.flatten;
		tempname = args.removeAt(0); // the first item is the name
		name = this.generateName("grp");
		thisgroup = DroneGroup.new(hub, tempname).defineGroup( args );
		name = name++"_"++tempname; // special case, as I want to be able to run many groups e.g. \grp_oso_loki (where loki is the group template)
		groupDict[name.asSymbol] = thisgroup;
		currentEnvironment.put(name.asSymbol, thisgroup); // this allows me to access drones through ~
		currentEnvironment.put((name[name.findAll("_")[0]+1..name.findAll("_")[1]-1]).asSymbol, thisgroup); // this allows me to access drones through ~
		^("New group defined : " ++ name);
	}

	killGroup { |name| // called from Satellite:kill
		groupDict.removeAt(name);
		nameArray.removeAll(name);
	}

	killGroups {
		groupDict.do({arg groups; groups.do({arg drone; drone.kill }) });
		groupDict = ();
	}

	groups { // avalable stored groups (stored in a file on disk)
		^("Available groups: ".post; states.storedGroupsDict.keys.asArray);
	}

	createMachine { | type=\amp, target=\all, time=4, rate=1, transtime=5, range=1, name|
		var machine;
		//hub.interpreter.postview.string_("");
		if(type==\args, {^("type, target, time, rate, transtime, range, name") });
		name = name ?? {this.generateName("mach")};
		machine = DroneMachine.new(hub, type, target, time, rate, transtime, range).name_(name);
		machineArray = machineArray.add(machine); // an array that takes care of drawing
		machineDict[name] = machine;
		currentEnvironment.put(name, machine); // this allows me to access drones through ~
		^("New machine created : " ++ machine);
	}

	killMachine { |name|
		"killing machine !!! : ".post; name.postln;
		[\machineArray1, machineArray].postln;
		machineArray.copy.do({arg machine, i;
			if(machine.name == name, {
				"same name".postln;
				machineArray.removeAt(i);
			});
		});
		[\machineArray2, machineArray].postln;
		machineDict.removeAt(name);
		nameArray.removeAll(name);
	}

	killMachines {
		// machineArray.do({arg machine; machine.kill() });
		machineArray = [];
		//machineDict.do({arg machines; machines.do({arg machine; machine.kill }) });
		machineDict = ();
	}

	killAll { | releasetime |
//		var rt;
//		rt = if(releasetime.isNil, { globalenv[1] }, { releasetime })
		droneArray.copy.do({arg drone; this.killDrone( drone.name, releasetime ) });
		machineArray.do({arg machine; machine.kill });
		// I should not need the below as all drones remove themselves from arrays and dicts
//		droneArray = [];
//		droneDict = ();
//		chordDict = ();
//		groupDict = ();
//		satellitesDict = ();
//		machineDict = ();
//		nameArray = [];
	}

	freeSynths { | releasetime |
		droneArray.do({arg drone; drone.freeSynths( releasetime ) });
	}

	// not finished yet
	undo {
		var cmd = hub.interpreter.lastCommand;

		// HMMM !
		[\lastcommand, hub.interpreter.lastCommand].postln;
		if(cmd.contains("createDrone"), { hub.drones.droneArray.last.kill });
		if(cmd.contains("tonic"), { "not ready yet".postln; }); // XXX - where to store the previous state?

	}

	rename { | oldname, newname |
		var drone;
		drone = droneDict.removeAt(oldname);
		drone.name = newname;
		droneDict[newname] = drone;
		^("New drone name : "++newname);
	}

	names {
		var dronenames;
		dronenames = "";
		droneDict.keys.do({ arg item; dronenames = dronenames ++ " | " ++ item });
		dronenames = dronenames ++ " | ";
		^dronenames;
	}

	select { |argname|
		droneArray.do({arg drone; drone.selected = false });
		droneDict[argname].selected = true;
		droneArray.do({arg drone, i; drone.postln; if(drone.selected, { selected = i }) }); // global var
		^("Drone selected : "++ argname);
	}

	deselect { |argname|
		droneArray.do({arg drone; drone.selected = false });
		^("Deselecting all drones");
	}

	tuning_ { |argtuning, dur|
		var temptuning;
		tuning = argtuning;
		temptuning = Tuning.newFromKey(tuning.asSymbol); 
		if(temptuning.isNil, { temptuning = DroneScale.new(tuning) });
		if(temptuning.isNil.not, { 
			droneArray.do({arg drone; drone.tuning_(tuning, dur) });
			if(speakers.drawscale, {
				speakercircles = speakers.drawImg(tuning);
				speakercirlesview.backgroundImage = speakercircles;
			}, {
				"This tuning does not exist".postln;
			});
		});
		^("Global tuning set to: " ++ argtuning);
	}

	scale_ { |argscale|
		scale = argscale;
		hub.scale = scale;
		droneArray.do({arg drone; drone.scale_(argscale) });
		speakers.setScale_(scale);
		speakercircles = speakers.drawImg(tuning);
		speakercirlesview.backgroundImage = speakercircles;
		^( "Global scale set to: " ++ argscale );
	}

	tunings {
		var tuningstring = "\n| ";
		TuningInfo.tunings.keys.asArray.sort.do({arg tuning; tuningstring = tuningstring ++ tuning ++ " | " });
		tuningstring = tuningstring ++ "\n";
		{if(hub.post, {hub.interpreter.postview.string_(tuningstring)})}.defer;
		^tuningstring;
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
		
	relativeAmp_ {|change=0, dur=2| // change amp relative to current amp of each drone
		droneArray.do({arg drone; drone.relativeAmp_(change, dur) });
		^("All drones -> amp altered by : " ++ (change*100) ++ " %");
	}

	relativeSpeed_ {|change=0| // change speed relative to current amp of each drone
		droneArray.do({arg drone; drone.relativeSpeed_(change) });
		^("All drones -> speed altered by : " ++ (change*100) ++ " %");
	}

	relativeLength_ {|change=0| // change length relative to current amp of each drone
		droneArray.do({arg drone; drone.relativeLength_(change) });
		^("All drones -> length altered by : " ++ (change*100) ++ " %");
	}

	set { | ...args | //  all commands to the synth that do NOT change the graphic drone on the GUI
		droneArray.do({arg drone; drone.set(*args) });
		^("All drones -> args set to : " ++ args);
	}

	
	getChordDict {
		^().put('major', [0, 4, 7]).put('minor', [0, 3, 7]).put('5', [0, 7]).put('dominant7th', [0, 4, 7, 10]).put('major7th', [0, 4, 7, 11]).put('minor7th', [0, 3, 7, 10]).put('minorMajor7th', [0, 3, 7, 11]).put('sus4', [0, 5, 7]).put('sus2', [0, 2, 7]).put('6', [0, 4, 7, 9]).put('minor6', [0, 3, 7, 9]).put('9', [0, 2, 4, 7, 10]).put('minor9', [0, 2, 3, 7, 10]).put('major9', [0, 2, 4, 7, 11]).put('minorMajor9', [0, 2, 3, 7, 11]).put('11', [0, 2, 4, 5, 7, 11]).put('minor11', [0, 2, 3, 5, 7, 10]).put('major11', [0, 2, 4, 5, 7, 11]).put('minorMajor11', [0, 2, 3, 5, 7, 11]).put('13', [0, 2, 4, 7, 9, 10]).put('minor13', [0, 2, 3, 7, 9, 10]).put('major13', [0, 2, 4, 7, 9, 11]).put('minorMajor13', [0, 2, 3, 7, 9, 11]).put('add9', [0, 2, 4, 7]).put('minorAdd9', [0, 2, 3, 7]).put('6add9', [0, 2, 4, 7, 9]).put('minor6add9', [0, 2, 3, 7, 9]).put('dominant7add11', [0, 4, 5, 7, 10]).put('major7add11', [0, 4, 5, 7, 11]).put('minor7add11', [0, 3, 5, 7, 10]).put('minorMajor7add11', [0, 3, 5, 7, 11]).put('dominant7add13', [0, 4, 7, 9, 10]).put('major7add13', [0, 4, 7, 9, 11]).put('minor7add13', [0, 3, 7, 9, 10]).put('minorMajor7thAdd13', [0, 3, 7, 9, 11]).put('7b5', [0, 4, 6, 10]).put('7s5', [0, 4, 8, 10]).put('7b9', [0, 1, 4, 7, 10]).put('7s9', [0, 3, 4, 7, 10]).put('7s5b9', [0, 1, 4, 8, 10]).put('m7b5', [0, 3, 6, 10]).put('m7s5', [0, 3, 8, 10]).put('m7b9', [0, 1, 3, 7, 10]).put('9s11', [0, 2, 4, 6, 7, 10]).put('9b13', [0, 2, 4, 7, 8, 10]).put('6sus4', [0, 5, 7, 9]).put('7sus4', [0, 5, 7, 10]).put('major7sus4', [0, 5, 7, 11]).put('9sus4', [0, 2, 5, 7, 10]).put('major9sus4', [0, 2, 5, 7, 11])
	}
	
	
	addInterpreter { |arginterpreter|
		interpreter = arginterpreter;
	}

	drawSpeakers_ { |bool|
		// speakers.drawspeakers = bool;
		speakers.speakers_(bool.asBoolean);
		speakercircles = speakers.drawImg;
		speakercirlesview.backgroundImage = speakercircles;
	}

	drawHarmonics_ { |bool|
		// speakers.drawharmonics = bool;
		{
			speakers.harmonics_(bool.asBoolean);
			speakercircles = speakers.drawImg;
			speakercirlesview.backgroundImage = speakercircles;
		}.defer; // in a deferred process in case it's called from a score
	}

	drawScale_ { |bool|
		// speakers.drawharmonics = bool;
		{
		speakers.scale_(bool.asBoolean);
		speakercircles = speakers.drawImg( tuning );
		speakercirlesview.backgroundImage = speakercircles;
		}.defer; // in a deferred process in case it's called from a score
	}

	drawOctaves_ { |bool|
		// speakers.drawharmonics = bool;
		{
		speakers.octaves_(bool.asBoolean);
		speakercircles = speakers.drawImg;
		speakercirlesview.backgroundImage = speakercircles;
		}.defer; // in a deferred process in case it's called from a score
	}

	reDraw {
		{
		speakers.drawImg;
		speakercircles = speakers.drawImg;
		speakercirlesview.backgroundImage = speakercircles;
		}.defer; // in a deferred process in case it's called from a score
	}

	gui_ { | bool |
		if( bool, {
			dronegui = DroneGUI.new(hub);
		}, {
			dronegui.remove;
		});
		hub.interpreter.gui(bool);
	}

	getSpeakerInfo {
		"speakers: ".post; speakers.drawspeakers.postln;
		"harmonics: ".post; speakers.drawharmonics.postln;	}

	state { // post the state of the dronecircle
		var string;
		if(hub.post, {
			//Document.listener.string = ""; // xxx - might remove
			string = "\ntuning : " ++ tuning.asString ++
			"\nscale : " ++ scale.asString ++
			"\nchords : " ++ chordDict.keys.asArray.asString ++
			"\nsatellites : " ++ satellitesDict.keys.asArray.asString ++
			"\ngroups : " ++ groupDict.keys.asArray.asString ++
			"\nmachines : " ++ machineDict.keys.asArray.asString ++
			"\ninterpreted : " ++ interDict.keys.asArray.asString ++
			"\ndrones : " ++ droneDict.keys.asArray.asString ++
			"\n";

			hub.interpreter.postview.string_(string);
		});
	}

	methods { // post what methods the dronecircle takes
		var string;
		if(hub.post, {
			//Document.listener.string = ""; // xxx - might remove
			string = "\nrename(oldname, newname)" ++
			"\ncreateDrone(type, tonic, harmonics, amp, speed, length, angle, ratio)" ++
			"\ncreateChord(type, chord(array), tonic, harmonics, amp, speed, length, angle)" ++
			"\ncreateSatellites(type, num, tonic, harmonics, amp, speed, length, angle, ratio, scale)" ++
			"\ntuning_ : set tuning (better done on a drones level)" ++
			"\nscale_ : set the scale (better done on a drones level)" ++
			"\nrelativeAmp : set relative amplitude (-0.1 will lower all drones by 10%)" ++
			"\ntunings : print the list of tunings" ++
			"\nscales : print the list of scales" ++
			"\nchords : print the list of chords" ++
			"\ndrawharmonics_ : draw the harmonic circles on GUI" ++
			"\ndrawspeakers_ : draw the speaker locations on GUI" ++
			"\nscope_ : draw spectra on GUI" ++
			"\nset : arguments to all drone synths" ++
			"\n";
			hub.interpreter.postview.string_(string);
		});
	}

	generateName { | type, name |
		var wovels, consonants;
		wovels = ["a", "e", "i", "o", "u", "y"];
		consonants = ["b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "z", "x", "y"];

		if(name.isNil, {
			name = "";
			3.do({ if(0.4.coin, { name = name++wovels.choose },{ name = name++consonants.choose }) });
		});

		// type is either a chord, satellites or a group
		name = if(type.isNil, {name}, {type++"_"++name}).asSymbol;

		if(nameArray.includesEqual(name), {
			"Generating a new name".postln;
			name = this.generateName(type); // recursion - try another name
		}, {
			nameArray = nameArray.add(name);
		});

		^name.asSymbol;
	}

	// DroneStates methods

	startRecord {
		states.startRecord();
	}

	stopRecord {
		states.stopRecord();
	}

	loadState { |name, recording=false|
		states.loadState(name, recording);
	}

	addState { |name, recording=false|
		states.addState(name, recording);
	}

	saveState { |name|
		states.saveState(name);
	}

	swapState { |name, time=10|
		states.swapState(name, time);
	}

	saveScore { |name|
		"saving score in DC : ".post; name.postln;
		states.saveScore(name);
	}

	playScore { |name, speed=1|
		states.playScore(name, speed);
	}

	playSubScore { |name, speed=1|
		states.playSubScore(name, speed);
	}

	stopScore {
		states.stopScore;
	}

	showScore { | name, scale=5, speed=1 |
		// states.showScore(name, scale, speed);
		codescore = states.showScore(name, scale, speed);
	}

	removeScore {
		states.removeScore;
		codescore = nil;
	}

	synchronic {
		^states.states;
	}

	diachronic {
		^states.scores;
	}

	states {
		^states.states;
	}

	scores {
		^states.scores;
	}

	// play a MIDI file
	playMIDI { | filepath |
		states.playMIDI(filepath);
	}

	// stop the playback of a MIDI file
	stopMIDI {
		droneArray.copy.do({ | drone, i |
			if(drone.name.asString.contains("midi"), {
				this.killDrone(drone.name);
			});
		});
	}

	// allow for the creation of drones on key press on a MIDI Keyboard
	// different from Drone:addMIDI (which listens to MIDI for a particular drone)
	addMIDI { | type=\saw, harmonics=4, inC=false, env=#[0,0] |
		var midiDroneArray, offset;
		midiDroneArray = Array.fill(127, { nil });
		// by default the system is in A - subtract 1 off the offset as base ratio in Threnoscope is 1 (and I'm minusing below)
		offset = (fundamental.cpsmidi%12)-1;
		if(hub.midi == false, {
			MIDIClient.init(2, 2);
			MIDIIn.connectAll;
			hub.midi = true;
		});
		MIDIdef.noteOn(\globalMIDIon, {arg ...args;
			var octave, ratio, amp, drone;
			args.postln;
//			if(inC, { // in case the user likes to think in C
//				"IN C".postln;
//				octave = (args[1]).div(12)+1;
//				ratio = (args[1]).mod(12)+1;
//			}, {
//				"NOT IN C".postln;
//				octave = (args[1]-offset).div(12)+1; // minusing offset here (see above)
//				ratio = (args[1]-offset).mod(12)+1;
//			});
			if(inC, { // in case the user likes to think in C
				"IN C".postln;
//				octave = (args[1]).div(12)+1;
//				ratio = (args[1]).mod(12)+1;
				ratio = args[1]-11;
			}, {
				"NOT IN C".postln;
//				octave = (args[1]-offset).div(12)+1; // minusing offset here (see above)
//				ratio = (args[1]-offset).mod(12)+1;
				ratio = args[1]-offset+1;
			});
			//ratio = args[1]-11;
			amp = args[0]/127;
		//	[\octave, octave, \ratio, ratio].postln;
			drone = this.createDrone(type, 1, harmonics, amp, 0, 300, rrand(210, 270), 1, ratio, env:env);
			if(midiDroneArray[args[1]].isNil, {
				midiDroneArray[args[1]] = drone;
			});
		});
		MIDIdef.noteOff(\globalMIDIoff, {arg ...args;
			midiDroneArray[args[1]].kill;
			midiDroneArray[args[1]] = nil;
		});
	}

	removeMIDI {
		MIDIdef(\globalMIDIon).free;
		MIDIdef(\globalMIDIoff).free;
	}

	// this method accepts two modes. drone mode is default, but synthargs allow for control that work on the synth through .set()
	exposeMIDI { |pad, name, synthargs|
		var lpd8index; // The MIDI controller index in the MIDI controller list
		//var offset;
	//	var offsetFlag = false;
//		var change;
//		var offset = {nil} ! 8 ;
//		var offsetFlag = {false}!8;

		// make sure MIDI is running
		if(hub.midi == false, {
			MIDIClient.init(4, 4);
			MIDIIn.connectAll;
			MIDIClient.sources.do({arg src, i; if(src.name=="LPD8", {lpd8index=i})});
			[\lpd8index, lpd8index].postln;
			MIDIOut.connect(0, MIDIClient.sources.at(0));
			MIDIOut.connect(1, MIDIClient.sources.at(1));
			MIDIOut.connect(2, MIDIClient.sources.at(2));
			hub.midi = true;
			midiout = MIDIOut(lpd8index);
		 });

		if(pad.isNil && name.isNil, { // no drone nor pad are specified - a global assignment of first 8 drones
			droneArray[0..7].do({arg drone, i;
				midipadNames[i] = drone.name;
				droneArray[i].exposeMIDI; // set up the drone-specific midi listener for the knobs
			});
		}, {
			 // select the right drone (either the one selected or the one dedicated by 'name')
			if(name.isNil.not, {
				droneArray.do({arg drone, i; droneArray[i].selected = false }); // deselect all
				droneArray.do({arg drone, i;
					if(drone.name == name, {
						droneArray[i].selected = true;
						selected = i;
					});
				});
			}, { // no name is passed, so the selected drone will get the pad
				name = droneArray[selected].name;
			});
			midipadNames[pad] = name;
			[\midipadNames, midipadNames].postln;
			droneArray[selected].exposeMIDI(synthargs); // set up the drone-specific midi listener for the knobs
		});

		MIDIdef.noteOn(\expnoteOn, { arg val, num, chan, src; �
			var nme;
			hub.padDown = true;
			[val, num, chan, src].postln;
//			{midiout.noteOn(0, num, 1)}.defer(0.1);

			{8.do({arg i; midiout.noteOff(0, 36+i)})}.defer(0.1); // the AKAI controller pad-lights

			"NOTE ON".postln;�
			nme = midipadNames[num-36];
			[\selectedNAME, nme].postln;
			if(nme.isNil.not, {
				droneArray.do({arg drone, i; droneArray[i].selected = false }); // deselect all
				droneArray.do({arg drone, i;
					if(drone.name == nme, {
						droneArray[i].selected = true;
						selected = i;
						//if(hub.post, {
							[\nme, nme, \selected, selected].postln;
							{hub.postDroneState(nme.asSymbol, selected)}.defer;
						//});
					});
				});
			});
		});

		MIDIdef.noteOff(\expnoteOff, { arg val, num, chan, src;
			hub.padDown = false;
			[val, num, chan, src].postln;
			{midiout.noteOn(0, num, 1)}.defer(0.1);
			// {midiout.noteOff(0, num, 0)}.defer(0.1);
		});

		// set the AKAI knobs
//		MIDIdef.cc(\AKAIknobs, { arg val, num, chan, src;
//			"control".postln;
//			[val, num, chan, src].postln;
//			num = num.min(7);
//			if(offsetFlag[num] == false, { offset[num] = val; offsetFlag[num] = true; });
//			//if(selected, {
//				// num is the parameter (the knob)
//				// val is the value
//
//				if(hub.padDown, { // change value offset (reset knob)
//					offset[num] = val;
//					[\offset, offset[num]].postln;
//				}, {
//					switch(num)
//						{1}{ // freq
//							"val is: ".post; val.postln;
//							if(val > offset[num], {change = 1},{change = -1});
//							droneArray[selected].freq = droneArray[selected].freq + change;
//							offset[num] = val;
//
//	//						if(synthargs[num].isNil, {
//	//							this.freq = this.freq + (val-offset);
//	//						},{
//	//							this.set(synthargs[num], 33); // need linlin here?
//	//						});
//							}
//						{2}{ // harmonics
//							"val is: ".post; val.postln;
//							if(val > offset[num], {change = 0.1},{change = -0.1});
//							droneArray[selected].harmonics = droneArray[selected].harmonics + change;
//							offset[num] = val;
//
//							}
//						{3}{ // amp
//							"val is: ".post; val.postln;
//							if(val > offset[num], {change = 0.01},{change = -0.01});
//							droneArray[selected].amp = droneArray[selected].amp + change;
//							offset[num] = val;
//							}
//						{4}{
//							"val is: ".post; val.postln;
//							}
//						{5}{ // freq high res
//							"val is: ".post; val.postln;
//							if(val > offset[num], {change = 0.1},{change = -0.1});
//							droneArray[selected].freq = droneArray[selected].freq + change;
//							offset[num] = val;
//							}
//						{6}{
//							}
//						{7}{
//							"val is: ".post; val.postln;
//							}
//						{8}{
//							"val is: ".post; val.postln;
//							};
//				});
//			//});
//		});

		// set the AKAI pads receivers - Jump between drones
//		MIDIdef.program(\program, { arg prog, chan, src;
//			var nme;
//			"program".postln;�
//			nme = midipadNames[prog];
//			[src,chan,prog].postln;
//			[\selectedNAME, nme].postln;
//			if(nme.isNil.not, {
//				droneArray.do({arg drone, i; droneArray[i].selected = false }); // deselect all
//				droneArray.do({arg drone, i;
//					if(drone.name == nme, {
//						droneArray[i].selected = true;
//						selected = i;
//					});
//				});
//			});
//		});


//
//		// set the AKAI knobs
//		MIDIdef.control(\control, { arg val, num, chan, src;
//			"control".postln;
//			[val, num, chan, src].postln;
//			if(offsetFlag == false, { offset = val; offsetFlag = true; });
//			if(selected, {
//				// num is the parameter (the knob)
//				// val is the value
//				switch(num)
//					{1}{ // freq
//						"val is: ".post; val.postln;
//						if(synthArguments[num].isNil, {
//							droneArray[selected].freq = droneArray[selected].freq + (val-offset);
//						},{
//							droneArray[selected].set(synthArguments[num], 33); // need linlin here?
//						});
//						}
//					{2}{ //
//						"val is: ".post; val.postln;
//							droneArray[selected].harmonics = droneArray[selected].harmonics + ((val-offset)/10);
//						}
//					{3}{
//						"val is: ".post; val.postln;
//						}
//					{4}{
//						"val is: ".post; val.postln;
//						}
//					{5}{ // freq high res
//						"val is: ".post; val.postln;
//						}
//					{6}{
//						"val is: ".post; val.postln;
//						}
//					{7}{
//						"val is: ".post; val.postln;
//						}
//					{8}{
//						"val is: ".post; val.postln;
//						};
//			});
//		});

	}

	// -> User interaction (used for automation of drones)

	// this method is also in Drone, but here for general control
	auto_ { | bool | // stop automation of all drones
		droneArray.do({arg drone; drone.auto_( bool ) });
	}

	// this method is also in Drone, but here for general control
	clearAuto { | bool | // clear automation of all drones
		droneArray.do({arg drone; drone.clearauto });
	}

	// the GUI control
	rec_ { | drone, method, min, max, rnd=0 | // non-user method called from Drone
		var movementarray = [];
		var offsetTime, scaled;
		var currentparamval, currentparamline;
		var smoothx;

		if(drone.isArray, { // coming from Chord, Group or Satellites
				"______________DRONE IS an ARRAY".postln;
			try{
				currentparamval = drone[0].perform(method); // simply getting at the instance variable
			}{
				"______________here".postln;
				currentparamval = drone[0].synthParams.at(method);			};
		}, {
				"______________DRONE IS a DRONE".postln;
			try{
				currentparamval = drone.perform(method); // simply getting at the instance variable
			}{
				currentparamval = drone.synthParams.at(method);			};
		});

		currentparamline = currentparamval.linlin(min, max, 0, screendimension).round(1)+0.5;
		smoothx = currentparamline;
		scaled = smoothx.min(screendimension).linlin( 0, screendimension, min, max ).round(rnd);

		drawView.drawFunc_({ // Swap out the draw function temporarily -> draw a line with the current value
			droneArray.do({arg drone; drone.update; drone.draw.value});
			deathArray.do({arg drone; drone.update; drone.draw.value});
			machineArray.do({arg machine; machine.draw.value });
			Pen.strokeColor = Color.black;
			Pen.color = Color.black;
 			Pen.line(Point(smoothx, 0), Point(smoothx, screendimension));
			Pen.addRect(Rect(smoothx-5, (screendimension/2)-5, 10, 10));
			Pen.stringAtPoint(scaled.asString, Point(smoothx+15, (screendimension/2)+15));
			Pen.stroke;

 			try{ Pen.stringAtPoint(droneArray[selected].name.asString, Point(20, screendimension-100)) };
		});

		drawView.mouseDownAction_({ | view, x |
			offsetTime = Main.elapsedTime;
			scaled = x.min(screendimension).linlin( 0, screendimension, min, max ).round(rnd);
			movementarray = movementarray.add( [ 0, scaled ] );
		});

		drawView.mouseMoveAction_({ | view, x |
//			smoothx = if( x>smoothx , {
//				smoothx = smoothx+((smoothx-x).abs/8)
//			},{
//				smoothx = smoothx-((smoothx-x).abs/8)
//			});
			smoothx = x;
			scaled = smoothx.min(screendimension).linlin( 0, screendimension, min, max ).round(rnd);
			movementarray = movementarray.add( [Main.elapsedTime - offsetTime, scaled ] );
			offsetTime = Main.elapsedTime;
			if(drone.isArray, { // coming from Chord, Group or Satellites
				drone.do({ |drone| drone.perform((method++"_").asSymbol, scaled); }); // update the user action in real-time
			}, {
				drone.perform((method++"_").asSymbol, scaled); // update the user action in real-time
			});
		});

		drawView.mouseUpAction_({
			if(drone.isArray, { // coming from Chord, Group or Satellites
				drone.do({ |drone| drone.startAuto( method, movementarray); }); // update the user action in real-time
			}, {
				drone.startAuto( method, movementarray );
			});
			drawView.mouseDownAction_( mouseDownAct );
			drawView.mouseMoveAction_( mouseMoveAct );
			drawView.mouseUpAction_(nil);
			drawView.drawFunc_( drawFunc );
		});
	}

	setParam_ { | drone, method, min, max, rnd=0 | // non-user method called from Drone
		var currentparamval, currentparamline;
		var scaled, smoothx;

		if(drone.isArray, { // coming from Chord, Group or Satellites
				"______________DRONE IS ARRAY".postln;
			try{
				currentparamval = drone[0].perform(method); // simply getting at the instance variable
			}{
				"______________here".postln;
				currentparamval = drone[0].synthParams.at(method);			};
		}, {
				"______________DRONE IS a DRONE".postln;
			try{
				currentparamval = drone.perform(method); // simply getting at the instance variable
			}{
				currentparamval = drone.synthParams.at(method);			};
		});

		currentparamline = currentparamval.linlin(min, max, 0, screendimension).round(1)+0.5;
		smoothx = currentparamline;
		scaled = smoothx.min(screendimension).linlin( 0, screendimension, min, max ).round(rnd);

		drawView.drawFunc_({ // Swap out the draw function temporarily -> draw a line with the current value
			droneArray.do({arg drone; drone.update; drone.draw.value});
			deathArray.do({arg drone; drone.update; drone.draw.value});
			machineArray.do({arg machine; machine.draw.value });
			Color(0, 0.3, 0).alpha_(0.7).set;

			Pen.color = Color.black;
 			Pen.line(Point(smoothx, 0), Point(smoothx, screendimension));
			Pen.addRect(Rect(smoothx-5, (screendimension/2)-5, 10, 10));
			Pen.stringAtPoint(scaled.asString, Point(smoothx+15, (screendimension/2)+15));
			Pen.stroke;
 			try{ Pen.stringAtPoint(droneArray[selected].name.asString, Point(20, screendimension-100)) };
		});

		drawView.mouseMoveAction_({ | view, x |
//			smoothx = if( x>smoothx , {
//				smoothx = smoothx+((smoothx-x).abs/8)
//			},{
//				smoothx = smoothx-((smoothx-x).abs/8)
//			});
			smoothx = x;
			// scaled = x.min(screendimension).linlin( 0, screendimension, min, max );
			scaled = smoothx.min(screendimension).linlin( 0, screendimension, min, max ).round(rnd);
			if(drone.isArray, { // coming from Chord, Group or Satellites
				drone.do({ |drone| drone.perform((method++"_").asSymbol, scaled); }); // update the user action in real-time
			}, {
				drone.perform((method++"_").asSymbol, scaled); // update the user action in real-time
			});
		});

		drawView.mouseUpAction_({
			drawView.mouseDownAction_( mouseDownAct );
			drawView.mouseMoveAction_( mouseMoveAct );
			drawView.mouseUpAction_(nil);
			drawView.drawFunc_( drawFunc );
		});
	}

	connectOSC { arg ip, port;
		"creating an OSC agent".postln;
		osc = NetAddr(ip, port);
	}

	sendOSC { arg address='/threnoscope', message;
		osc.sendMsg(address, message);
	}

	channelEA { arg shift=0, channelOffset=20; // Electroacoustic channel setup (set ServerOptions:numOutbutBusChannels to 16)
/*
speaker channels:
	6
0		1

4		5

2		3
	7
*/

		hub.channelOffset = channelOffset;
		Synth(\channelrouter8_2, [\in, channelOffset+0, \out, shift+4], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+1, \out, shift+0], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+2, \out, shift+6], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+3, \out, shift+1], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+4, \out, shift+5], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+5, \out, shift+3], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+6, \out, shift+7], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+7, \out, shift+2], 1, \addToTail );
	}

	channelLeeds { arg shift=0, channelOffset=20; // Leeds channel setup (set ServerOptions:numOutbutBusChannels to 16)
/*
speaker channels:
	0 	1

2			3

4			5

	6	7

	1
0		3

2		5

4		7
	6


*/

		hub.channelOffset = channelOffset;
		Synth(\channelrouter8_2, [\in, channelOffset+0, \out, shift+2], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+1, \out, shift+0], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+2, \out, shift+1], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+3, \out, shift+3], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+4, \out, shift+5], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+5, \out, shift+7], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+6, \out, shift+6], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+7, \out, shift+4], 1, \addToTail );
	}



	channelShift { arg shift=2, channelOffset=20; // special method for MOTU soundcards
		hub.channelOffset = channelOffset;
		Synth(\channelrouter8_2, [\in, channelOffset+0, \out, shift+0], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+1, \out, shift+1], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+2, \out, shift+2], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+3, \out, shift+3], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+4, \out, shift+4], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+5, \out, shift+5], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+6, \out, shift+6], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+7, \out, shift+7], 1, \addToTail );
	}

	// mix an 8 channel piece to stereo
	mixEightToStereo { arg channelOffset=20;
		hub.channelOffset = channelOffset;
		// left
		Synth(\channelrouter8_2, [\in, channelOffset+0, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+1, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+6, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+7, \out, 0], 1, \addToTail );
		// right
		Synth(\channelrouter8_2, [\in, channelOffset+2, \out, 1], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+3, \out, 1], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+4, \out, 1], 1, \addToTail );
		Synth(\channelrouter8_2, [\in, channelOffset+5, \out, 1], 1, \addToTail );

	}

	mixEightToMono { arg in=0, channelOffset=20; // this might become loud with long (in degrees) drones
		hub.channelOffset = channelOffset;
		Synth(\channelrouter8_1, [\in, channelOffset+0, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+1, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+6, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+7, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+2, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+3, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+4, \out, 0], 1, \addToTail );
		Synth(\channelrouter8_1, [\in, channelOffset+5, \out, 0], 1, \addToTail );
	}

	tuningTheory {
		var tuning;
		tuning = TuningTheory.new;
		tuning.createGUI;	
	}
	
	quit {
		this.killAll;
		window.close;
	}

	mode {arg mode;
		this.quit;
		ThrenoScope.new(hub.channels, mode, hub.key, hub.appPath);
	}

}
