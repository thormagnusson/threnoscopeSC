# threnoscopeSC
SuperCollider source code for the Threnoscope

Author: Thor Magnusson

Further information about the Threnoscope can be found here:

https://thormagnusson.github.io/threnoscope/

Plese note that [sc3-plugins](https://supercollider.github.io/sc3-plugins/) must be installed in order for the Threnoscope to work.

Enjoy!

## Installation Guide
The procedure is the same on MacOs, Linux and Windows. 
For MacOs Users, a standalone App is also available at: https://thormagnusson.github.io/threnoscope/

Inside Supercollider type and evaluate:

```SuperCollider
Quarks.install("https://github.com/thormagnusson/threnoscopeSC.git");
```
Recompile the class library.

Inside SuperCollider type and evaluate: 

```SuperCollider
s.boot;
ThrenoScope.new(2);
```

If everything works correctly this should launch the Threnoscope!
Type this line inside the Threnoscope editor to check if everything works:

```
~drones.createDrone(\saw, 2, 3);
```

Now drone on!

Go to:
https://thormagnusson.github.io/threnoscope/ 

for more documentation, reference and tutorials! 
