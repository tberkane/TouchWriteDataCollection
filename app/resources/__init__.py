import os
import sys
import inspect
src_dir = os.path.dirname(inspect.getfile(inspect.currentframe()))
# LeapSDK
arch_dir = 'LeapSDK/v53_python39/lib/x64'
sys.path.insert(0, os.path.abspath(os.path.join(src_dir, arch_dir)))
