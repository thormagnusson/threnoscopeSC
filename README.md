# threnoscopeSC
SuperCollider source code for the Threnoscope

Author: Thor Magnusson

Further information about the Threnoscope can be found here:

https://thormagnusson.github.io/threnoscope/

Plese note that [sc3-plugins](https://supercollider.github.io/sc3-plugins/) must be installed in order for the Threnoscope to work.

Enjoy!

## Installation Guide
The procedure is the same on MacOs, Linux and Windows. 

Inside Supercollider type:

```SuperCollider
Quarks.install(‘https://github.com/thormagnusson/threnoscopeSC.git’);
```

Open the User Support Directory. 
Create a folder named **threnoscope**. 
Inside the threnoscope folder create three folders:
- scl
- scl_user
- samples

Inside the samples folder create an empty file named **_samples.scd** (include the underscore). 

Recompile the class library.

Inside Supercollider type and evaluate:

```SuperCollider
s.boot;
ThrenoScope.new(2);
```

If everything works correctly this should launch the Threnoscope!

Go to:
https://thormagnusson.github.io/threnoscope/ 

for documentation, reference and tutorials! 
