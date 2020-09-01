
/*

check the use of first order one pass filter:
OnePole.ar( <your signal>, ( -2pi * (freq / SampleRate.ir) ).exp );

i.e. 6db filtering
LPF

*/

DroneSynths {

	classvar <sampleDict;
	classvar hub;
	
	*new { arg loadsamples, hub;
		^super.new.initDroneSynths(loadsamples, hub);
	}

	initDroneSynths {arg loadsamples, arghub;
		
		hub = arghub;
		
		this.initSampleDict;

		SynthDef(\channelrouter8_2, {arg in=0, out=6;
			Out.ar(out, In.ar(in, 1));
		}).add;

		SynthDef(\channelrouter8_1, {arg in=0, out=6;
			Out.ar(out, In.ar(in)!2);
		}).add;

		/*
		SynthDef(\atom, {| atomnumber, freq, amp, harmonics, probability, rate, pitch, loopStart, loopEnd, snapFreqs, snapPull|
			SendReply.kr(Changed.kr(freq), '/atom',  [atomnumber, 0, freq]);
			SendReply.kr(Changed.kr(amp), '/atom',  [atomnumber, 1, amp]);
			SendReply.kr(Changed.kr(harmonics), '/atom', [atomnumber, 2, harmonics]);
			SendReply.kr(Changed.kr(probability), '/atom', [atomnumber, 3, probability]);
			SendReply.kr(Changed.kr(rate), '/atom', [atomnumber, 4, rate]);
			SendReply.kr(Changed.kr(pitch), '/atom', [atomnumber, 5, pitch]);
			SendReply.kr(Changed.kr(loopStart), '/atom', [atomnumber, 6, loopStart]);
			SendReply.kr(Changed.kr(loopEnd), '/atom', [atomnumber, 7, loopEnd]);

			SendReply.kr(Changed.kr(snapFreqs), '/atom', [atomnumber, 8, snapFreqs]);
			SendReply.kr(Changed.kr(snapPull), '/atom', [atomnumber, 9, snapPull]);
		}).add;
		*/

		// the Atom Synth works only with Chris Kiefer's Atom synthesis system (all OSC out)
		SynthDef(\atom, {| atomnumber, freq, amp, harmonics, probability, rate, pitch, loopStart, loopEnd, snapFreqs = #[ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ], snapPull|
			SendReply.kr(Changed.kr(freq), '/atom',  [atomnumber, 0, freq]);
			SendReply.kr(Changed.kr(amp), '/atom',  [atomnumber, 1, amp]);
			SendReply.kr(Changed.kr(harmonics), '/atom', [atomnumber, 2, harmonics]);
			SendReply.kr(Changed.kr(probability), '/atom', [atomnumber, 3, probability]);
			SendReply.kr(Changed.kr(rate), '/atom', [atomnumber, 4, rate]);
			SendReply.kr(Changed.kr(pitch), '/atom', [atomnumber, 5, pitch]);
			SendReply.kr(Changed.kr(loopStart), '/atom', [atomnumber, 6, loopStart]);
			SendReply.kr(Changed.kr(loopEnd), '/atom', [atomnumber, 7, loopEnd]);

			SendReply.kr(Changed.kr(snapFreqs.sum), '/atom', [atomnumber, 8] ++ snapFreqs);
			SendReply.kr(Changed.kr(snapPull), '/atom', [atomnumber, 9, snapPull]);
		}).add;



		SynthDef(\instr, {arg buffer, out=0, freq=440, midinote=69, gate=1, startLoop=43100, endLoop=161200, t_trig=1, attack=1, ratex=1, 
			dir= 1, amp=0.42, env=#[3,3], harmonics=4, resonance=1, resamp=0, doneAction=2;
		
			var signal, delay, tr, att, envl, onsetenvtime;
			// rate supports microtunings -> midinote is the reference pitch of the sample
		//	var rate = Lag.kr((freq.cpsmidi - midinote ).midiratio, 0.5);
			// midinote is the reference frequency, i.e. the freq of the original sample (whether in tune or not)
			var rate = (freq.cpsmidi - midinote ).midiratio;
			var looplength = ((endLoop-startLoop)/SampleRate.ir)/rate;
			var trianglelength = looplength; // in case I want to shorten the triangle (e.g., looplength/2)
			var changetr =  ( Changed.kr(freq)-1).abs; // drops down to zero if there is a change
		
		//		rate.poll;
		
			// sample playback mechanism
			att = PlayBuf.ar(1, buffer, rate, 1, loop:0)*EnvGen.ar(Env.perc(0.0000001, 2, 1, -4))*attack; // attack
			//tr = TDelay.kr(Impulse.kr(looplength.reciprocal), looplength-(trianglelength/2)).poll;
			tr = TDelay.kr(TDuty.kr(looplength, (changetr-1).abs), looplength-(trianglelength/2));
		
		
			signal = LoopBuf.ar(1, buffer, rate, changetr, startLoop, startLoop, endLoop, 2);
			//delay = LoopBuf.ar(1, buffer, rate, changetr, 0, startLoop+22000, endLoop+22000, 2);
			delay = DelayN.ar(LoopBuf.ar(1, buffer, rate*dir, changetr, startLoop, startLoop, endLoop, 2), trianglelength/2, trianglelength/2);
			signal = XFade2.ar(delay, signal+att, (EnvGen.ar((Env.triangle(trianglelength, -2)), tr+(changetr-1)) +1), Lag.kr(amp, 3));
		
			// threnoscope system
			signal = LPF.ar(signal, freq * harmonics);
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));
			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}).add;
		
		SynthDef(\droneLimiter, {
			var input = In.ar(0, 2);
		//	input = Select.ar(CheckBadValues.ar(input, 0, 0), [input, DC.ar(0), DC.ar(0), input]);
			ReplaceOut.ar(0, Limiter.ar(input, 0.8)) ;
		}).add;

		SynthDef(\dronesaw, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(Saw.ar(freqmodosc * freqmodline * freq, Lag.kr(amp, 3) * 0.33) +
					Saw.ar([freq, freq+detune], Lag.kr(amp, 3) * 0.33)).sum,
				freq * harmonics);

	//		signal = BPeakEQ.ar(signal, MouseX.kr(55, 5555).poll, 0.5, MouseY.kr(0, 24));
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			// DetectSilence.ar(signal, 0.001, doneAction:2); // security so synths don't pile up
			Out.ar(out, signal);
		}, [0, 0.4]).add;

		SynthDef(\eliane, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

//			signal = LPF.ar(
//					(Saw.ar(freqmodosc * freqmodline * freq, Lag.kr(amp, 3) * 0.33) +
//					Saw.ar([freq, freq+detune], Lag.kr(amp, 3) * 0.33)).sum,
//				freq * harmonics);

			signal = ({|i| var x, y, f; x=i+1;
				y=LFNoise2.ar(0.1);
				f=freqmodosc * freqmodline * freq*(x/2)+y.range(-2,2);
				LPF.ar(Saw.ar(f,Lag.kr(amp, 3)*y*(0.75/(x*0.3))),f*harmonics)}!10).sum; // 10 saw waves

	//		signal = BPeakEQ.ar(signal, MouseX.kr(55, 5555).poll, 0.5, MouseY.kr(0, 24));
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			// DetectSilence.ar(signal, 0.001, doneAction:2); // security so synths don't pile up
			Out.ar(out, signal);
		}, [0, 0.4]).add;
		
		
		SynthDef(\dronenoise, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = HPF.ar(LPF.ar(
					(WhiteNoise.ar(Lag.kr(amp, 3) * 0.63) +
					BrownNoise.ar(Lag.kr(amp, 3) * 0.63 * freqmodosc)),
				(freq * harmonics).clip(20, 16000)), freq.clip(20, 16000));

	//		signal = BPeakEQ.ar(signal, MouseX.kr(55, 5555).poll, 0.5, MouseY.kr(0, 24));
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			// DetectSilence.ar(signal, 0.001, doneAction:2); // security so synths don't pile up
			Out.ar(out, signal);
		}, [0, 0.4]).add;


		SynthDef(\droneklank, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar( 
			 	DynKlank.ar(`[
			 	[freq, 
			 	freq*LFNoise2.ar(0.1).range(1.9, 2.1), 
			 	freq*LFNoise2.ar(0.1).range(2.9, 3.1), 
			 	freq*LFNoise2.ar(0.1).range(3.9, 4.1),
			 	freq*LFNoise2.ar(0.1).range(4.9, 5.1),
			 	freq*LFNoise2.ar(0.1).range(5.9, 6.1),
			 	freq*LFNoise2.ar(0.1).range(6.9, 7.1),
			 	freq*LFNoise2.ar(0.1).range(7.9, 8.1)
			 	], 
			 	nil, [0.7, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7]], 
			 	LPF.ar((WhiteNoise.ar(0.003)+WhiteNoise.ar(0.003))*amp, freq*4)), (freq * harmonics).clip(20, 16000));

	//		signal = BPeakEQ.ar(signal, MouseX.kr(55, 5555).poll, 0.5, MouseY.kr(0, 24));
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			// DetectSilence.ar(signal, 0.001, doneAction:2); // security so synths don't pile up
			Out.ar(out, signal);
		}, [0, 0.4]).add;


		SynthDef(\dronegendy, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2, range = 20;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

			signal = LPF.ar(
				Gendy1.ar(2,3,minfreq:freq,maxfreq:freq,durscale:0.4,initCPs:40)
			 , (freq * harmonics).clip(20, 16000))
			 * harmonics.linexp(1, 15, 1, 0.1);

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}, [0, 0.4]).add;

		


/*
		SynthDef(\dronenoise, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(WhiteNoise.ar(Lag.kr(amp, 3) * 0.33) +
					BrownNoise.ar(Lag.kr(amp, 3) * 0.33)).sum,
				freq * harmonics);

	//		signal = BPeakEQ.ar(signal, MouseX.kr(55, 5555).poll, 0.5, MouseY.kr(0, 24));
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			// DetectSilence.ar(signal, 0.001, doneAction:2); // security so synths don't pile up
			Out.ar(out, signal);
		}, [0, 0.4]).add;
*/		
/*
a = Synth(\eliane, [\freq, 111])
a.set(\freq, 111)
a.set(\harmonics, 11)

*/
//		SynthDef(\dronesaw, {arg freq=110, harmonics=2, oscfreq=1, out=0, amp=0.42;
//			var signal;
//			signal = LPF.ar(Saw.ar(freq, amp), freq*harmonics);
//			Out.ar(out, signal);
//		}).add;

/*
SynthDef(\instr, {arg buffer, out=0, freq=440, midinote=69, gate=1, startLoop=43100, endLoop=161200, t_trig=1, attack=1, ratex=1, dir= 1, amp=0.42, env=#[3,3], harmonics=4, resonance=1, resamp=0;

	var signal, delay, tr, att, envl, onsetenvtime;
	// rate supports microtunings -> midinote is the reference pitch of the sample
//	var rate = Lag.kr((freq.cpsmidi - midinote ).midiratio, 0.5);
	var rate = (freq.cpsmidi - midinote ).midiratio;
	var looplength = ((endLoop-startLoop)/44100)/rate;
	var trianglelength = looplength; // in case I want to shorten the triangle (e.g., looplength/2)
	// var looptrigger = (Changed.kr(freq) + Changed.kr(startLoop) + Changed.kr(endLoop)).poll;
	// var changetr =  ( Changed.kr(freq)-1).abs; // drops down to zero if there is a change

	onsetenvtime= ((endLoop-1000)/44100).max(2.5); // if endloop is less than 2.5 sec, then use that
	// sample playback mechanism
	att = PlayBuf.ar(1, buffer, rate, 1, loop:0)*EnvGen.ar(Env.perc(0.0000001, 2, 1, -4))*attack; // attack
	tr = TDelay.kr(Impulse.kr(looplength.reciprocal), looplength-(trianglelength/2));
	signal = LoopBuf.ar(1, buffer, rate, 1, startLoop, startLoop, endLoop, 2);
	//delay = LoopBuf.ar(1, buffer, rate*dir, 1, 0, startLoop, endLoop, 2);
	delay = DelayN.ar(LoopBuf.ar(1, buffer, rate*dir, 1, startLoop, startLoop, endLoop, 2), trianglelength/2, trianglelength/2);
	signal = XFade2.ar(delay, signal+att, (EnvGen.ar((Env.triangle(trianglelength, -2)), tr)+1).poll, Lag.kr(amp, 3));
	//signal = XFade2.ar(delay, signal+att, (MouseX.kr(-1, 1)).poll, Lag.kr(amp, 3));

	// threnoscope system
	signal = LPF.ar(signal, freq * harmonics);
	signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));
	envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:2);
	signal = signal * envl * AmpComp.kr(freq, 55);
	Out.ar(out, signal);
}).add;

*/

		SynthDef(\droneinstr, {arg buffer, out=0, freq=440, midinote=69, gate=1, startLoop=43100, endLoop=161200, t_trig=1, attack=1, ratex=1, 
			dir= 1, amp=0.42, env=#[3,3], harmonics=4, resonance=1, resamp=0, doneAction=2;
		
			var signal, delay, tr, att, envl, onsetenvtime;
			// rate supports microtunings -> midinote is the reference pitch of the sample
		//	var rate = Lag.kr((freq.cpsmidi - midinote ).midiratio, 0.5);
			// midinote is the reference frequency, i.e. the freq of the original sample (whether in tune or not)
			var rate = (freq.cpsmidi - midinote ).midiratio;
			var looplength = ((endLoop-startLoop)/SampleRate.ir)/rate;
			var trianglelength = looplength; // in case I want to shorten the triangle (e.g., looplength/2)
			var changetr =  ( Changed.kr(freq)-1).abs; // drops down to zero if there is a change
		
		//		rate.poll;
		
			// sample playback mechanism
			att = PlayBuf.ar(1, buffer, rate, 1, loop:0)*EnvGen.ar(Env.perc(0.0000001, 2, 1, -4))*attack; // attack
			//tr = TDelay.kr(Impulse.kr(looplength.reciprocal), looplength-(trianglelength/2)).poll;
			tr = TDelay.kr(TDuty.kr(looplength, (changetr-1).abs), looplength-(trianglelength/2));
		
		
			signal = LoopBuf.ar(1, buffer, rate, changetr, startLoop, startLoop, endLoop, 2);
			//delay = LoopBuf.ar(1, buffer, rate, changetr, 0, startLoop+22000, endLoop+22000, 2);
			delay = DelayN.ar(LoopBuf.ar(1, buffer, rate*dir, changetr, startLoop, startLoop, endLoop, 2), trianglelength/2, trianglelength/2);
			signal = XFade2.ar(delay, signal+att, (EnvGen.ar((Env.triangle(trianglelength, -2)), tr+(changetr-1)) +1), Lag.kr(amp, 3));
		
			// threnoscope system
			signal = LPF.ar(signal, freq * harmonics);
			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));
			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}).add;

		SynthDef(\dronesine, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.3, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);
//
//			signal = LPF.ar(
//					(SinOsc.ar(freqmodosc * freqmodline * freq, pi.rand, Lag.kr(amp, 3) * 0.33) +
//					SinOsc.ar([freq, freq+detune], [pi.rand, pi.rand], Lag.kr(amp, 3) * 0.33)).sum,
//				freq * harmonics);
			signal =
					(SinOsc.ar(freqmodosc * freqmodline * freq, pi.rand, Lag.kr(amp, 3) * 0.33) +
					SinOsc.ar([freq, freq+detune], [pi.rand, pi.rand], Lag.kr(amp, 3) * 0.33)).sum;

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}, [0.2, 0.2]).add;



		SynthDef(\dronetri, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(LFTri.ar(freqmodosc * freqmodline * freq, 0, Lag.kr(amp, 3) * 0.13) +
					LFTri.ar([freq, freq+detune], 0, Lag.kr(amp, 3) * 0.13)).sum,
				freq * harmonics);

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}, [0.4, 0.4]).add;

		SynthDef(\dronecub, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(LFCub.ar(freqmodosc * freqmodline * freq, 0, Lag.kr(amp, 3) * 0.15) +
					LFCub.ar([freq, freq+detune], 0, Lag.kr(amp, 3) * 0.15)).sum,
				freq * harmonics);

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}, [0.4, 0.4]).add;


		SynthDef(\dronepulse, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(LFPulse.ar(freqmodosc * freqmodline * freq, 0, 0.5, Lag.kr(amp, 3) * 0.2) +
					LFPulse.ar([freq, freq+detune], 0, 0.5, Lag.kr(amp, 3) * 0.2)).sum,
				freq * harmonics);

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}, [0.4, 0.4]).add;


		SynthDef(\droneformant, {arg freq=110, formfreq=3, bwfreq=2, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;

			var signal, freqmodosc, freqmodline, envl;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(Formant.ar(freqmodosc * freqmodline * freq, formfreq*freq, bwfreq*freq, Lag.kr(amp, 3) * 0.13) +
					Formant.ar([freq, freq+detune],formfreq*freq, bwfreq*freq, Lag.kr(amp, 3) * 0.13)).sum,
				freq * harmonics);

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal!2);
		}, [0.4, 12, 12, 0.4]).add;


		SynthDef(\dronepad, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.3, gate=1,
			env=#[3,3], dep=1, arr=2, time=10, fgate=0, detune=0, resonance=1, resamp=0, doneAction=2;
			var signal, freqmodosc, freqmodline, envl, amps;

			freqmodosc = SinOsc.ar(oscfreq, pi.rand, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);
			amps = [0.6134, 0.5103, 0.3041, 0.2216, 0.4175, 0.1082, 0.067, 0.0773, 0, 0.01546];
			signal = amps.collect({|amp, i| SinOsc.ar([freq *(i+1), freq *(i+1) +Rand(1, 3.8)], 0, amp*0.1) }).sum
			+ amps.collect({|amp, i| SinOsc.ar([freqmodosc * freqmodline * freq *(i+1), freq *(i+1) +Rand(1, 3.8)], 0, amp*0.1) }).sum;

			signal = BPeakEQ.ar(signal, freq*Lag.kr(resonance, 1), 1, Lag.kr(resamp, 1));

			envl = EnvGen.kr(Env.asr(env[0], 1, env[1], 0), gate, doneAction:doneAction);
			signal = signal * envl * AmpComp.kr(freq, 55);
			Out.ar(out, signal);
		}).add;



		SynthDef(\dronemixer2, { Out.ar(100, In.ar(0, 2).sum) }).add;
		SynthDef(\dronemixer4, { Out.ar(100, In.ar(0, 4).sum) }).add;
		SynthDef(\dronemixer5, { Out.ar(100, In.ar(0, 5).sum) }).add;
		SynthDef(\dronemixer8, { Out.ar(100, In.ar(0, 8).sum) }).add;


	}

	*bufferPath{arg instr, freq;
		var nearestFreq, path;
		/*
		The Logic:
		1) The sampleDict contains the list of samples, their actual frequencies (might be detuned instrument), path and good loopStart and loopEnd.
		2) The sample with the nearest frequency is found for the actual frequency to be played. That frequency (item in the dict) has an associated sample.
		3) When the actual freq is passed to the synth, this is referenced to the desired frequency, and the correct frequency/rate calculated (play rate of the LoopBuf).
		*/
		[\freq, freq, \sampledictfreqs, sampleDict[instr].keys.asArray.asFloat.sort].postln;
		nearestFreq = freq.nearestInList(sampleDict[instr].keys.asArray.asFloat.sort);
	//	path = sampleDict[instr][nearestFreq.asSymbol][\path];
		// STANDALONE
		path = hub.appPath ++"/samples/"++ sampleDict[instr][nearestFreq.asSymbol][\path];
		// CLASSES
		path = Platform.userAppSupportDir ++"/threnoscope/samples/"++ sampleDict[instr][nearestFreq.asSymbol][\path];

		[\nearestFreq, nearestFreq, \sample, path].postln;
		^path;
	}

	*noteData {arg instr, freq;
		var thisNote, startLoop, endLoop;
		thisNote = freq.nearestInList(sampleDict[instr].keys.asArray.asFloat.sort);
		startLoop = sampleDict[instr][thisNote.asSymbol][\startPos];
		endLoop = sampleDict[instr][thisNote.asSymbol][\endPos];
	//	[\thisNote, thisNote, \startLoop, startLoop, \endLoop, endLoop].postln;
		^[thisNote, startLoop, endLoop];
	}


	*nearestFreq {arg instr, freq;
		^freq.nearestInList(sampleDict[instr].keys.asArray.asFloat.sort);
	}


	// here instruments and their frequencies and disk paths are listed
	initSampleDict {
		var file, string;
	//	file = File("sounds/threnoscope/_samples.scd","r");
	
		//file = File("/Users/thm21/Library/Application Support/SuperCollider/sounds/threnoscope/_samples.scd","r");
		
		[\HUBAPPPATH, hub.appPath].postln;

		// STANDALONE 
	  	// file = File(hub.appPath++"/samples/_samples.scd","r");
		
		// RUNNING TS AS CLASSES IN SC
		file = File(Platform.userAppSupportDir++"/threnoscope/samples/_samples.scd","r");

		

	//	file = File(hub.appPath++"/sounds/threnoscope/_samples.scd","r");
		string = file.readAllString;
		sampleDict = string.interpret;
		file.close;
		
		~sd = sampleDict;
		[\sampleDict, sampleDict].postln;


//Drone
//sampleDict = ();
//
//// piano
//sampleDict.add(\piano -> ());
//sampleDict[\piano].add(\55 -> ().add(\midinote -> 33).add(\path -> "sounds/threnoscope/55hz.aiff"));
//sampleDict[\piano].add(\110 -> ().add(\midinote -> 45).add(\path -> "sounds/threnoscope/110hz.aiff"));
//sampleDict[\piano].add(\220 -> ().add(\midinote -> 57).add(\path -> "sounds/threnoscope/220hz.aiff"));
//sampleDict[\piano].add(\440 -> ().add(\midinote -> 69).add(\path -> "sounds/threnoscope/440hz.aiff"));
//
//// organ
//sampleDict.add(\organ -> ());
//sampleDict[\organ].add(\55 -> "sounds/threnoscope/440hz.aiff");
//sampleDict[\organ].add(\110 -> "sounds/threnoscope/440hz.aiff");
//sampleDict[\organ].add(\220 -> "sounds/threnoscope/440hz.aiff");

		// etc
	}
}

/*


// with a gate and a freq line and a freq osc:

// choose whether one want's a line or osc

		SynthDef(\dronesaw, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			envtime=10, dep=1, arr=2, time=10, fgate=0, detune=0;

			var signal, freqmodosc, freqmodline, env;

			freqmodosc = SinOsc.ar(oscfreq, 0, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(Saw.ar(freqmodosc * freqmodline * freq, Lag.kr(amp, 3) * 0.5) +
					Saw.ar([freq, freq+detune], Lag.kr(amp, 3) * 0.35)).sum,
				freq * harmonics);

			env = EnvGen.kr(Env.asr(envtime, 1, envtime), gate, doneAction:2);
			signal = signal * env;
			Out.ar(out, signal!2);
		}, [0.2, 0.2]).add;


a = Synth(\dronesaw,[\amp, 0.5])

a = Synth(\dronesaw,[\amp, 0.5])

a.set(\fgate, 1)
a.set(\dep, 1)
a.set(\arr, 1.5)
a.set(\time, 13)
a.set(\fgate, 0)
a.set(\fgate, 1)

a.set(\oscfreq, 1)
a.set(\oscamp, 0.015)
a.set(\oscamp, 0)

a.set(\detune, 0.7)



		SynthDef(\dronesine, {arg freq=110, harmonics=2, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1,
			envtime=10, dep=1, arr=2, time=10, fgate=0, detune=0;

			var signal, freqmodosc, freqmodline, env;

			freqmodosc = SinOsc.ar(oscfreq, 0, oscamp, 1);
			freqmodline = EnvGen.ar(Env.new([dep,arr],[time]), fgate);

		//	signal = LPF.ar(Saw.ar(freqmodosc * freqmodline * freq, amp), freq * harmonics);

			signal = LPF.ar(
					(SinOsc.ar(freqmodosc * freqmodline * freq, 0, Lag.kr(amp, 3) * 0.5) +
					SinOsc.ar([freq, freq+detune], 0, Lag.kr(amp, 3) * 0.35)).sum,
				freq * harmonics);

			env = EnvGen.kr(Env.asr(envtime, 1, envtime), gate, doneAction:2);
			signal = signal * env;
			Out.ar(out, signal!2);
		}, [0.2, 0.2]).add;

a = Synth(\dronesine,[\amp, 0.5])

a.set(\freq, 1333)

a.set(\fgate, 1)
a.set(\dep, 1)
a.set(\arr, 1)
a.set(\time, 1)
a.set(\fgate, 0)
a.set(\fgate, 1)

a.set(\oscfreq, 0.01)
a.set(\oscamp, 0.005)
a.set(\oscamp, 0)

a.set(\detune, 1)



*/






/*
// SinOscFB (the feedback gives harmonics moving from 0 to 2 in the iphase)

SynthDef(\dronesinefb, {arg freq=110, iphase=0.9, harmonics=20, oscfreq=0.1, oscamp=0, out=0, amp=0.42, gate=1, atttime=1, dectime=1, freqmoddep=1, freqmodarr=2, freqmodtime=10, freqmodlinegate=0, phaseoscfreq=0.1, phaseoscamp=0, phasemoddep=1, phasemodarr=2, phasemodtime=10, phasemodlinegate;
	var signal, freqmodosc, freqmodline, phasemodosc, phasemodline, env;
	freqmodosc = SinOsc.ar(oscfreq, 0, oscamp, 1);
	freqmodline = EnvGen.ar(Env.new([freqmoddep,freqmodarr],[freqmodtime]), freqmodlinegate);
	phasemodosc = SinOsc.ar(phaseoscfreq, 0, phaseoscamp, 1).poll;
	phasemodline = EnvGen.ar(Env.new([phasemoddep, phasemodarr],[phasemodtime]), phasemodlinegate);
	signal = LPF.ar(SinOscFB.ar(freqmodosc * freqmodline * freq, iphase* phasemodosc , amp), freq * harmonics);
	env = EnvGen.ar(Env.asr(atttime, 1, dectime), gate, doneAction:2);
	signal = signal * env;
	Out.ar(out, signal);
}).add;

b = Synth(\dronefsine,[\amp, 0.5])

a = Synth(\dronesinefb)
a.set(\freqmodlinegate, 1)
a.set(\freqmoddep, 2)
a.set(\freqmodarr, 1)
a.set(\freqmodtime, 1)
a.set(\freqmodlinegate, 0)
a.set(\freqmodlinegate, 1)

a.set(\phaseoscfreq, 0.15)
a.set(\phaseoscamp, 0.4)
a.set(\oscamp, 0.1)
a.set(\iphase, 0.1)


*/