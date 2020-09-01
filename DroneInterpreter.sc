
DroneInterpreter {

	var <>mainwin, states, <>textview, <>postview, mode; // textview accessed from DroneStates
	var <rect, <postrect, <lastCommand;
	var lineview;
	
	*new { |hub, mode|
		^super.new.initDroneInterpreter(hub, mode);
	}

	initDroneInterpreter { |arghub, argmode|
		var leftborder;
		var hub = arghub;
		var droneDict = ();
		var thiscommand;
		var drones = hub.drones; // a reference to the DroneMachineCircles class
		var selected = drones.selected;
		var selectedName = try{drones.droneArray[selected].name.asString};
		if(selectedName == nil, {selectedName = "oxo"});
		\deb0.postln;
		//var postrect;
	
		mainwin = hub.window;
		leftborder = mainwin.bounds.height;

		mode = argmode;

		rect = Rect(leftborder+1, 30, Window.screenBounds.width-leftborder, mainwin.bounds.height-250);
		postrect = Rect(leftborder+1, rect.height+11, Window.screenBounds.width-leftborder, mainwin.bounds.height-rect.height-10);
		states = hub.states;

		if(mode == \dev, { // if in development mode, an extra window is created
			"Mode is DEV".postln;
			mainwin = Window("drone", Rect(mainwin.bounds.width, 0, 400, mainwin.bounds.height), resizable:true, border:true).front;
			mainwin.onClose_({ hub.window.close; drones.killAll; });
			mainwin.view.background = Color.white;
			leftborder = 0;
			rect = Rect(leftborder+1, 30, 400, mainwin.bounds.height-60-240);
			postrect = Rect(leftborder+1, rect.height+11, 400, mainwin.bounds.height-rect.height-64);

		}, {
			mainwin.bounds = Window.screenBounds;
		});

		[\mainwinRECT, mainwin.bounds, \rect, rect].postln;

		// main controls - mode, quit, volume,
		Button(mainwin,Rect(rect.left+5, 5 , 50, 20))
			.font_(Font("Monaco", 11))
			.states_([["Quit", Color.black, Color.clear]])
			.action_({ arg butt;
			// STANDALONE
	    		// {0.exit}.value;
	    		// CLASSES
	    			~drones.quit;
			});

		Button(mainwin,Rect(rect.left+65, 5 , 50, 20))
			.font_(Font("Monaco", 11))
			.states_([["Help", Color.black, Color.clear]])
			.action_({ arg butt;
	    		"open http://thormagnusson.github.io/threnoscope".unixCmd;
			});

		StaticText(mainwin, Rect(rect.left+125, 5, 120, 20))
			.font_(Font("Monaco", 11))
			.string_("Window mode:");

		PopUpMenu(mainwin,Rect(rect.left+210, 5, 70, 20))
			.font_(Font("Monaco", 11))
			.items_([ "perform", "performWin", "dev", "displayFS", "displayWin"])
			.action_({ arg menu;
				hub.drones.mode(menu.item.asSymbol);
	    		[menu.value, menu.item].postln;

			});

		StaticText(mainwin, Rect(rect.left+290, 5, 60, 20))
			.font_(Font("Monaco", 11))
			.string_("Main vol:");

		Slider(mainwin, Rect(rect.left+350, 5, 150, 20))
			.value_(0.5)
    		.action_({arg slider;
        		Server.default.volume_(((0.00001+slider.value)*2).ampdb.postln;)
        	});

		Button(mainwin,Rect(rect.left+505, 5, 50, 20))
			.font_(Font("Monaco", 11))
			.states_([["Tuning", Color.black, Color.clear]])
			.action_({ arg butt;
	    		var t;
	    		hub.drones.mode(\dev);
	    		t = TuningTheory.new;
	    		t.createGUI;
			});


		textview = TextView.new(mainwin, rect)
				.focusColor_(Color.clear)
				.hasVerticalScroller_(true)
				.string_("")
				.resize_(2)
				.font_(Font("Helvetica", 14 ))
				.keyDownAction_({|doc, char, mod, unicode, keycode |
					var linenr, string;
					//[doc, char, mod, unicode, keycode].postln;
					// evaluating code (the next line will use .isAlt, when that is available)
					if((mod & 524288 == 524288) && (keycode==124), { // alt + right
						linenr = doc.string[..doc.selectionStart-1].split($\n).size;
						string = doc.selectedString;
						thiscommand = string[1..string.size].replace("   ", "").replace("  ", "");
						this.opInterpreter(string);
						nil;
					});
					if((mod & 524288 == 524288) && (keycode==123), { // alt + left
						textview.string_(textview.string++thiscommand);
						nil;
					});
					
					if((mod & 524288 == 524288) && (keycode==126), { // alt + up
						
						var name, collectivename;
						selected = (selected-1).clip(0, drones.droneArray.size-1);
						name = drones.droneArray[selected].name.asString;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							if(name.contains("chd_"), {
								hub.drones.chordDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.chordDict[key].selected = true;
									});
								});
							});
							if(name.contains("sat_"), {
								hub.drones.satellitesDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.satellitesDict[key].selected = true;
									});
								});
								//satellitesDict[name[0..6].asSymbol].selected = true;
							});	
							if(name.contains("grp_"), {
								//var grpname = key.asString[0..key.asString.findAll("_")[1]-1];
								hub.drones.groupDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.groupDict[key].selected = true;
									});
								});
							});	
							drones.selectedName = collectivename; // only the name of the group
							block{ arg break;
								drones.droneArray.do({ arg drone, i;
									if(drone.name.asString.contains("_"), {
										if((drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename), {
											selected = i;
											break.value();
										});
									});
								});											};
						}, { 
							drones.selectedName = drones.droneArray[selected].name.asString;
							drones.droneArray[selected].selected = true;
						});
																		drones.selected = selected;
						//drones.droneArray[selected].selected = true;
						[\SELECTED_, drones.droneArray[selected].name].postln;
						
						selectedName = drones.droneArray[selected].name.asString;
						hub.postDroneState(selectedName, selected);
