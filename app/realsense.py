import pyrealsense2 as rs                 # Intel RealSense cross-platform open-source API
import time
import json
import numpy as np
from threading import Thread, Timer


DATA={}
ID=2
NUM=0

def task():
    global DATA
    global NUM
    
    with open(f'realsense_id{ID}_num{NUM}.json', 'w') as fp:
        json.dump(DATA.copy(), fp)
    NUM+=1
    DATA.clear()
    Timer(1.0, task).start()
    


def main():
    pipe = rs.pipeline()                      # Create a pipeline
    cfg = rs.config()                         # Create a default configuration
    print("Pipeline is created")

    print("Searching Devices..")
    selected_devices = []                     # Store connected device(s)

    for d in rs.context().devices:
        selected_devices.append(d)
        print(d.get_info(rs.camera_info.name))
    if not selected_devices:
        print("No RealSense device is connected!")

    pipe.start(cfg)                                 # Configure and start the pipeline


    for _ in range(10):                                       # Skip first frames to give syncer and auto-exposure time to adjust
        frameset = pipe.wait_for_frames()
        
    try:
        while True:
            frameset = pipe.wait_for_frames()                     # Read frames from the file, packaged as a frameset
            depth_frame = frameset.get_depth_frame()              # Get depth frame
            color_frame = frameset.get_color_frame()
            DATA[int(time.time()*1000)]=(np.asanyarray(depth_frame.get_data()).tolist())
            # print(int(time.time()*1000))
    except KeyboardInterrupt:
        pass   
    finally:
        pipe.stop()                                               # Stop the pipeline

    with open(f'realsense_id{ID}_num{NUM}.json', 'w') as fp:
        json.dump(DATA, fp)
    

if __name__ == "__main__":
    Timer(1.0, task).start()
    main()