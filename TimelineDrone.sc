
TimelineDrone {
	
	classvar gltopdown, glmiddledown, glbottomdown; // class var since I want to detect only the top one
	var topdown, middledown, bottomdown; 
	//	var hub, controller, mainwin, guiwin, intRect;
	var track;
	var top, bottom, length, left;
	var color, mousedownoffset, timeoffsets, selevent;
	var drone, score, name, scoreview;
	var scorestring;
	var scale;
	var ytrans = 20;
	var xtrans = 50;
	var parent;
	var >doubleClick;
	
	*new { | drone, scale, parent |
		^super.new.initTimelineDrone( drone, scale, parent );
	}

	initTimelineDrone { | argdrone, argscale, argparent |
		parent = argparent;
		drone = argdrone;
		track = drone[\track];
		name = drone[\name];
		scale = argscale;
		left = ((track)*20)-3;
		top = 0;// start;
		bottom = 300000; //duration+top;
		length = bottom;
		topdown = middledown = bottomdown = false;
		gltopdown = glmiddledown = glbottomdown = false;
		color = Color.rand;
		scoreview = parent.scoreview;
		doubleClick = false;
		this.parseScore;
	}

	mouseDown { | array, i, ax, ay, clickCount |
		var x, y;
		x = ax-xtrans;
		y = ay-ytrans;
		if(gltopdown == false, {
			if(Rect(left-3, top-5, 10, 10).contains(Point(x, y)), { 
				gltopdown = true; 
				topdown = true; 
				array.swap(i, array.size-1);
				//[\name, name].postln;
				//[\scoreview, scoreview].postln;
				//[\gltopdown, "oo"].postln;
				timeoffsets = score.collect({|event| event[0] }) ;
				if(clickCount == 2, { doubleClick = true }, { doubleClick = false });
				this.displayScore;
			 });

		});
		if(glmiddledown == false, {
			if(Rect(left, top+5, 4, length-5).contains(Point(x, y)), { 
				glmiddledown = true; 
				middledown = true; 
				array.swap(i, array.size-1);  
				//[\glmiddledown, "o"].postln;
				//[\name, name].postln;
				timeoffsets = score.collect({|event| event[0] }) ;
				selevent = nil; // set it to nil by default
				score[1..score.size-2].do({ | event, i |
					if(Rect(left, event[0]*scale, 7.5, 8.5).contains(Point(x, y)), { 
						selevent = i+1;
						//"I'm IN event nr: ".post; (i+1).postln;
					});
				});
				if(clickCount == 2, { doubleClick = true }, { doubleClick = false });
				this.displayScore;
			});
		});
		if(glbottomdown == false, {
			if(Rect(left-3, top-5+length, 10, 10).contains(Point(x, y)), { 
				glbottomdown = true; 
				bottomdown = true; 
				array.swap(i, array.size-1); 
				//[\glbottomdown, "o"].postln;
				//[\name, name].postln;
				timeoffsets = score.collect({|event| event[0] }) ;
				if(clickCount == 2, { doubleClick = true }, { doubleClick = false });
				this.displayScore;
			});
		});
		mousedownoffset = y; 
	}

	mouseMove { | ax, ay, operator=false |
		var x, y;
		x = ax-xtrans;
		y = ay-ytrans;
		if(gltopdown, {
			if(topdown, { 
				score[0][0] = (y/scale).round(0.01);
				if(operator, { 
					score.do({|event, i| 
						if(i!=(score.size-1), {event[0]=timeoffsets[i]+((y-mousedownoffset)/scale).round(0.01)})
					})
				}); //shift
				score.do({|event, i| 
					if(i != 0, { if( event[0] < (score[i-1][0]+(6/scale)), { event[0] = score[i-1][0]+(6/scale) }) }) 
				});
				score.do({|event, i| if( event[0] < 0, { event[0] = 0 }) }); // check top boundary
				top = score[0][0]*scale;
				bottom = score[score.size-1][0]*scale;
				length = bottom - top;
				this.displayScore;
				//"GL TOP DOWN".postln;
			});
		});
		if(glmiddledown, {
			if(middledown, { 
				//if(score[0][0] < 0, {score[0][0] = 0}); // check top boundary
				if(selevent.isNil, {
					score = score.collect({|event, i| event[0] = timeoffsets[i]+((y-mousedownoffset)/scale).round(0.01)}) ;
				},{
					score[selevent][0] = timeoffsets[selevent]+((y-mousedownoffset)/scale).round(0.01) ;
					score.do({|event, i| if(i != 0, { if( event[0] < (score[0][0]+(6/scale)), { event[0] = score[0][0]+(6/scale) }) }) });
					score.do({|event, i| 
						if(i != (score.size-1), {if( event[0] > (score[score.size-1][0]-(6/scale)), {event[0]=score[score.size-1][0]-(6/scale) }) }) 
					});
				});
				score.do({|event, i| if( event[0] < 0, { event[0] = 0 }) }); // check top boundary
				top = score[0][0] * scale;
				bottom = top+length;
				this.displayScore;
				//"GL MIDDLE DOWN".postln;		
			});
		});
		if(glbottomdown, {
			if(bottomdown, { 
				score[score.size-1][0] = (y/scale).round(0.01);
				if(operator, { score.do({|event, i| if(i!=0, {event[0]=timeoffsets[i]+((y-mousedownoffset)/scale).round(0.01)})})}); //shift
				score.do({|event, i| if(i != (score.size-1), {if( event[0] > (score[i+1][0]-(6/scale)), {event[0]=score[i+1][0]-(6/scale) }) }) });
				score.do({|event, i| if( event[0] < 0, {event[0]=0 }) }); // check top boundary

				top = score[0][0]*scale;
				bottom = score[score.size-1][0]*scale;
				length = bottom - top;
				this.displayScore;
				//"GL Bottom DOWN".postln;		
			});
		});
//		if(Rect(left, top, 10, 10).contains(Point(x, y)), {
//			"MoVE".postln;
//		});
		
		//[topdown, middledown, bottomdown].postln;
	}
	
	displayScore {
		scoreview = parent.scoreview;
		scorestring = score.asCompileString;
		scorestring = scorestring.replace(" ", "").replace("[[", "[").replace("]]", "]").replace("],", "]").replace("]", "]\n").replace(",", ", ");
		scoreview.string_(scorestring);		
	}
	
	mouseUp { | array, x, y |
		if(topdown || middledown || bottomdown, {
			"MOUSE UP ON DRONE : ".post; name.postln;
			//this.reParseScore;
			this.parseScore;
		});
		topdown = middledown = bottomdown = false;
		gltopdown = glmiddledown = glbottomdown = false;
	}

	parseScore {
		score = drone[\dronescore];
		[\parsing___, score].postln;
		Post << score;
		[\top1, top].postln;
		
		top = score[0][0] * scale; // scale
		[\top2, top].postln;

		if(score.last[1].asCompileString.contains("kill"), { // asCompileString because of Function
			"_____________ FOUND KILL".postln;
			bottom = score.last[0] * scale;
		},{
			score = score.add([50000, {name++".kill"}]); // add a kill after 50000 secs (~14 hours)
			bottom = 50000*scale;
		});
//		if(score.last[1].contains("kill"), {
//			"_____________ FOUND KILL".postln;
//			bottom = score.last[0].asFloat * scale;
//		},{
//			score = score.add([50000, {name++".kill"}]); // add a kill after 50000 secs (~14 hours)
//			bottom = 50000*scale;
//		});
		length = bottom - top;
		[\oooo_______, \bottom, bottom, \top, top, \length, length].postln;
	}
	
	reParseScore {
		var string, interpretedstring;
			"______xxxxxx reparsing score xxx_____".postln;
			[\name, name, \doubleClick, doubleClick].postln;
		if(doubleClick, { // double click means the view is open on top of droneview
			string = parent.scoreview.string; //scoreview.string;
			string = 	string.replace("]", "],").replace("\n", "");
			string = "["++string++"]";
			string = 	string.replace("],]", "]]");
			string.postln;
			interpretedstring = try{string.interpret};
			drone[\dronescore] = if(interpretedstring.isNil.not, { interpretedstring }, {drone[\dronescore]});
			[\score________111111111, drone[\dronescore]].postln;
		//	doubleClick = false;
			this.parseScore;
		});
	}
	
	draw {
//		"ERROR HERE?".postln;
//		
//		[\name, name, \left, left, \top, top, \length, length].postln;
		if(name == \global, {
			Color.white.set;		
			Pen.fillRect(Rect(left+xtrans, top+ytrans, 4, length)+0.5);
			Color.black.set;
			Pen.strokeRect(Rect(left+xtrans, top+ytrans, 4.5, length)+0.5);
		},{
			color.set;		
			Pen.fillRect(Rect(left+xtrans, top+ytrans, 4, length)+0.5);
			Color.black.set;
			Pen.strokeRect(Rect(left+xtrans, top+ytrans, 4.5, length)+0.5);
	
			color.set;		
			Pen.fillOval(Rect(left+xtrans-3, top-5+ytrans, 10, 10)+0.5);			Pen.fillOval(Rect(left+xtrans-3, bottom-5+ytrans, 10, 10)+0.5);	
			Color.black.set;
			Pen.strokeOval(Rect(left+xtrans-3, top-5+ytrans, 10, 10)+0.5);			Pen.strokeOval(Rect(left+xtrans-3, bottom-5+ytrans, 10, 10)+0.5);
		});
		// the events
		if(score.size>2,{
			score[1..score.size-2].do({ | event |
				//[\event__, event].postln;
				color.blend(Color(0.2, 0.2, 0.2)).set;
				Pen.fillRect(Rect(left+xtrans, event[0]*scale+ytrans, 4.5, 5.5)+0.5);
				Color.black.set;
				Pen.strokeRect(Rect(left+xtrans, event[0]*scale+ytrans, 4.5, 5.5)+0.5);
			});
		});
	}
	
//	remove {
//		"REMOVING +_____ IN DRONEGUI ________".postln;
//	}
}