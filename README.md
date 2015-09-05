# Android app
Android companion app for Pebble™

Made for my master's thesis at the University of Ghent, Belgium.

The Android app has the following features:
- TODO

Workflow:
- TODO

Remarks:
- Don't look too much into the commit messages. Since this project has been implemented by a single person, Git versioning isn't really needed but it was useful as a backup cloud service out of safety or before major changes.

- When exercising the up/down/left/right gesture, the start position is the position where the wearable display looks towards the sky and is horizontally aligned.

- The below section in the thesis book, under section 5.2.1 (page 38) is out-of-date:
START QUOTE
Wanneer de Application Data van de mobiele app werd gewist, heeft de mobiele app geen weet van 
de systemID’s van elke Action Device. De gebruiker moet in dat geval een eerste verbinding leggen
via het beschreven dialoogvenster, vervolgens vanop het Action Device de gewenste systemID
pushen naar de mobiele app, en vervolgens het dialoogvenster terug gebruiken waarin nu wel de
intuïtieve systemID te zien is.
END QUOTE

The workflow for connecting to a new Action Device has been made more user-friendly after finalizing the thesis book.
Follow the instructions within the Android application.
Here they are briefly:
1) Insert the desired systemID and IPv4 address using the pattern 'systemID//IPaddress'. Example: first-laptop//192.168.1.2
2) On the Action Device, insert the same systemID within the Action Device Runtime application and push this information using the 'Send information to central hub' button. (You don't need yet to select any gestures you want to support, but you are allowed to.)
3) The connection between systemID and IP address has now been confirmed and is immediately usable.
This means the user does NOT have to reenter the IP Insertion dialog like the segment in the thesis states.
