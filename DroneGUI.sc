DroneGUI {
	
	var hub, controller, mainwin, guiwin, intRect;
var modegui, typegui, scalegui, chordgui, namegui, tonicgui, harmgui, ampgui, speedgui, lengthgui, anglegui, tuninggui, envgui, spreadgui, numbergui, freqgui;
	
	*new { arg hub;
		^super.new.initDroneGUI(hub);
	}

	initDroneGUI{arg ahub;

		var params = ().add('type' -> \saw).add('tonic' -> 1).add('harmonics' -> 3).add('amp' -> 0.2).add('speed' -> 1).add('length' -> 30).add('angle' -> 0).add('chord' -> \minor).add('ratio' -> 1).add('env' -> 1).add('freq' -> 55).add('spread' -> 1).add('number' -> 5);
		var selected = 0;
		var val = 0;
		hub = ahub;		

		mainwin = hub.interpreter.mainwin;
		intRect = hub.interpreter.rect;
	//	guiwin = Window("drone", Rect(mainwin.bounds.width+100, 0, 400, 800), resizable:true, border:true).front;
	
		guiwin = CompositeView(mainwin, Rect(intRect.left, 0, 400, 400));
		
	// -------- DRONES

		modegui = PopUpMenu(guiwin, Rect(300, 50, 80, 20))
				.items_(["drone","chord","satellites"])
				.action_({ arg menu;
					params.add('mode' -> menu.item);
Ê Ê 					[menu.value, menu.item].postln;
				});
	
		typegui = PopUpMenu(guiwin, Rect(300, 75, 80, 20))
				.items_([\saw, \pulse, \tri, \sine])
				.action_({ arg menu;
					params.add('type' -> menu.item.asSymbol);
Ê Ê 					[menu.value, menu.item].postln;
				});

		scalegui = PopUpMenu(guiwin, Rect(300, 100, 80, 20))
				.items_([\minor, \major,\iwato])
				.action_({ arg menu;
					params.add('scale' -> menu.item);
Ê Ê 					[menu.value, menu.item].postln;
				});


		chordgui = PopUpMenu(guiwin, Rect(300, 125, 80, 20))
				.items_(~drones.getChordDict.keys.asArray.deepCopy) // 
				.action_({ arg menu;
					params.add('chord' -> menu.item.asSymbol);
Ê Ê 					[menu.value, menu.item].postln;
				});

		namegui = PopUpMenu(guiwin, Rect(300, 150, 80, 20))
				.items_([" - new - "] 
						++ ~drones.chordDict.keys.asArray.deepCopy
						++ ~drones.satellitesDict.keys.asArray.deepCopy 
						++ ~drones.interDict.keys.asArray.deepCopy 
						++ ~drones.droneDict.keys.asArray.deepCopy)
				.mouseDownAction_({arg menu; 
					menu.items_([" - new - "] 
						++ ~drones.chordDict.keys.asArray.deepCopy 
						++ ~drones.satellitesDict.keys.asArray.deepCopy 
						++ ~drones.interDict.keys.asArray.deepCopy 
						++ ~drones.droneDict.keys.asArray.deepCopy)})
				.action_({ arg menu;
					// get the params from the drone and update GUI
					if(menu.item != " - new - ", {
						~drones.select(menu.item);
						selected = ~drones.selected;
					});
				});

		Button(guiwin, Rect(300, 175, 80, 20))
			.states_([["create"]])
			.action_({

				params.postln;

				if(modegui.value == 0, { // drone
					~drones.createDrone(params.type, params.tonic, params.harmonics, params.amp, params.speed, params.length, params.angle, 1, params.ratio, params.env, params.octave);
				});
				
				if(modegui.value == 1, { // chord
					"chord".postln;
					~drones.createChord(params.type, params.chord, params.tonic, params.harmonics, params.amp, params.speed, params.length, params.angle, 1, params.ratio, params.env);
				});
				
				if(modegui.value == 2, { // satellites
					"sat".postln;
					~drones.createSatellites(params.type, params.scale, params.tonic, params.harmonics, params.amp, params.speed, params.length, params.angle, params.number, params.spread, params.env);
				});
				
			});

		Button(guiwin, Rect(300, 200, 80, 20))
			.states_([["kill"]])
			.action_({
				var command;
				command = "~"++namegui.item++".kill";
				command.interpret;
				//~drones.hub.interpreter.opInterpreter("> " ++ command);
				[\command, command].postln;
				namegui.value_(0);
			//	modegui.value.postln;
			});

		Button(guiwin, Rect(300, 225, 80, 20))
			.states_([["killAll"]])
			.action_({
				//~drones.killAll;
				hub.drones.killAll;
				//~drones.hub.interpreter.opInterpreter("> ~drones.killAll");
				namegui.value_(0);
			});


		Button(guiwin, Rect(300, 240, 80, 20))
			.states_([["debug"]])
			.action_({
				
				namegui.items_([" - new - "] 
						++ ~drones.chordDict.keys.asArray.deepCopy 
						++ ~drones.satellitesDict.keys.asArray.deepCopy 
						++ ~drones.interDict.keys.asArray.deepCopy 
						++ ~drones.droneDict.keys.asArray.deepCopy);
	
			//	modegui.value.postln;
			});
		
		tonicgui = EZSlider(guiwin, Rect(10, 50, 280, 20), "tonic ", ControlSpec(1, 20, step:1), initVal: params[\tonic])
				.action_({ arg sl;
					var command = "~"++namegui.item++".tonic_("++sl.value.asString++")";
					params.add('tonic' -> sl.value);
Ê Ê 					"tonic = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		harmgui = EZSlider(guiwin, Rect(10, 75, 280, 20), "harmonics ", ControlSpec(1, 10, step:1), initVal: params[\harmonics])
				.action_({ arg sl;
					var command = "~"++namegui.item++".harmonics_("++sl.value.asString++")";
					params.add('harmonics' -> sl.value);
Ê Ê 					"harmonics = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		ampgui = EZSlider(guiwin, Rect(10, 100, 280, 20), "amp ", initVal: params[\amp])
				.action_({ arg sl;
					var command = "~"++namegui.item++".amp_("++sl.value.asString++")";
					params.add('amp' -> sl.value);
Ê Ê 					"amp = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		speedgui = EZSlider(guiwin, Rect(10, 125, 280, 20), "speed ", ControlSpec(-40, 40, step:0.1), initVal: params[\speed])
				.action_({ arg sl;
					var command = "~"++namegui.item++".speed_("++sl.value.asString++")";
					params.add('speed' -> sl.value);
Ê Ê 					"speed = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		lengthgui = EZSlider(guiwin, Rect(10, 150, 280, 20), "length ", ControlSpec(1, 360, step:1), initVal: params[\length])
				.action_({ arg sl;
					var command = "~"++namegui.item++".length_("++sl.value.asString++")";
					params.add('length' -> sl.value);
Ê Ê 					"length = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		anglegui = EZSlider(guiwin, Rect(10, 175, 280, 20), "angle ", ControlSpec(0, 360, step:1), initVal: params[\angle])
				.action_({ arg sl;
					var command = "~"++namegui.item++".angle_("++sl.value.asString++")";
					params.add('angle' -> sl.value);
Ê Ê 					"angle = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		tuninggui = EZSlider(guiwin, Rect(10, 200, 280, 20), "ratio ", ControlSpec(1, 24, step:1), initVal: params[\ratio])
				.action_({ arg sl;
					var command = "~"++namegui.item++".ratio_("++sl.value.asString++")";
					params.add('ratio' -> sl.value);
Ê Ê 					"ratio = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		envgui = EZSlider(guiwin, Rect(10, 225, 280, 20), "env ", ControlSpec(0.1, 10, step:0.1), initVal: params[\env])
				.action_({ arg sl;
					var command = "~"++namegui.item++".set('env',"++sl.value.asString++")";
					params.add('env' -> sl.value);
Ê Ê 					"env = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		freqgui = EZSlider(guiwin, Rect(10, 250, 280, 20), "freq ", ControlSpec(10, 10000, step:0.01), initVal: params[\freq])
				.action_({ arg sl;
					var command = "~"++namegui.item++".freq_("++sl.value.asString++")";
					params.add('freq' -> sl.value);
Ê Ê 					"freq = ".post; sl.value.postln;
					if(namegui.item != " - new - ", {
						if(sl.value != val, {
							command.interpret;
							//~drones.hub.interpreter.opInterpreter("> " ++ command);
							val = sl.value;
						});
					});
				});

		spreadgui = EZSlider(guiwin, Rect(10, 275, 280, 20), "spread ", ControlSpec(1, 10, step:1), initVal: params[\spread])
				.action_({ arg sl;
					params.add('spread' -> sl.value);
Ê Ê 					"spread = ".post; sl.value.postln;
				});

		numbergui = EZSlider(guiwin, Rect(10, 300, 280, 20), "number ", ControlSpec(1, 10, step:1), initVal: params[\ratio])
				.action_({ arg sl;
					params.add('number' -> sl.value);
Ê Ê 					"number = ".post; sl.value.postln;
				});

		
	}
	
	remove {
		"REMOVING +_____ IN DRONEGUI ________".postln;

		guiwin.remove;
		mainwin.refresh;
	}
}