//						Document.listener.string = ""; // clear post window
//						string = "~"++selectedName++"\n"++
//						"~"++selectedName++".type = \\"++drones.droneArray[selected].type++"\n"++
//						"~"++selectedName++".tonic = "++drones.droneArray[selected].tonic++"\n"++
//						"~"++selectedName++".harmonics = "++drones.droneArray[selected].harmonics++"\n"++
//						"~"++selectedName++".amp = "++drones.droneArray[selected].amp++"\n"++
//						"~"++selectedName++".speed = "++(drones.droneArray[selected].speed*1000)++"\n"++
//						"~"++selectedName++".length = "++(drones.droneArray[selected].length*360/(2*pi))++"\n"++
//						"~"++selectedName++".angle = "++(drones.droneArray[selected].angle*360/(2*pi))++"\n"++
//						"~"++selectedName++".degree = "++drones.droneArray[selected].degree++"\n"++
//						"~"++selectedName++".ratio = "++drones.droneArray[selected].ratio++"\n"++
//						"~"++selectedName++".env = "++drones.droneArray[selected].env++"\n"++
//						"~"++selectedName++".octave = "++drones.droneArray[selected].octave++"\n";
//						Document.listener.string = string; // add info
//						if(hub.post, { hub.interpreter.postview.string_(string) });
						nil;
					});
					
					
					if((mod & 524288 == 524288) && (keycode==125), { // alt + down
						var name, collectivename;
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = (selected+1).clip(0, drones.droneArray.size-1);
						name = drones.droneArray[selected].name.asString;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							if(name.contains("chd_"), {
								hub.drones.chordDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.chordDict[key].selected = true;
									});
								});
							});
							if(name.contains("sat_"), {
								hub.drones.satellitesDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.satellitesDict[key].selected = true;
									});
								});
							});	
							if(name.contains("grp_"), {
								hub.drones.groupDict.keys.do({ |key| 
									if(key.asString == collectivename, {
										hub.drones.groupDict[key].selected = true;
									});
								});
							});	
							drones.selectedName = collectivename; // only the name of the group/sat/chord
							block{ arg break;
							drones.droneArray[selected .. drones.droneArray.size].do({ arg drone, i;
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
								    selected = (selected+i-1).clip(0, drones.droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							drones.selectedName = drones.droneArray[selected].name.asString;
							drones.droneArray[selected].selected = true;
						});						
						
						/*
						// select another drone
						var name, collectivename;
						var foundFlag = false;
						var oldfoundFlag = false;
						
						selected = drones.selected;
						//drones.droneArray[selected].selected = false;
						selected = (selected+1).clip(0, drones.droneArray.size-1);

						name = drones.droneArray[selected].name.asString;
						[\oooo_, drones.droneArray[selected].name].postln;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						
						if(name.asString.contains("_"), { // it's a group of some sort
							collectivename = name[0..name.findAll("_").last-1];
							[\collectivename, collectivename].postln;
							if(name.contains("chd_"), {
								hub.drones.chordDict[collectivename.asSymbol].selected = true;
							});
							if(name.contains("sat_"), {
								hub.drones.satellitesDict[collectivename.asSymbol].selected = true;
							});	
							if(name.contains("grp_"), {
								hub.drones.groupDict[collectivename.asSymbol].selected = true;
							});	
							drones.selectedName = collectivename; // only the name of the group
//							drones.chordDict[name.asString[0..6].asSymbol].selected = true;
							block{ arg break;
							drones.droneArray[selected .. drones.droneArray.size].do({ arg drone, i;
								"prisdfasd".scramble.postln;
								oldfoundFlag = foundFlag;
								if(drone.name.asString[0..drone.name.asString.findAll("_").last-1] == collectivename, {
									//drone.selected = true;
									foundFlag = true;
								},{
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
									"LAST DRONE IN THE GROUP - nr: ".post; i.postln;
								    selected = (selected+i-1).clip(0, drones.droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							//selected = (selected+1).clip(0, drones.droneArray.size-1);
							drones.selectedName = drones.droneArray[selected].name.asString;
							drones.droneArray[selected].selected = true;
						});
						*/
						
						/*
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(name.asString.contains("_"), { // it's a group of some sort
							hub.drones.chordDict[name.asString[0..6].asSymbol].selected = true;
							selected = (selected + hub.drones.chordDict[name.asString[0..6].asSymbol].dronearray.size-1).clip(0, drones.droneArray.size-1);
						//}, {
							//selected = (selected+1).clip(0, drones.droneArray.size-1);
						//	drones.droneArray[selected].selected = true;
						//	drones.selected = selected;
						});
						
						*/
																		drones.selected = selected;
						selectedName = drones.droneArray[selected].name.asString;
						hub.postDroneState(selectedName, selected);
												
	//drones.droneArray[selected].selected = true;
						[\SELECTED_, drones.droneArray[selected].name].postln;
				
//						Document.listener.string = ""; // clear post window
//						string = "~"++selectedName++"\n"++
//						"~"++selectedName++".type = \\"++drones.droneArray[selected].type++"\n"++
//						"~"++selectedName++".tonic = "++drones.droneArray[selected].tonic++"\n"++
//						"~"++selectedName++".harmonics = "++drones.droneArray[selected].harmonics++"\n"++
//						"~"++selectedName++".amp = "++drones.droneArray[selected].amp++"\n"++
//						"~"++selectedName++".speed = "++(drones.droneArray[selected].speed*1000)++"\n"++
//						"~"++selectedName++".length = "++(drones.droneArray[selected].length*360/(2*pi))++"\n"++
//						"~"++selectedName++".angle = "++(drones.droneArray[selected].angle*360/(2*pi))++"\n"++
//						"~"++selectedName++".degree = "++drones.droneArray[selected].degree++"\n"++
//						"~"++selectedName++".ratio = "++drones.droneArray[selected].ratio++"\n"++
//						"~"++selectedName++".env = "++drones.droneArray[selected].env++"\n"++
//						"~"++selectedName++".octave = "++drones.droneArray[selected].octave++"\n";
//						Document.listener.string = string; // add info
//						if(hub.post, { hub.interpreter.postview.string_(string) });

						nil;
					});

