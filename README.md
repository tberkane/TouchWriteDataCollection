# Touch Write Data Collection
Android app and scripts for collecting the data which is analysed in https://github.com/tberkane/TouchWriteAnalysis.

# Data format
The collected data will have the following formats:

## Capacitive data
A file containing capacitive data is named `recording_idID_HAND.json`, where ID is the participant's identifier (to sync with depth and hand pose data) and HAND can be rightHanded or leftHanded.

The file contains JSON with the following format:

`{character: {timestamp: {CAP_IMG: capImage, PEN_POS: penPos},...},...}`

Where `character` is a 4-digit string, of the format "CSGN" where C is the character being written (e.g. 1, 2, a, etc.), S is the size of the character (l, m or s), G is whether the writing style is guided or freehand (g or s) and N is the index of the collected sample (0, 1, 2, ...),

`capImage` is a 1d array containing the capacitive values for the 37*49 capacitive image,

and `penPose` is the (x,y) coordinates of the pen tip on the screen.

## Depth data
A participant's recorded depth data is separated in several JSON files.

A file containing depth data is named `realsense_idID_numNUM.json`, where ID is the participant's identifier (to sync with depth and hand pose data) and NUM is the index of the file (0, 1, 2, ...).

The file contains JSON with the following format:

`{timestamp: depth_array,...}`

## Hand pose data
A participant's recorded hand pose data is separated in several JSON files.

A file containing hand pose data is named `joint_positions_idID_numNUM.json`, where ID is the participant's identifier (to sync with depth and hand pose data) and NUM is the index of the file (0, 1, 2, ...).

The file contains JSON with the following format:

`{timestamp: joint_positions,...}`

Where `joint_positions` contains the 3d positions of the participant's wrist and fingers.

# How to run the Android app
The app will only work on a Samsung Galaxy Tab S2 with a modified kernel.

The app can be installed on the tablet using Android Studio.

# How to run the data collection scripts
Simply execute the two scripts before starting the data collection process and they record data continuously. You can then stop them when the data collection is done.


# Project Organization
    ├── README.md          <- README for developers using this project.
    │
    ├── android            <- The Android app to collect the data.
    │   ├── CapImage       <- Low-level code to access the tablet's capacitive data (credits to https://github.com/FIGLAB/3DHandPose).
    │   └── DataCollection <- Code for the app with which the participant interacts.
    │
    └── scripts
        ├── leapmotion.py  <- Script to record the Leap Motion data and warn when it is not tracking properly.
        ├── realsense.py   <- Script to record the RealSense data.
        └── visualize.py   <- Script to visualize the recorded hand pose data.
