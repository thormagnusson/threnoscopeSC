
// Based on Batuhan Bozkurt's 2009 StageLimiter class

DroneLimiter { 
	classvar lmFunc, activeSynth;
	
	*activate {
		
		fork{
			lmFunc = { { 
				activeSynth = Synth(\droneLimiter, target: RootNode(Server.default), addAction: \addToTail) }.defer(0.01)};
			lmFunc.value;
			//CmdPeriod.add(lmFunc);
			"DroneLimiter active".postln;
		}
	}
	
	*deactivate {
		activeSynth.free;
		//CmdPeriod.remove(lmFunc);
		"DroneLimiter inactive...".postln;
	}
}
