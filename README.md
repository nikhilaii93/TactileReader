# TactileReader

# Overview
Android app that can be used by visually impaired to help them identify regions of textured braille scripts. This will aid them in understanding & studying (particularly academic content) better by providing them information of the pointed regions through text-to-speech feature of the smart phone.

# Usage
The user needs to wear a tag on his/her finger that is vibrant in color and thus can be recognized easily by a smartphone's camera. The camera needs to be placed atop the concerned document. Once the application is started, it'll track the user's finger and identify the region on which the user is pointing and it'll then start reading the relevant content.
The content reading can be controlled (stop/start/next) by various finger gestures of the user.

# Setup
To ease usage of the application, the user can get a stand which is ergonimically designed to help in proper placement of the document and a smartphone holder atop which the user can place his/her phone after starting the application, the design of the stand can be found at: [ErgonomicStand](https://github.com/nikhilaii93/ErgonomicStand)

# Pre-processing
The content that'll be read is first to be provided to the application in terms of transcripts which can then be read by the application. This can be done for standardized texts and the publishing authority can provide the transcripts of the concerned documents to the application. To help in standardizing and creating such content the publisher needs to use a Java application which is available at: [PolygonSel](https://github.com/nikhilaii93/PolygonSel)
