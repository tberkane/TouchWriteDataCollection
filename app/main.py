import sys
import json
from collections import deque
from threading import Thread
from resources.LeapSDK.v53_python39 import Leap

_JOINT_POSITIONS={}

class LeapRecord(Leap.Listener):
    finger_names = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky']
    bone_names = ['Metacarpal', 'Proximal', 'Intermediate', 'Distal']

    def __init__(self):
        super(LeapRecord, self).__init__()
        self.fps = 20
        self.processing = True
        self.t = None
        self.last_time = 0

    def on_init(self, controller):
        self.t = Thread(target=self.process_frame, args=(self,))
        self.t.start()
        print("Initialized")

    def on_connect(self, controller):
        print("Connected")
        print("=====================")

    def on_disconnect(self, controller):
        # Note: not dispatched when running in a debugger.
        print("Disconnected")

    def on_exit(self, controller):
        print("=====================")
        print("Exited")
    
    def _check_frame(self, frame):
        if not (len(frame.hands)==1 and len(frame.fingers)==5 and frame.hands[0].is_right):
            return False
        return True

    def on_frame(self, controller):
        # Get the most recent frame
        frame = controller.frame()
        if self._check_frame(frame):
            # todo calibration/preprocessing?
            hand=frame.hands[0]
            joint_positions={'Wrist':(hand.arm.wrist_position[0],hand.arm.wrist_position[1],hand.arm.wrist_position[2])}
            for finger in hand.fingers:
                finger_joints={}
                for b in range(4):
                    bone=finger.bone(b)
                    finger_joints[self.bone_names[bone.type]]=(bone.prev_joint[0],bone.prev_joint[1],bone.prev_joint[2])
                    # finger_joints[self.bone_names[bone.type]]=(bone.next_joint[0],bone.next_joint[1],bone.next_joint[2])
                joint_positions[self.finger_names[finger.type]]=finger_joints

            _JOINT_POSITIONS[frame.timestamp]=joint_positions
            print(joint_positions)

    @staticmethod
    def process_frame(listener):
        print("process")

    def exit(self):
        self.processing = False
        self.t.join()
        self.exit_actions()

    def exit_actions(self):
        print("exit")

def main():
    # Create a listener and controller
    listener = LeapRecord()
    controller = Leap.Controller()
   
    # Have the listener receive events from the controller
    controller.add_listener(listener)

    # Keep this process running until Enter is pressed
    print("Listener added")
    try:
        sys.stdin.readline()
    except KeyboardInterrupt:
        pass
    finally:
        # Remove the listener when done
        controller.remove_listener(listener)
        print("Listener removed")
        listener.exit()
    
    with open('joint_positions.json', 'w') as fp:
        json.dump(_JOINT_POSITIONS, fp)
    

if __name__ == "__main__":
    main()
