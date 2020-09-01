
DroneSpeakersNew {

	var <>nrChannels;
	var <>harmonicsColor, <>scaleOctaveColor, <>octaveColor, <>degreeColor, <>speakerColor;
	var hub, window, image, backgroundColor;
	var sd; // screendimensions
	var fundamental;
	//var strokeColor, strongStrokeColor;
	var done = false;
	var <>drawspeakers = true;
	var <>drawharmonics = true;
	var <>drawoctaves = false;
	var <>drawscale = false;
	var <>drawnote = true;
	var <scale;

	*new { arg hub, channels, fundamental;
		^super.new.initDroneSpeakersNew(hub, channels, fundamental);
	}

	initDroneSpeakersNew { |arghub, channels=1, argfundamental|
		nrChannels = channels;
		hub = arghub;
		window = hub.window;
		sd = window.bounds.height;
		backgroundColor = window.view.background;
		fundamental = argfundamental;
		scale = hub.scale ? Scale.minor; // xxx get this from the hub
		harmonicsColor = if(backgroundColor == Color.white, { Color.black }, { Color.white });
		scaleOctaveColor = Color.red;
		octaveColor = Color.green;
		speakerColor = if(backgroundColor == Color.white, { Color.black }, { Color.white });
		degreeColor = if(backgroundColor == Color.white, { Color.black }, { Color.white });
		//strokeColor = if(backgroundColor == Color.white, {Color.black.alpha_(0.2)}, {Color.white.alpha_(0.2)});
		//strongStrokeColor = if(backgroundColor == Color.white, {Color.black}, {Color.white});
		[\scaleRatios, scale.ratios].postln;
	}

	/*
	draw {
		if(done.not, {
			if(image.isNil, {
				"DRAWING THIS ONLY ONCE _".postln;
				Color.black.alpha_(0.25).set;
				Pen.line(Point(sd-0.5, 0), Point(sd-0.5, sd));
				Pen.stroke;
				this.drawSpeakers.value;
				this.drawHarmonics.value;
			}, {
				hub.drones.drawView.backgroundImage_(image, 1, 1);
				done = true;
			});
		});
	}
	*/

	drawImg {
		var image = nil;
		image = Image(sd.asInteger, sd.asInteger);
		image.draw({
			"DRAWING THIS ONLY ONCE _".postln;
			Color.black.alpha_(0.25).set;
			Pen.line(Point(sd-0.5, 0), Point(sd-0.5, sd));
			Pen.stroke;
			this.drawSpeakers.value;
			this.drawHarmonics.value;
		});
		^image;
	}
// the following methods are used in DroneController

	speakers_ { | bool |
		drawspeakers = bool.asBoolean; // asBoolean allows for 0 or 1 to save typings
		done = false;
		image = nil;
	}

	harmonics_ { | bool |
		drawharmonics = bool.asBoolean;
		done = false;
		image = nil;
	}

	octaves_ { | bool | // experimental
		drawoctaves = bool.asBoolean;
		done = false;
		image = nil;
	}

	scale_ { | bool | // experimental
		drawscale = bool.asBoolean;
		done = false;
		image = nil;
	}

	setScale_ { | argscale |
		scale = Scale.newFromKey(argscale.asSymbol);
		if(scale.isNil, { scale = DroneScale.new(argscale) }); // support of the Scala scales
		done = false;
		image = nil;
	}

	drawSpeakers {
		if(drawspeakers, {
			switch(nrChannels)
			{1} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point((sd/2)+0.5, sd/2), Point((sd/2)+0.5, 0));
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}
			}
			{2} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point(0, (sd/2)+0.5), Point(sd, (sd/2)+0.5));
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}

			}
			{4} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point(0, (sd/2)+0.5), Point(sd, (sd/2)+0.5));
				Pen.line(Point((sd/2)+0.5, sd), Point((sd/2)+0.5, 0));
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}

			}
			{5} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point((sd/2)+0.5, 0));
				Pen.line(Point(200, 0), Point((sd/2)+0.5, (sd/2)+0.5));
				Pen.line(Point(800.5, 0), Point((sd/2)+0.5, (sd/2)+0.5));
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point(1000, 625.5)); // cross
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point(0, 625.5));
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}

			}
			{8} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point(0, (sd/2)+0.5), Point(sd, (sd/2)+0.5));
				Pen.line(Point((sd/2)+0.5, sd), Point((sd/2)+0.5, 0));
				Pen.line(Point(0, 0.5), Point(sd, sd));
				Pen.line(Point(sd, 0), Point(0, sd));
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}
			}
		});
	}


	drawHarmonics { |nrHarm = 50|
		var expoctaves, circle;

		// drawing the harmonics as an image rather than ovals (CPU going down from 23% to 2%)
		//if(image.isNil, {
			if(drawharmonics, {
				\DRAWING_Harmonics.postln;
				harmonicsColor.alpha_(0.2).set;
				nrHarm.do({arg ratio;
					//circle = (fundamental * ratio).cpsmidi * ((sd/2)/40) - 220;
					circle = (ratio*fundamental).cpsmidi*11 - 220;
					//Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					Pen.line(Point(0, (sd-circle).round(1)+0.5), Point(400, (sd-circle).round(1)+0.5));// Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
				});
				Pen.stroke;
			});

			if(drawnote, {
				var notes = [ 33.midicps, 35.midicps, 36.midicps, 40.midicps ]; // these notes should be retrieved from the keyboard playing them
				var freqs = notes.nearestInList( fundamental*scale.ratios );
				[\scaleratios, scale.ratios].postln;
				\DRAWING_Note.postln;
				notes.do({ arg freq, i;
					var left = rrand(300, 420);
					var right = rrand(550, 750);
					Color.rand.alpha_(0.8).set;
					
					nrHarm.do({arg ratio;
						//circle = (fundamental * ratio).cpsmidi * ((sd/2)/40) - 220;
						//circle = (ratio*freq).cpsmidi*11 - 220;
						circle = (ratio*freqs[i]).cpsmidi*11 - 220;
						// Pen.strokeOval( Rect(sd/2-circle, sd/2-circle, //circle*2, circle*2) );
						Pen.line(Point(left, (sd-circle).round(1)+0.5), Point(right, (sd-circle).round(1)+0.5));// Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					});
					Pen.stroke;
				});
			});

			// if(drawoctaves && drawscale.not, { // no need to draw octaves if already drawing scale
			if(drawoctaves, { // might need to draw octaves if scale drawn is NOT octave-repeating
				\DRAWING_Octaves.postln;
				octaveColor.alpha_(0.2).set;
				nrHarm.do({arg ratio;
					circle = fundamental*2.6 + (132 * (ratio))  ; // OCTAVES - WORKING !
					//Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					Pen.line(Point(0, (sd-circle)+0.5), Point(600, (sd-circle)+0.5));// Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
				});
				Pen.stroke;
			});

	/*
	// this does not work for scales that are non-ocatave repeating, such as Bohlen Pierce
			if(drawscale, {
				\DRAWING_Scale.postln;
				expoctaves = [0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536]-1;
				expoctaves.size.do({arg ratio, i;
					scale.ratios.do({arg degree;
						circle = (fundamental*(expoctaves[ratio]+1)  * degree).cpsmidi * ((sd/2)/90) - 80; // HARMONICS - WORKING!
						if(degree == 1, {strongStrokeColor.set}, {strokeColor.alpha_(0.4).set});
						Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					});
				});
			});
	*/
	
	
			if(drawscale, {
				//var octaveRatio = scale.tuning.octaveRatio; // 2 in normal scales, but some are non-ooctave repeating
				var octaveRatio = 2; // 2 in normal scales, but some are non-ooctave repeating
				var t = octaveRatio;
				expoctaves = ( { |o| octaveRatio = octaveRatio * t } ! 16 ).insert(0,t).insert(0,1).insert(0,0)-1;
			//	expoctaves = [0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536]-1;
			//	expoctaves = [0, 1, 3, 9, 27, 81, 72, 144, 288, 576, 1152, 2304, 4608, 9216, 18432, 36864, 73728, 147456, 294912]-1;
				//Pen.translate(0.5, 0.5);
				expoctaves.size.do({arg octave, i;
					scale.ratios.do({arg degree;
						//[\degree, degree].postln;
						//[\expoctaves, degree].postln;
						
						//circle = (fundamental * (expoctaves[octave]+1)  * degree).cpsmidi * ((sd/2)/40) - 220; // HARMONICS - WORKING!
						// circle = (fundamental * (expoctaves[octave]+1)  * degree).cpsmidi * 11 - 352; // HARMONICS - WORKING!
						circle = (55 * (expoctaves[octave]+1)  * degree).cpsmidi * 11 - 352; // HARMONICS - WORKING! - hard coding to 55 Hz
						
						//circle = (fundamental * ratio).cpsmidi * ((sd/2)/40) - 220;
						//circle = (ratio*fundamental).cpsmidi*11 - 220;
						//[\circle, circle].postln;
						//if(degree == 1, {strongStrokeColor.set}, {strokeColor.alpha_(0.4).set});
						if(degree == 1, {Color.red.set}, {degreeColor.alpha_(0.3).set});
						//Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
						
					Pen.line(Point(110, (sd-circle).round(1)+0.5), Point(500, (sd-circle).round(1)+0.5));// Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					//Pen.line(Point(110, (sd-circle)+0.5), Point(500, (sd-circle)+0.5));// Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					Pen.stroke;
					});
				});
			});
	
			image = Image.fromWindow(hub.window, Rect(0, 0, hub.window.bounds.height, hub.window.bounds.height));
		//});
	}
}
