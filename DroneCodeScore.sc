
// TODO - Add system code events to the score, such as ~drones.env_([0, 1]) or ~drones.tuning = \pythagorean

// TODO - Specify which track drone appears on - also reuse tracks that are not used
// TODO - Make this score window appear as a floating window (good for composition)

DroneCodeScore {
	
	var hub, controller, mainwin, timelinewin, intRect;
	var timelinetask, userview, image, length;
	var <>scoreview;
	var <>scale, drones, dronelines;
	var leavescoreviewopen = false;
	var track = 0;
	var <score;
	var timeline = -5;
	//	var randomseed, offsettime, lastoffsettime, score;

	*new { arg hub, scale=30;
		^super.new.initDroneCodeScore(hub, scale);
	}

	initDroneCodeScore{ | ahub, ascale |

		var name = "compscore";
		//var timeline = 0;

		hub = ahub;
		
		"DOING THE CODE SCORE".postln;
		
		intRect = hub.interpreter.rect;
		scale = ascale; // from 5 to 60
		length = 2000;	
		"hub mode = ".post; hub.mode.postln;
		if(hub.mode == \dev, {
			mainwin = Window("codescore", Rect(hub.screenbounds.width+20, 0, hub.screenbounds.width-intRect.left, hub.screenbounds.height)).front;
			timelinewin = SCScrollView(mainwin, Rect(intRect.left, 0, hub.screenbounds.width-intRect.left, hub.screenbounds.height)).resize_(5);
			userview = UserView(timelinewin, Rect(0, 0, hub.screenbounds.width-intRect.left, length)).background_(Color.white);

		},{
			mainwin = hub.interpreter.mainwin;
			timelinewin = SCScrollView(mainwin, Rect(intRect.left, 0, hub.screenbounds.width-intRect.left, hub.screenbounds.height-200));
			userview = UserView(timelinewin, Rect(0, 0, hub.screenbounds.width-intRect.left-12, length)).background_(Color.white);
		});
		
		image = this.drawImg; // drawing the background (not the drones)
		userview.backgroundImage_(image);
		
		userview.mouseDownAction_({|view, x, y, modifiers, buttonNumber, clickCount| 
			//[view, x, y, modifiers, buttonNumber, clickCount].postln;  
			
			// the "down" button at the bottom
			if(Rect((userview.bounds.width/2)-20, userview.bounds.height-40, 40.5, 20.5).contains(Point(x, y)), {
				"EXPANDING".postln;	
				length = length + 2000;
				
				userview.bounds_(Rect(0, 0, hub.screenbounds.width-intRect.left-12, length));
				image = this.drawImg;
				userview.backgroundImage_(image);
			}, { // if it's not the down button, then it's drones
				this.addTextScore(clickCount);
				dronelines.copy.do({ |drone, i| drone.mouseDown(dronelines, i, x, y, clickCount); });
			});
			userview.refresh;

//			if(Rect((userview.bounds.width/2)-20, userview.bounds.height-40, 40.5, 20.5).contains(Point(x, y)), {
//				
//			});
		});
		
		userview.mouseMoveAction_({|view, x, y, modifiers, buttonNumber, clickCount|  
			dronelines.copy.do({ |drone, i| drone.mouseMove(x, y, modifiers.isShift)});
			userview.refresh;		
		});
		
		userview.mouseUpAction_({|view, x, y, modifiers, buttonNumber, clickCount|     
			dronelines.copy.do({ |drone| drone.mouseUp(dronelines, x, y) });

			//score = score.sort({arg a, b; a[0] <= b[0] }); // Sort main score so it's ready do be written to file
			
			"______________________score___________________".postln;
			Post << score.sort({arg a, b; a[0] <= b[0] }); "".postln;
			if(hub.states.playing == true, { hub.states.updateScore( score ) }); // realtime updates of the score
			this.mouseUpRemoveTextScore;
			userview.refresh; 

		});

		userview.drawFunc_({
			dronelines.do({ | drone | drone.draw });
			Color.red.set;
			Pen.line(Point(50, timeline)+0.5, Point(((userview.bounds.width/20).floor * 20), timeline)+0.5);  
			Pen.stroke;	
		});
		
	}
	
	parseScore { | ascore |
		score = ascore;
		drones = ();
		drones.add(\global -> ().add(\track -> 0).add(\name -> \global ).add(\dronescore -> [ [0, {} ] ] )); // the global timeline (w. dummy entry at 0)
		dronelines = [];
		score.do({| event, i | 
			var cmd, agent, prename, name;
			"NEW EVENT IN SCORE ------------------------------------------".postln;
//			cmd = event[1].replace("  ", "").replace(" ", "");
			cmd = event[1].asCompileString;
			[\time, event[0], \cmd_____, cmd].postln;
			if(	cmd.contains("createDrone") 
				|| cmd.contains("createSatellites") 
				|| cmd.contains("createChord")
				|| cmd.contains("createMachine")
			, { // find a new drone and then its score
				track = track + 1;
				prename =  cmd[cmd.find("name:")+6 .. ];
				name = prename[0 .. prename.find("'")-1 ];
				[\_________name, name].postln;
				drones.add(name.asSymbol -> ().add(\track -> track).add(\name -> name.asSymbol ).add(\dronescore -> [ event ] ));
				"DEV x - drone added".postln;
			}, {
			//	agent = cmd[1.. cmd.find(".")-1];
				agent = cmd[1.. cmd.find(".")-1].tr($~,\); //.tr($ ,\); // find agent name, but remove space (if space) and tilde
				[\agent_________, agent].postln;
				if(drones[agent.asSymbol].isNil, { // global score changes (not creating drones or manipulating them)
					// global commands (such as time change etc.)
					"FOUND GLOBAL ________".post; event.postln;
					drones[\global][\dronescore] = drones[\global][\dronescore].add(event);
				},{ // manipulating drones
					drones[agent.asSymbol][\dronescore] = drones[agent.asSymbol][\dronescore].add(event);
				});
				//[\agent, agent].postln;
			});
				[\track, track, \event, event].postln;
//				 "DRONES GLOBAL IS _________________".post; drones[\global].postln;
		});
		
	//	Post << drones; "".postln;
	//	[\dronesize, drones.size].postln;
		
		drones.do({ | drone, i |
			//dronelines = dronelines.add( TimelineDrone( drones.size+1, event[0]*60*scale, 4000 ) );
			"CREATING DRONE ---->".postln; drone.postln;
			dronelines = dronelines.add( TimelineDrone( drone, scale, this ) );
		});		
	}
		
	addTextScore { | clickcount |
			//"adding scoreview ?????????".postln;
		if(scoreview.isNil, {
			//"adding scoreview".postln;

			scoreview = TextView(mainwin, Rect(hub.screenbounds.height/2+3, 40, hub.screenbounds.height/2-6, hub.screenbounds.height-80));
			scoreview.string_("code score");
			scoreview.focus(true);
			if(clickcount == 2, { 
				leavescoreviewopen = true;
				scoreview.keyUpAction_({ dronelines.do({ | drone | drone.reParseScore; }) });
			});
		});
	}
	
	mouseUpRemoveTextScore {
		if(leavescoreviewopen.not, {
		//	"removing scoreview".postln;
			scoreview.remove;
			scoreview = nil;
			dronelines.do({ | drone | drone.doubleClick = false; });
		});
	}

	removeTextScore { // removing scoreview from the DroneGUI
		//"removing scoreview from GUI".postln;
		
		dronelines.do({ |drone| drone.reParseScore });
		scoreview.remove;
		scoreview = nil;
		leavescoreviewopen = false;
		userview.refresh;
	}
	
	drawImg {
		Pen.font_(Font("Monaco", 9)); // Maybe needed for Qt GUI? (Don't delete)
		image = Image(userview.bounds.width.asInteger, userview.bounds.height.asInteger);
		image.draw({
			Color.black.alpha_(0.4).set;

			// the drone tracks
			((userview.bounds.width/20).floor-3).do({arg i;
				Pen.line(Point(50+(i*20), 20)+0.5, Point(50+(i*20), length)+0.5);  
			});
			((length/scale).floor).do({arg j;
				if(scale < 10, {
					if(j.asInteger.even, { // every other line in low time resolution
						Pen.line(Point(50, j*scale+20)+0.5, Point(((userview.bounds.width/20).floor * 20), j*scale+20)+0.5);
						//Pen.stringAtPoint( (j+"sec").asString, Point(5, j*scale+20)+0.5); // WORKS on Qt GUI
						image.drawStringAtPoint( (j+"s").asString, Point(5, j*scale+20)+0.5, Font("Monaco", 9)); // Needed on Cocoa GUI (else upside down)
					});
				},{
					Pen.line(Point(50, j*scale+20)+0.5, Point(((userview.bounds.width/20) * 20), j*scale+20)+0.5);
					//Pen.stringAtPoint( (j+"sec").asString, Point(5, j*scale+20)+0.5); // WORKS on Qt GUI
					image.drawStringAtPoint( (j+"s").asString, Point(5, j*scale+20)+0.5, Font("Monaco", 9)); // Needed on Cocoa GUI (else upside down)
				});
			});	
			Pen.stroke;
	
			// button at the bottom
			Color.white.set;
			Pen.fillRect( Rect((userview.bounds.width/2)-20, userview.bounds.height-40, 40.5, 20.5)+0.5);
			Color.black.set;
			Pen.strokeRect( Rect((userview.bounds.width/2)-20, userview.bounds.height-40, 40.5, 20.5)+0.5);
			image.drawStringAtPoint("down", Point((userview.bounds.width/2)-15, userview.bounds.height-38), Font("Monaco", 9) );
		});
		^image;
	}

	
	startTimeline { | speed=1 |
		"Starting task".postln;
		timelinetask = Task({
			var now;
			var starttime = Main.elapsedTime;
			inf.do({
				
				now = (Main.elapsedTime - starttime) * speed;
				//[\now, now].postln;
				timeline = (20 + (now * scale)).round(0.01); // FIND THE SCALE (20 is the pixels from top)
				//[\timeline, timeline].postln;
				{
				if(timeline>length, {
					length = length + 2000;
					
					userview.bounds_(Rect(0, 0, hub.screenbounds.width-intRect.left-12, length));
					image = this.drawImg;
					userview.backgroundImage_(image);
					userview.refresh;
				});
				if( timeline > ( timelinewin.visibleOrigin.y+timelinewin.bounds.height), {
					timelinewin.visibleOrigin_(Point(0, timeline))
				});
				userview.refresh;
				}.defer;
				0.05.wait;	
			})
		}).start;
	}
	
	stopTimeline {
		timelinetask.stop;	
	}
	
	remove { // called from DroneController:removeScore
		"REMOVING +_____  code score ________".postln;

		timelinewin.remove;
		scoreview.remove;
		mainwin.refresh;
	}
	
}