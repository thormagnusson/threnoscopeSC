
DroneSpeakers {

	var <>nrChannels;
	var <>harmonicsColor, <>scaleOctaveColor, <>octaveColor, <>degreeColor, <>speakerColor;
	var hub, window, image, backgroundColor;
	var sd; // screendimensions
	var fundamental, scScale;
	//var strokeColor, strongStrokeColor;
	var done = false;
	var <>drawspeakers = true;
	var <>drawharmonics = true;
	var <>drawoctaves = false;
	var <>drawscale = false;
	var <scale, <tuning;

	*new { arg hub, channels, fundamental;
		^super.new.initDroneSpeakers(hub, channels, fundamental);
	}

	initDroneSpeakers { |arghub, channels=1, argfundamental|
		nrChannels = channels;
		hub = arghub;
		window = hub.window;
		sd = window.bounds.height;
		backgroundColor = hub.threnoscopeColor;
		fundamental = argfundamental;
		scale = Scale.minor; // xxx get this from the hub
		scScale = true; // it's a SuperCollider scale, vs. Scala (different in terms of idea of tuning)
		harmonicsColor = if(backgroundColor == Color.white, { Color.black }, { Color.white });
		scaleOctaveColor = Color.red;
		octaveColor = Color.green;
		speakerColor = if(backgroundColor == Color.white, { Color.black }, { Color.white });
		degreeColor = if(backgroundColor == Color.white, { Color.black }, { Color.white });
	}

	drawImg { |argtuning|
		var image = nil;
		"DRAWING IMAGE".postln;
		tuning = argtuning;
		image = Image.new(sd.asInteger, sd.asInteger);
		image.draw({
Ê Ê Ê Ê Ê Ê   Pen.color = backgroundColor;
			Pen.fillRect(Rect(0, 0, sd.asInteger, sd.asInteger));
			Color.black.alpha_(0.25).set;
			Pen.line(Point(sd-0.5, 0), Point(sd-0.5, sd));
			Pen.stroke;
			this.drawSpeakers.value;
			this.drawCircles.value;
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
		scScale = true;
		scale = Scale.newFromKey(argscale.asSymbol);
		if(scale.isNil, { 
			scScale = false; // as SC makes a distinction between a scale and tuning, but Scala not.
			scale = DroneScale.new(argscale) 
		}); // support of the Scala scales
		[\scale, scale].postln;
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
			{44} { // quadrophonic corner system
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point(0, 0.5), Point(sd, sd));
				Pen.line(Point(sd, 0), Point(0, sd));
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}
			}
			{5} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point((sd/2)+0.5, 0)); // top middle
				Pen.line(Point(190, 0), Point((sd/2)+0.5, (sd/2)+0.5)); // top left
				Pen.line(Point(710, 0), Point((sd/2)+0.5, (sd/2)+0.5)); // top right
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point(1000, 653)); // bottom right
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point(0, 615)); // bottom left
				Pen.strokeOval(Rect((sd/2)+0.5-10, (sd/2)+0.5-10, 20, 20));
				Pen.stroke;
				}
			}
			{6} {
				^{
				speakerColor.alpha_(0.2).set;
				Pen.line(Point((sd/6)+40.5, 0), Point((sd/2)+0.5, (sd/2)+0.5));
				Pen.line(Point(((sd/6)*5)-39.5, 0), Point((sd/2)+0.5, (sd/2)+0.5));
				Pen.line(Point((sd/6)+40.5, sd), Point((sd/2)+0.5, (sd/2)+0.5));
				Pen.line(Point(((sd/6)*5)-39.5, sd), Point((sd/2)+0.5, (sd/2)+0.5));
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point(0, (sd/2)+0.5)); // cross
				Pen.line(Point((sd/2)+0.5, (sd/2)+0.5), Point(sd, (sd/2)+0.5));
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


	drawCircles { |nrHarm = 30 |
		var expoctaves, circle;
		var scl = scale.ratios;

	// drawing the harmonics as an image rather than ovals (CPU going down from 23% to 2%)
	//if(image.isNil, {
		if(drawharmonics, {
			\DRAWING_Harmonics.postln;
			harmonicsColor.alpha_(0.2).set;
			nrHarm.do({arg ratio;
				circle = (fundamental * ratio).cpsmidi * ((sd/2)/90) - 80;
				Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
			});
		});

		// if(drawoctaves && drawscale.not, { // no need to draw octaves if already drawing scale
		if(drawoctaves, { // might need to draw octaves if scale drawn is NOT octave-repeating
			\DRAWING_Octaves.postln;
			octaveColor.alpha_(0.2).set;
			nrHarm.do({arg ratio;
				circle = fundamental*1.55 + (60 * (ratio))  ; // OCTAVES - WORKING !
				Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
			});
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

// WORKING BRILLIANTLY - July 2014
		if(drawscale, {
			var octaveRatio = scale.tuning.octaveRatio; // 2 in normal scales, but some are non-ooctave repeating
			var t = octaveRatio;
			expoctaves = ( { |o| octaveRatio = octaveRatio * t } ! 16 ).insert(0,t).insert(0,1).insert(0,0)-1;
		//	expoctaves = [0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536]-1;
		//	expoctaves = [0, 1, 3, 9, 27, 81, 72, 144, 288, 576, 1152, 2304, 4608, 9216, 18432, 36864, 73728, 147456, 294912]-1;
		
			if(scScale, {
				expoctaves.size.do({arg octave, i;
					scale.tuning_(tuning).ratios.do({arg degree;
						circle = (fundamental * (expoctaves[octave]+1)  * degree).cpsmidi * ((sd/2)/90) - 80; // HARMONICS - WORKING!
						if(degree == 1, {scaleOctaveColor.alpha_(0.4).set}, {degreeColor.alpha_(0.3).set});
						Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					});
				});
			}, {
				expoctaves.size.do({arg octave, i;
					scale.ratios.do({arg degree;
						circle = (fundamental * (expoctaves[octave]+1)  * degree).cpsmidi * ((sd/2)/90) - 80; // HARMONICS - WORKING!
						if(degree == 1, {scaleOctaveColor.alpha_(0.4).set}, {degreeColor.alpha_(0.3).set});
						Pen.strokeOval(Rect(sd/2-circle, sd/2-circle, circle*2, circle*2));
					});
				});
			});
		});
	}
}
