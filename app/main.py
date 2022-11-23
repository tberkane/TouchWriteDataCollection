import sys
import json
import time
from threading import Thread, Timer
from playsound import playsound
from resources.LeapSDK.v53_python39 import Leap
from threading import Thread

_JOINT_POSITIONS={}
ID=4
NUM=0

def sound(a):
    playsound(a)


def task():
    global _JOINT_POSITIONS
    global NUM

    Timer(10.0, task).start()
    with open(f'joint_positions_id{ID}_num{NUM}.json', 'w') as fp:
        json.dump(_JOINT_POSITIONS.copy(), fp)
    NUM+=1
    _JOINT_POSITIONS.clear()

class LeapRecord(Leap.Listener):
    finger_names = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky']
    bone_names = ['Metacarpal', 'Proximal', 'Intermediate', 'Distal']

    def __init__(self):
        super(LeapRecord, self).__init__()
        self.tracking=False
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
            if not self.tracking:
                # playsound('detected.wav')
                Thread(target=sound, args=("detected.wav",)).start()

            self.tracking=True
            hand_positions={}
            for hand in frame.hands:
                joint_positions={'Wrist':(hand.arm.wrist_position[0],hand.arm.wrist_position[1],hand.arm.wrist_position[2])}
                for finger in hand.fingers:
                    finger_joints={}
                    for b in range(4):
                        bone=finger.bone(b)
                        finger_joints[self.bone_names[bone.type]]=(bone.prev_joint[0],bone.prev_joint[1],bone.prev_joint[2])
                        # finger_joints[self.bone_names[bone.type]]=(bone.next_joint[0],bone.next_joint[1],bone.next_joint[2])
                    joint_positions[self.finger_names[finger.type]]=finger_joints
                hand_positions["Right" if hand.is_right else "Left"]=joint_positions
            _JOINT_POSITIONS[int(time.time()*1000)]=hand_positions
        else:
            if self.tracking or int(time.time()*1000)%900==0:
                # playsound('lost.wav')
                Thread(target=sound, args=("lost.wav",)).start()
            self.tracking=False    

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
    controller.set_policy_flags(Leap.Controller.POLICY_IMAGES)
   
    # Have the listener receive events from the controller
    controller.add_listener(listener)

    # Keep this process running until Enter is pressed
    print("Listener added")
    Timer(10.0, task).start()
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
        json.dump(_JOINT_POSITIONS, fp)

  

if __name__ == "__main__":
    main()
