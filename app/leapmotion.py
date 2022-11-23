import json
import sys
import time
from threading import Thread
from threading import Timer

from playsound import playsound

from resources.LeapSDK.v53_python39 import Leap

_JOINT_POSITIONS = {}  # contains the data which will be saved to a json file every 10s
ID = 4  # participant's id
NUM = 0  # index of the json file


def save_joint_data():
    """
    Dumps the data in _JOINT_POSITIONS to a json file, then does the same in 10s
    """
    global _JOINT_POSITIONS
    global NUM

    Timer(10.0, save_joint_data).start()  # we want this to happen every 10s
    with open(f'joint_positions_id{ID}_num{NUM}.json', 'w') as fp:
        json.dump(_JOINT_POSITIONS.copy(), fp)
    NUM += 1
    _JOINT_POSITIONS.clear()


class LeapRecord(Leap.Listener):
    finger_names = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky']
    bone_names = ['Metacarpal', 'Proximal', 'Intermediate', 'Distal']

    def __init__(self):
        super(LeapRecord, self).__init__()
        self.tracking = False
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
        """
        Checks if the given frame contains a unique right hand
        """
        if not (len(frame.hands) == 1 and len(frame.fingers) == 5 and frame.hands[0].is_right):
            return False
        return True

    def on_frame(self, controller):
        # Get the most recent frame
        frame = controller.frame()
        if self._check_frame(frame):
            if not self.tracking:  # play a sound if hand becomes detected
                Thread(target=playsound, args=("detected.wav",)).start()
            self.tracking = True
            hand_positions = {}
            for hand in frame.hands:
                joint_positions = {
                    'Wrist': (hand.arm.wrist_position[0], hand.arm.wrist_position[1], hand.arm.wrist_position[2])}
                for finger in hand.fingers:
                    finger_joints = {}
                    for b in range(4):
                        bone = finger.bone(b)
                        finger_joints[self.bone_names[bone.type]] = (
                            bone.prev_joint[0], bone.prev_joint[1], bone.prev_joint[2])
                        # finger_joints[self.bone_names[bone.type]]=(bone.next_joint[0],bone.next_joint[1],bone.next_joint[2])
                    joint_positions[self.finger_names[finger.type]] = finger_joints
                hand_positions["Right" if hand.is_right else "Left"] = joint_positions
            _JOINT_POSITIONS[int(time.time() * 1000)] = hand_positions
        else:
            if self.tracking or int(time.time() * 1000) % 900 == 0:  # play a sound regularly if hand becomes undetected
                Thread(target=playsound, args=("lost.wav",)).start()
            self.tracking = False

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
    Timer(10.0, save_joint_data).start()  # save joint data for the first time in 10s
    try:
        sys.stdin.readline()
    except KeyboardInterrupt:
        pass
    finally:
        # Remove the listener when done
        controller.remove_listener(listener)
        print("Listener removed")
        listener.exit()
    with open(f'joint_positions_id{ID}_num{NUM}.json', 'w') as fp:
        json.dump(_JOINT_POSITIONS, fp)  # also save at the end to make sure we have all the data


if __name__ == "__main__":
    main()