/*

					if((mod & 524288 == 524288) && (keycode==125), { // alt + down
						// select another drone
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = drones.selected;
						//drones.droneArray[selected].selected = false;
						[\oooo_, drones.droneArray[selected].name].postln;
						
						selected = (selected+1).clip(0, drones.droneArray.size-1);
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(drones.droneArray[selected].name.asString.contains("_"), { // it's a group of some sort
							
							block{ arg break;
							drones.droneArray[selected .. drones.droneArray.size].do({ arg drone, i;
								"prisdfasd".scramble.postln;
								oldfoundFlag = foundFlag;
								if(drones.droneArray[selected].name.asString[0..6] == drone.name.asString[0..6], {
									drone.selected = true;
									foundFlag = true;
								},{
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
									"LAST DRONE IN THE GROUP - nr: ".post; i.postln;
								    selected = (selected+i-1).clip(0, drones.droneArray.size-1);
									("selected down = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							//selected = (selected+1).clip(0, drones.droneArray.size-1);
							drones.droneArray[selected].selected = true;
						});
													drones.selected = selected;
//	drones.droneArray[selected].selected = true;

						nil;
					});

					if((mod & 524288 == 524288) && (keycode==125), { // alt + down
						// select another drone
						var foundFlag = false;
						var oldfoundFlag = false;
						selected = drones.selected;
						//drones.droneArray[selected].selected = false;
						[\oooo_, drones.droneArray[selected].name].postln;
						drones.droneArray.do({ arg drone; drone.selected = false }); // deselect all
						if(drones.droneArray[selected].name.asString.contains("_"), { // it's a group of some sort
							
							block{ arg break;
							drones.droneArray.do({ arg drone, i;
								oldfoundFlag = foundFlag;
								if(drones.droneArray[selected].name.asString[0..6] == drone.name.asString[0..6], {
									drone.selected = true;
									foundFlag = true;
								},{
									foundFlag = false;
								});
								if((oldfoundFlag == true) && (foundFlag == false), { // we've reached a drone outside group
									"LAST DRONE IN THE GROUP - nr: ".post; i.postln;
									selected = i.clip(0, drones.droneArray.size-1);
									("selected = "+selected).postln;
									break.value;
								});
							});
							}
						}, {
							selected = (selected+1).clip(0, drones.droneArray.size-1);
							drones.droneArray[selected].selected = true;
						});
													drones.selected = selected;
//	drones.droneArray[selected].selected = true;

						nil;
					});

*/

					if(  (mod & 524288 == 524288)  && (unicode==127), { // cmd + backdelete button = KILL Selected drone from textview
						drones.droneArray[selected].kill;
					});

					if((mod == 131332) || (mod == 131076) && (keycode==36), { // evaluate code in SC mode -> (SHIFT + RETURN)
//						Document.listener.string_("");
//						{
//						postview.string_(Document.listener.string);
//						// postview.select(postview.string.size-2,0); // THIS CAN WORK in Qt
//				//		if(dev.not, {Document.listener.string = ""}); // XXX REVISE !!!
//						}.defer(0.05);

					});

				});

					postview = TextView.new(mainwin, postrect)
					.focusColor_(Color.clear)
			//	.background_(Color.new(0.6, 0.6, 0.6))
					.hasVerticalScroller_(true)
					.font_(Font("Helvetica", 12 ))
					.resize_(2)
					.string_("ThrenoScope - instrument for spatial microtones of long duration");

		lineview = UserView.new(mainwin, Rect(postrect.left, postrect.top-2, postrect.width, 4)).background_(Color.black.alpha_(0.5));

	}
	
	opInterpreter { "WARNING: THIS METHOD IS NOT USED ANYMORE".postln; }

	/*
	
	opInterpreter { |argstring|
		var agent, dot, command, interprstr, combinedstring, string;
		//[\argstring_____________, argstring].postln;
		string = argstring[1..argstring.size].replace("   ", " ").replace("  ", " ");
		string = string[0..string.size].replace("\n", "");
		if(string[0] == $ , {string = string[1..string.size]});
		dot = string.find(".");
		agent = string[0..dot-1];
		command = string[dot..string.size];
		interprstr = nil;
		//[\agent_______, agent, \command, command, \string, string].postln;
	
		{postview.string_("");}.defer;
		//if(dev.not, {Document.listener.string = ""});

		if(~drones.droneDict[agent.asSymbol].isNil.not, {
			"DRONE EXISTS".postln;
			combinedstring = ("~drones.droneDict[\\"++agent++"]"++command);
			//combinedstring.postln;
			try{
				//{
				interprstr = combinedstring.interpret;
				states.addToScore_(combinedstring);
				//}.defer;
			}{ "\nThis drone exists, but there is a syntax error".postln };
		}, {
			if(~drones.chordDict[agent.asSymbol].isNil.not, {
				var dronearray, chord;
				"CHORD EXISTS".postln;
				chord = ~drones.chordDict[agent.asSymbol];
				[\chord, chord].postln;
				chord.perform(command);
//				dronearray.do({arg drone;
//					combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
//					combinedstring.postln;
//					try{
//						interprstr = combinedstring.interpret;
//						states.addToScore_(combinedstring);
//					}{ "\nThis chord exists, but there is a syntax error".postln };
//				});
				if(command.contains("kill"), {
					~drones.chordDict.removeAt(agent.asSymbol);
				});
			}, {
				if(~drones.satellitesDict[agent.asSymbol].isNil.not, {
					var dronearray, satellites;
					"SATELLITE EXISTS".postln;
					satellites = ~drones.satellitesDict[agent.asSymbol];
					[\satellites, satellites].postln;
					satellites.perform(command);
//					dronearray.do({arg drone;
//						combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
//						combinedstring.postln;
//						try{
//							interprstr = combinedstring.interpret;
//							states.addToScore_(combinedstring);
//						}{ "\nThis satellite exists, but there is a syntax error".postln };
//					});
					if(command.contains("kill"), {
						~drones.satellitesDict.removeAt(agent.asSymbol);
					});
				}, {
					if(~drones.groupDict[agent.asSymbol].isNil.not, {
						var dronearray, group, tonicoffset, tonicshift, tonicmove;
						"GROUP EXISTS".postln;
						group = ~drones.groupDict[agent.asSymbol];
						group.perform(command);
						/*
						if(command.contains("tonic"), { // exemption - this is needed here since a group should be relative to tonic
							tonicoffset = command[8..command.size].asInteger;
							tonicshift = dronearray.collect({arg drone, i; drone.tonic});
							tonicmove = tonicoffset-tonicshift.minItem;
							tonicshift = tonicshift+tonicmove;  // (the logic -> 4-[4, 2, 7].minItem; [4,2,7]+2)
							dronearray.do({arg drone, i;
								combinedstring = ("~drones.droneDict[\\"++drone.name++"].tonic_("++tonicshift[i]++")");
								combinedstring.postln;
								try{
									interprstr = combinedstring.interpret;
									states.addToScore_(combinedstring);
								}{ "\nThis group exists, but there is a syntax error".postln };
							});
						},{
							dronearray.do({arg drone;
								combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
								combinedstring.postln;
								try{
									interprstr = combinedstring.interpret;
									states.addToScore_(combinedstring);
								}{ "\nThis group exists, but there is a syntax error".postln };
							});
						});
						*/
						if(command.contains("kill"), {
							~drones.groupDict.removeAt(agent.asSymbol);
						});
					}, {
						if(~drones.interDict[agent.asSymbol].isNil.not, {
							var dronearray;
					//		"INTERPRETATION EXISTS".postln;
							dronearray = ~drones.interDict[agent.asSymbol];
							dronearray.do({arg drone;
								combinedstring = ("~drones.droneDict[\\"++drone.name++"]"++command);
								//combinedstring.postln;
								try{
									// XXX - check if I need to defer this (check with score)
									{interprstr = combinedstring.interpret;
									states.addToScore_(combinedstring);}.defer;
								}{ "\nThis interpretation exists, but there is a syntax error".postln };
							});
							if(command.contains("kill"), {
								~drones.interDict.removeAt(agent.asSymbol);
							});
						}, {
							if(~drones.machineDict[agent.asSymbol].isNil.not, {
					//		"MACHINE EXISTS".postln;
								combinedstring = ("~drones.machineDict[\\"++agent++"]"++command);
								//combinedstring.postln;
								try{
									{interprstr = combinedstring.interpret;
									states.addToScore_(combinedstring);}.defer;
								}{ "\nThis machine exists, but there is a syntax error".postln };
								if(command.contains("kill"), {
									~drones.machineDict.removeAt(agent.asSymbol);
								});
							}, {
								if(agent == "~drones", { // all ~drones.xxx methods evaluated here
									"INSIDE DRONES +++++++++++++ ".postln;
									//[\string, string].postln;
									//string.interpret;
									try{
										
										interprstr = string.interpret;
										[\string, string, \interprstr, interprstr].postln;
										//{
										states.addToScore_(string);
										//}.defer;
									}{ "\nSome syntax error occured".postln };
								}, {
								//	interprstr = "No drone, chord or satellite with that name";
									interprstr = "Command not understood";
								});
							});
						});
						// [\combinedstring, combinedstring].postln;
					});
				});
			});
		});

		{
		//  textview.string_(textview.string++"\n"++interprstr++"\n> "); // post result
		    textview.string_(textview.string++"\n> ");
		//	postview.string_(Document.listener.string++"\n\n"++interprstr++"\n");
			postview.string_("\n\n"++interprstr++"\n");
		//	if(dev.not, {Document.listener.string = ""});
		}.defer(0.01);

		lastCommand = command;

	}
	*/

	gui { | bool |
		if( bool, {
			textview.bounds_(Rect(rect.left, 400, rect.width, rect.height-400));
			//mainwin.refresh;
		}, {
			//mainwin.refresh;
			textview.bounds_(rect);
		});
		mainwin.refresh;
	}

	codescore { | bool |
		if( bool, {
			textview.bounds_(Rect(rect.left, 703, rect.width, rect.height-300));
			postview.bounds_(Rect(rect.left, 1100, rect.width, rect.height)); // remove this out of the screen
			lineview.bounds_(Rect(rect.left, 703-2, rect.width, 4));

			//mainwin.refresh;
		}, {
			//mainwin.refresh;
			"removing codescore".postln;
			lineview.bounds_(Rect(postrect.left, postrect.top-2, postrect.width, 4));
			textview.bounds_(rect);
			postview.bounds_(postrect);
		});
		
		mainwin.refresh;
	}

	newSelect { |argselectname|
		textview.string_(textview.string++"\n> "++argselectname);
	}

	getTextViewString {
		^textview.string;
	}

	setTextViewString_ {arg string;
		textview.string_("\n\n"++string++"\n");
	}
}
