
DroneMachine {

	var task;
	var hub;
	var type, target, time, rate, range, <name, transtime;
	var activedrones;
	var sd;
	var selecteBeforeMachine;
	var updateFunc, drawFunc, drawNow;
	var dronesarray, initialStates;
	var colarray, locarray;
	
	*new { arg hub, type, target, time, rate, transtime, range; 
		^super.new.initDroneMachine(hub, type, target, time, rate, transtime, range);
	}
	
	initDroneMachine {|arghub, argtype, argtarget, argtime, argrate, argtranstime, argrange|
		hub = arghub;
		type = argtype ? \amp;
		target = argtarget; // the drone, chord or satellite that is the target for this machine
		activedrones = [];
		time = argtime; // the machine's lifetime
		rate = argrate; // update rate of the machine (hz.reciprocal)
		range = argrange; // the number of octaves
		transtime = argtranstime;
		drawNow = true; // engines can set it to false once in a while 
		sd = hub.window.bounds.height;
		selecteBeforeMachine = hub.drones.droneArray[hub.drones.selected].name.asSymbol;

		"Creating a Drone Machine".postln;
		
		if(target != \all, {
			if(hub.drones.droneDict.includesKey(target), { // if it's one drone that's the target
				dronesarray = hub.drones.droneDict.at(target).asArray;
			});
			if(hub.drones.chordDict.includesKey(target), { // if it's a chord that's the target
				dronesarray = hub.drones.chordDict.at(target).asArray;
			});
			if(hub.drones.satellitesDict.includesKey(target), { // if it's a satellite that's the target
				dronesarray = hub.drones.satellitesDict.at(target).asArray;
			});
			if(hub.drones.groupDict.includesKey(target), { // if it's a satellite that's the target
				dronesarray = hub.drones.groupDict.at(target).asArray;
			});
			if(hub.drones.interDict.includesKey(target), { // if it's a satellite that's the target
				dronesarray = hub.drones.interDict.at(target).asArray;
			});
		}, {
			dronesarray = hub.drones.droneArray;
		});

		// INITIAL STATES -> in order to put drones back into their original state after a machine
		switch(type)
		{ \chord }{
			initialStates = dronesarray.collect({arg drone; drone.ratio }); 
		}
		{ \scale }{
			initialStates = dronesarray.collect({arg drone; drone.degree }); 
		}
		{ \amp }{
			initialStates = dronesarray.collect({arg drone; drone.amp }); 
		}
		{ \harmonics }{
			initialStates = dronesarray.collect({arg drone; drone.harmonics }); 
		};
	
	[\dronesarray, dronesarray].postln;
	
		updateFunc = this.getUpdateFunc(type, target, range, transtime);
		drawFunc = this.getDrawFunc(type);
		this.start();
	}
	
	name_ { |argname|
		name = argname.asSymbol;
		"MACHINE NAME : ".post; name.postln;
	}

	start {
		// start a task (on low framerate 1Hz) that calls this.update
		task = {
			((time*rate.reciprocal) - 1).do({ |i|
				this.update(false); 
				rate.wait;
			});
			this.update(true); // last update 
			[rate, transtime].maxItem.wait; // wait for things to settle
			this.kill();
		}.fork(TempoClock.new);
	}
	
	stop {
		task.stop();
		this.update(true); // if machine is killed in the middle of operation, set drones back to default
	}
	
	kill { // from User and from start (above - when task is finished)
		"Killing machine: ".post; this.name.postln;
		hub.drones.killMachine(this.name);
		this.stop();
	}

	update { | last=false |
		//[\updatefunc, updateFunc].postln;
		updateFunc.value(last);
	}
	
	draw {
		//[\activedrones, activedrones].postln;
		// [\drawNow, drawNow].postln;
		// if(drawNow, { ^drawFunc });
		^drawFunc;
	}
	
	getUpdateFunc { | type, target, range, transtime |
		"Getting update function".postln;
		switch(type)
		
			{\chord} {
				var ready = true;
				^{ |last|
				var drone, added;
				//hub.drones.select(hub.drones.droneDict.choose.name.asSymbol); // select drones
				if(target == \all, {
					drone = hub.drones.droneDict.choose;
				},{
					drone = dronesarray.choose;
				});		
				if(activedrones.size < 4, {
					added = false;
					activedrones.do({arg item; if(item === drone, { added = true }) }); 
					if(added == false, { activedrones = activedrones.add(drone) });
				}, {
					activedrones = activedrones.reject({arg item; item === drone });
				});	
				// the logic
				if(last.not, {
					{ drawNow = true;
					{ drawNow = false }.defer(0.3);
					if(0.5.coin, {
						if(ready, {
							{ drone.chordnote_(1+drone.chord.size.rand, transtime) }.defer(0.1);
							ready = false;
							{ ready = true }.defer(transtime); // make sure two commands are not on the same drone (it jumps)
						});
					})}.value;
				}, { // the last event puts it back into place
					drawNow = true;
					{ drawNow = false }.defer(0.3);
					initialStates.do({ |ratio, i|
						// only going back after transtime (since the command above might have run it)
						{ dronesarray[i].ratio_(ratio, transtime) }.defer(transtime); // no competition 
					});
				});
			}}
			
			{\scale} {
				var ready = true;
				^{ |last|
				var drone, added;
				//hub.drones.select(hub.drones.droneDict.choose.name.asSymbol); // select drones
				if(target == \all, {
					drone = hub.drones.droneDict.choose;
				},{
					drone = dronesarray.choose;
				});		
				if(activedrones.size < 4, {
					added = false;
					activedrones.do({arg item; if(item === drone, { added = true }) }); 
					if(added == false, { activedrones = activedrones.add(drone) });
				}, {
					activedrones = activedrones.reject({arg item; item === drone });
				});	
				// the logic
				if(last.not, {
					{var degree; 
					degree = range*(1+(drone.scalesize.rand));
					drawNow = true;
					{drawNow = false}.defer(0.3);
					if(0.5.coin, {
						if(ready, {
							{ drone.degree_(degree, transtime) }.defer(0.1);
							ready = false;
							{ ready = true }.defer(transtime); // make sure two commands are not on the same drone (it jumps)
						});
					})}.value;
				}, {
					drawNow = true;
					{ drawNow = false }.defer(0.3);
					initialStates.do({ |degree, i|
						{ dronesarray[i].degree_(degree, transtime) }.defer(transtime); // back to tonic
					});
				});
			}}
			{\amp} {
				var ready = true;
				^{ |last|
				var drone, added;
				//hub.drones.select(hub.drones.droneDict.choose.name.asSymbol); // select drones
				if(target == \all, {
					drone = hub.drones.droneDict.choose;
				},{
					drone = dronesarray.choose;
				});		
				if(activedrones.size < 4, {
					added = false;
					activedrones.do({arg item; if(item === drone, { added = true }) }); 
					if(added == false, { activedrones = activedrones.add(drone) });
				}, {
					activedrones = activedrones.reject({arg item; item === drone });
				});	
				// the logic
				if(last.not, {
					{
					var newamp;
					newamp = rand2(0.5, 1); // -50% to -10% or 10% to 50% change in amp
					drawNow = true;
					{drawNow = false}.defer(0.3);
					if(0.5.coin, {
						if(ready, {
							{ drone.relAmp_(newamp.range(0, 2), transtime) }.defer(0.1);
							ready = false;
							{ ready = true }.defer(transtime); // make sure two commands are not on the same drone (it jumps)
						});
					})}.value;
				}, {
					drawNow = true;
					{ drawNow = false }.defer(0.3);
					initialStates.do({ |amp, i|
						{ dronesarray[i].amp_(amp, transtime) }.defer(transtime); // back to orignal amp
					});
				});
			}}			
			{\harmonics} {
				var ready = true;
				^{ |last|
				var drone, added;
				//hub.drones.select(hub.drones.droneDict.choose.name.asSymbol); // select drones
				if(target == \all, {
					drone = hub.drones.droneDict.choose;
				},{
					drone = dronesarray.choose;
				});		
				if(activedrones.size < 4, {
					added = false;
					activedrones.do({arg item; if(item === drone, { added = true }) }); 
					if(added == false, { activedrones = activedrones.add(drone) });
				}, {
					activedrones = activedrones.reject({arg item; item === drone });
				});	
				// the logic
				if(last.not, {
					{var newharm;
					newharm = rrand(1, 7);
					drawNow = true;
					{drawNow = false}.defer(0.3);
					if(0.5.coin, {
						if(ready, {
							{ drone.harmonics_(newharm.max(drone.harmonics), transtime) }.defer(0.1);
							ready = false;
							{ ready = true }.defer(transtime); // make sure two commands are not on the same drone (it jumps)
						});
					})}.value;
				}, {
					drawNow = true;
					{ drawNow = false }.defer(0.3);
					initialStates.do({ |harm, i|
						{ dronesarray[i].harmonics_(harm, transtime)}.defer(transtime); // back to orignal amp
					});
				});
			}}			
			// not ready yet
			
			{\create} {^{ |last|
				var drone, added;
				//hub.drones.select(hub.drones.droneDict.choose.name.asSymbol); // select drones
				if(target == \all, {
					drone = hub.drones.droneDict.choose;
				},{
					"target is : ".post; target.postln;
					drone = dronesarray.choose;
				});		
				if(activedrones.size < 4, {
				//	"Machine added drone:  -->>>-- ".post; drone.name.postln;
					added = false;
					activedrones.do({arg item; if(item.name == drone.name, {added = true}) }); 
					if(added == false, {activedrones = activedrones.add(drone) });
				}, {
					activedrones = activedrones.reject({arg item; item.name == drone.name });
				//	"Machine removed drone:  --<<<-- ".post; drone.name.postln;
				});	
			}}
			{\neutral} {^{
				var drone, added;
				//hub.drones.select(hub.drones.droneDict.choose.name.asSymbol); // select drones
				if(target == \all, {
					drone = hub.drones.droneDict.choose;
				},{
					"target is : ".post; target.postln;
					drone = dronesarray.choose;
				});		
				if(activedrones.size < 4, {
				//	"Machine added drone:  -->>>-- ".post; drone.name.postln;
					added = false;
					activedrones.do({arg item; if(item.name == drone.name, {added = true}) }); 
					if(added == false, {activedrones = activedrones.add(drone) });
				}, {
					activedrones = activedrones.reject({arg item; item.name == drone.name });
				//	"Machine removed drone:  --<<<-- ".post; drone.name.postln;
				});	
			}};
	}
	
	getDrawFunc { |type| // this is only called on startup
		"Getting draw function".postln;
		switch(type)
			{\chord} {
				colarray = [[0.2, rrand(0.15, 0.5)]] ++ ({[rrand(0.15, 0.5), rrand(0.1, 0.4)]}!5);
				locarray = {[(2pi).rand, (2pi).rand]}!4;
				^{
				// the machine function (the lines)
				if(drawNow, {
					activedrones.do({ arg drone;
						Pen.use {
							Pen.strokeColor = Color.red(0.2, rrand(0.7, 1));
//					 		Pen.fillColor = Color.red( 0.8, rrand(0.7, 1) );
					 		if(0.5.coin, {
								Pen.rotate(drone.getDroneLook[2], (sd/2)+0.5, sd/2 );
						 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		}, {
								Pen.rotate(drone.getDroneLook[2]+drone.getDroneLook[3], (sd/2)+0.5, sd/2 );
					 			Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		});	 		
					 		Pen.stroke;
					 		Pen.fill;
					 	};
					 });
				});

				// the machine look (the center wedges)
				if(0.5.coin, {  // make the machines move slower (if true would make it really fast)
					colarray = [[0.2, rrand(0.15, 0.5)]] ++ ({[rrand(0.15, 0.5), rrand(0.1, 0.4)]}!5);
			//		locarray = {[(2pi).rand, (2pi).rand]}!4;
					locarray = locarray+[rrand(0.1, 0.3), rrand(0.1, 0.3), rrand(0.1, 0.3), rrand(0.1, 0.3)];
				});
					
				Pen.strokeColor = Color.red(colarray[0][0], colarray[0][1]);
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.stroke;

				Pen.fillColor = Color.red(colarray[1][0], colarray[1][1]);
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.fillStroke;
				Pen.fillColor = Color.red(colarray[2][0], colarray[2][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 10, 22, locarray[0][0], locarray[0][1]);
				Pen.fillStroke;
				Pen.fillColor = Color.red(colarray[3][0], colarray[3][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 16, 32, locarray[1][0], locarray[1][1]);
				Pen.fillStroke;
				Pen.fillColor = Color.red(colarray[4][0], colarray[4][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 20, 36, locarray[2][0], locarray[2][1]);
				Pen.fillStroke;
				Pen.fillColor = Color.red(colarray[5][0], colarray[5][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 26, 32, locarray[3][0], locarray[3][1]);
				Pen.fillStroke;
				}
			}
			{\scale} {
				^{
				if(drawNow, {
					activedrones.do({ arg drone;
						Pen.use {
							Pen.strokeColor = Color.green(0.3, rrand(0.7, 1));
//					 		Pen.fillColor = Color.green( 0.4, rrand(0.7, 1) );
					 		if(0.5.coin, {
								Pen.rotate(drone.getDroneLook[2], (sd/2)+0.5, sd/2 );
						 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		}, {
								Pen.rotate(drone.getDroneLook[2]+drone.getDroneLook[3], (sd/2)+0.5, sd/2 );
					 			Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		});	 		
					 		Pen.stroke;
					 		Pen.fill;
					 	
					 	};
					 });
				});
				
				// the machine look (the center wedges)
				if(0.3.coin, {  // make the machines move slower (if true would make it really fast)
					colarray = [[0.2, rrand(0.15, 0.5)]] ++ ({[rrand(0.15, 0.5), rrand(0.1, 0.4)]}!5);
					locarray = {[(2pi).rand, (2pi).rand]}!4;
				});

				Pen.strokeColor = Color.green(colarray[0][0], colarray[0][1]);
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.stroke;

				Pen.fillColor = Color.green(colarray[1][0], colarray[1][1]);
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.fillStroke;
				Pen.fillColor = Color.green(colarray[2][0], colarray[2][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 10, 22, (2pi).rand, (2pi).rand);
				Pen.fillStroke;
				Pen.fillColor = Color.green(colarray[3][0], colarray[3][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 16, 32, (2pi).rand, (2pi).rand);
				Pen.fillStroke;
				Pen.fillColor = Color.green(colarray[4][0], colarray[4][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 20, 36, (2pi).rand, (2pi).rand);
				Pen.fillStroke;
				Pen.fillColor = Color.green(colarray[5][0], colarray[5][1]);
				Pen.addAnnularWedge((sd/2)@(sd/2), 26, 32, (2pi).rand, (2pi).rand);
				Pen.fillStroke;
				}
			}
			{\amp} {
				^{
				if(drawNow, {
					activedrones.do({ arg drone;
						Pen.use {
							Pen.strokeColor = Color.red(0.3, rrand(0.7, 1));
					 		//Pen.fillColor = Color.red( 0.4, rrand(0.7, 1) );
					 		if(0.5.coin, {
								Pen.rotate(drone.getDroneLook[2], (sd/2)+0.5, sd/2 );
						 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		}, {
								Pen.rotate(drone.getDroneLook[2]+drone.getDroneLook[3], (sd/2)+0.5, sd/2 );
					 			Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		});	 		
					 		Pen.stroke;
					 		Pen.fill;
					 	
					 	};
					});
				});
				

				// the machine look (the center wedges)
				if(0.3.coin, {  // make the machines move slower (if true would make it really fast)
					colarray = [[0.2, rrand(0.15, 0.5)]] ++ ({[rrand(0.15, 0.5), rrand(0.1, 0.4)]}!5);
					locarray = {[(2pi).rand, (2pi).rand]}!4;
				});

				Pen.strokeColor = Color.red(colarray[0][0], colarray[0][1]);
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.stroke;

//				Pen.fillColor = Color.green(colarray[1][0], colarray[1][1]);
//				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
//				Pen.fillStroke;
//				Pen.fillColor = Color.green(colarray[2][0], colarray[2][1]);
//				Pen.addAnnularWedge((sd/2)@(sd/2), 10, 22, (2pi).rand, (2pi).rand);
//				Pen.fillStroke;
//				Pen.fillColor = Color.green(colarray[3][0], colarray[3][1]);
//				Pen.addAnnularWedge((sd/2)@(sd/2), 16, 32, (2pi).rand, (2pi).rand);
//				Pen.fillStroke;
//				Pen.fillColor = Color.green(colarray[4][0], colarray[4][1]);
//				Pen.addAnnularWedge((sd/2)@(sd/2), 20, 36, (2pi).rand, (2pi).rand);
//				Pen.fillStroke;
//				Pen.fillColor = Color.green(colarray[5][0], colarray[5][1]);
//				Pen.addAnnularWedge((sd/2)@(sd/2), 26, 32, (2pi).rand, (2pi).rand);
//				Pen.fillStroke;
//
//
//				Pen.strokeColor = Color.red(0.8, rrand(0.7, 1)).alpha_(rrand(0.05, 0.4));
				if(0.5.coin, { 
					Pen.fillColor = Color.red(colarray[1][0], colarray[1][1]).alpha_(rrand(0.05, 0.4));
					Pen.addOval(Rect((sd/2)-10, (sd/2)-10, 20, 20)) });
				if(0.5.coin, { 
					Pen.fillColor = Color.red(colarray[2][0], colarray[2][1]).alpha_(rrand(0.05, 0.4));
					Pen.addOval(Rect((sd/2)-15, (sd/2)-15, 30, 30)) });
				if(0.5.coin, { 
					Pen.fillColor = Color.red(colarray[3][0], colarray[3][1]).alpha_(rrand(0.05, 0.4));
					Pen.addOval(Rect((sd/2)-20, (sd/2)-20, 40, 40)) });
				if(0.5.coin, { 
					Pen.fillColor = Color.red(colarray[4][0], colarray[4][1]).alpha_(rrand(0.05, 0.4));
					Pen.addOval(Rect((sd/2)-25, (sd/2)-25, 50, 50)) });
				if(0.5.coin, { 
					Pen.fillColor = Color.red(colarray[5][0], colarray[5][1]).alpha_(rrand(0.05, 0.4));
					Pen.addOval(Rect((sd/2)-30, (sd/2)-30, 60, 60)) });
				Pen.stroke;
				}
			}
			{\harmonics} {
				^{
				if(drawNow, {
					activedrones.do({ arg drone;
						Pen.use {
							Pen.strokeColor = Color.green( 0.2, rrand(0.7, 1));
					 	//	Pen.fillColor = Color.green( 0.2, rrand(0.7, 1) );
					 		if(0.5.coin, {
								Pen.rotate(drone.getDroneLook[2], (sd/2)+0.5, sd/2 );
						 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		}, {
								Pen.rotate(drone.getDroneLook[2]+drone.getDroneLook[3], (sd/2)+0.5, sd/2 );
					 			Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
						 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
					 		});	 		
					 		Pen.stroke;
					 		Pen.fill;
					 	
					 	};
					});
				});
				Pen.strokeColor = Color.green(0.2, rrand(0.5, 0.7));
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.stroke;

				Pen.strokeColor = Color.green(0.3, rrand(0.7, 1)).alpha_(rrand(0.05, 0.4));
				if(0.5.coin, { Pen.addOval(Rect((sd/2)-10, (sd/2)-10, 20, 20)) });
				if(0.5.coin, { Pen.addOval(Rect((sd/2)-15, (sd/2)-15, 30, 30)) });
				if(0.5.coin, { Pen.addOval(Rect((sd/2)-20, (sd/2)-20, 40, 40)) });
				if(0.5.coin, { Pen.addOval(Rect((sd/2)-25, (sd/2)-25, 50, 50)) });
				if(0.5.coin, { Pen.addOval(Rect((sd/2)-30, (sd/2)-30, 60, 60)) });
				Pen.stroke;
				}
			}
			{\create} {
				^{
				activedrones.do({ arg drone;
					Pen.use {
						
						Pen.strokeColor = Color.green(0.3, rrand(0.7, 1));
				 		Pen.fillColor = Color.green( 0.3, rrand(0.7, 1) );
				 		
				 		if(0.5.coin, {
							Pen.rotate(drone.getDroneLook[2], (sd/2)+0.5, sd/2 );
					 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
				 			Pen.stroke;
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
				 			Pen.fill;
				 		}, {
							Pen.rotate(drone.getDroneLook[2]+drone.getDroneLook[3], (sd/2)+0.5, sd/2 );
				 			Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
				 			Pen.stroke;
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
				 			Pen.fill;
				 		});	 		
//				 		Pen.stroke;
//				 		Pen.fill;
				 	
					};
				});
				Pen.strokeColor = Color.green(0.3, rrand(0.7, 1));
				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
				Pen.stroke;
				Pen.strokeColor = Color.green(0.3, rrand(0.7, 1)).alpha_(rrand(0.05, 0.4));
				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-15, (sd/2)-15, 30, 30)) });
				Pen.stroke;
				Pen.width = 1.5;
				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-25, (sd/2)-25, 50, 50)) });
				Pen.stroke;
				Pen.width = 0.5;
				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-35, (sd/2)-35, 70, 70)) });
				Pen.stroke;
				Pen.width = 2.5;
				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-45, (sd/2)-45, 90, 90)) });
				Pen.stroke;
				Pen.width = 3.5;
				Pen.stroke;
				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-55, (sd/2)-55, 110, 110)) });
				Pen.stroke;
				}
			}
			{\neutral} {
				var points;
				points = Array.fill(8, { Point(40.rand2, 40.rand2) });
				^{
				var droneB = activedrones.choose;
				activedrones.do({ arg drone;
					Pen.use {
						
						Pen.strokeColor = Color.green(0.3, rrand(0.7, 1));
				 		Pen.fillColor = Color.green( 0.3, rrand(0.7, 1) );
				 		
				 		if(0.5.coin, {
							Pen.rotate(drone.getDroneLook[2], (sd/2)+0.5, sd/2 );
					 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
							Pen.stroke;
							Pen.strokeColor = Color.blue(0.3, rrand(0.7, 1));
						Pen.line(Point((sd/2)+0.5+(drone.getDroneLook[1]), (sd/2)), Point((sd/2)+0.5+(droneB.getDroneLook[1]), sd/2));
				 			Pen.stroke;
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
				 			Pen.fill;
				 		Pen.fillColor = Color.blue( 0.3, rrand(0.7, 1) );
					 		Pen.addOval( Rect( (sd/2)+0.5+(droneB.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
				 			Pen.fill;
				 		}, {
							Pen.rotate(drone.getDroneLook[2]+drone.getDroneLook[3], (sd/2)+0.5, sd/2 );
					 		Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5+(drone.getDroneLook[1]), sd/2));
							Pen.stroke;
							Pen.strokeColor = Color.blue(0.3, rrand(0.7, 1));
						Pen.line(Point((sd/2)+0.5+(drone.getDroneLook[1]), (sd/2)), Point((sd/2)+0.5+(droneB.getDroneLook[1]), sd/2));
				 			Pen.stroke;
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[0])-4, (sd/2)-4, 8, 8) );
					 		Pen.addOval( Rect((sd/2)+0.5+(drone.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
				 			Pen.fill;
				 		Pen.fillColor = Color.blue( 0.3, rrand(0.7, 1) );
					 		Pen.addOval( Rect( (sd/2)+0.5+(droneB.getDroneLook[1])-4, (sd/2)-4, 8, 8) );
				 			Pen.fill;
				 		 });	 		
				 		//Pen.stroke;
				 		//Pen.fill;
				 	
					};
				});
//				Pen.strokeColor = if(0.5.coin, {Color.green(0.3, rrand(0.7, 1))}, {Color.blue(0.3, rrand(0.7, 1))});
//				Pen.addOval(Rect((sd/2)+0.5-5, (sd/2)-5, 10, 10));
//				Pen.stroke;
//				Pen.strokeColor = if(0.5.coin, {Color.green(0.3, rrand(0.7, 1))}, {Color.blue(0.3, rrand(0.7, 1))});
//				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-15, (sd/2)-15, 30, 30)) });
//				Pen.stroke;
//				Pen.width = 1.5;
//				Pen.strokeColor = if(0.5.coin, {Color.green(0.3, rrand(0.7, 1))}, {Color.blue(0.3, rrand(0.7, 1))});
//				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-25, (sd/2)-25, 50, 50)) });
//				Pen.stroke;
//				Pen.width = 0.5;
//				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-35, (sd/2)-35, 70, 70)) });
//				Pen.stroke;
//				Pen.strokeColor = if(0.5.coin, {Color.green(0.3, rrand(0.7, 1))}, {Color.blue(0.3, rrand(0.7, 1))});
//				Pen.width = 0.5;
//				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-45, (sd/2)-45, 90, 90)) });
//				Pen.stroke;
//				Pen.width = 1.5;
//				Pen.stroke;
//				if(0.5.coin, { Pen.addOval(Rect((sd/2)+0.5-55, (sd/2)-55, 110, 110)) });
//				Pen.stroke;
//				Pen.width = 0.5;
				
				points = Array.fill(8, { Point((sd/2)+30.rand2, (sd/2)+30.rand2) });
			Ê Ê Pen.strokeColor = Color.green(0.3);
			Ê Ê Pen.moveTo(points[0]);
				points.do({arg point;
					Pen.lineTo(point);
				});
			Ê Ê Pen.stroke;
			Ê Ê Pen.strokeColor = Color.green(0.3);
				points.do({arg point;
				Ê Ê Pen.addArc(point, 2+(4.0.rand), 2pi, 2pi);
				});
				Pen.stroke;

				}
			}
	}
}