# Android app
Android companion app for Pebble™

Made for my master's thesis at the University of Ghent, Belgium.



The Android app has the following features:
- TODO



Workflow:
- TODO



Installation instructions:
TODO



Used:
- 3D gesture recognition framework $3 Gesture Recognizer:
https://code.google.com/p/three-dollar-gesture-recognizer/

- MQTT client for Android:
http://dalelane.co.uk/blog/?p=1599



Remarks:
- This application GUI has only been optimized for tablets. It will work on smartphones but won't look as expected.

- Don't look too much into the commit messages. Since this project has been implemented by a single person, GitHub was mostly used as a simple backup cloud service.

- When exercising the up/down/left/right gesture, the start position is the position where the wearable display looks towards the sky and is horizontally aligned.

- When training gestures, it's very important to choose gestures that don't have any slowdowns regarding acceleration occuring. Because, if one were to excersize this gesture in the Recognize phase, the slowdowns would make the Gesture Spotting mechanism think the gesture has been completed and hence, will stop the tracking too fast.
For example: don't use a Z form but use an S form, because the latter doesn't have any slowdowns happening.
Likewise, use a circle form instead of a square form.

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

- The Connect tab can be rotated to both portrait and landscape orientation.
The code provided by the $3 Gesture Recognizer framework however, forces landscape mode by default. Since changing orientation destroys and recreates Android Fragments, this has probably been forced for a reason.
We didn't test the implications of disabling this enforcement and switching orientations. This means the Train, Recognize and Gesture Library tabs will always run in landscape mode.



Known issues:
- As the message on the Recognize tab states: the Vibration Feedback function is experimental since with this feature enabled, sometimes additional gestures get triggered aside from the requested one. For example, when you exercised a circle, the recognizer will classify it as a circle. The Pebble will then vibrate 1 time to indicate a gesture was recognized. And this vibration will trigger an up, down, left or right gesture right after.
Within the Pebble application, data samples that occured during vibrations already get discarded immediately and won't get sent over to the Android application at all. But seemingly, this is not enough.

- It's important that your device's Auto-Rotate function is ON. (Otherwise some panels won't display properly.)

- In very rare occasions, the application can crash while trying to record a gesture. When you then go to the Gesture Library section, the database won't be able to get loaded and you will encounter the message: "Error loading the gesture library. Clear the Application Data and retrain your gestures."
This bug was already occuring in the used source code. I haven't been able to reproduce this bug and fix it.

- Ideally, the list under the Bluetooth toggle button should display all paired BT devices when BT is on. When toggling BT off, the list gets cleared as expected, but when toggling on, the list doesn't get updated automatically. One has to go to another tab and then reenter the Connect tab.
Same for when a completely new Pebble is getting paired through the 'Connect Pebble™' button. Only after switching GUI panels do the changes get reflected.

- While pressing the Back button (bottom left corner) repeatedly, the currently selected tab doesn't get highlighted.

- When touching the 'Clear all trained gestures' button, the list's view doesn't get cleared immediately but only when going to another GUI panel and back to the Gesture Library panel. (The underlying data does get immediately cleared though, as expected.)
