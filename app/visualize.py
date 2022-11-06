from ntpath import join
import numpy as np
from mpl_toolkits import mplot3d
import matplotlib.pyplot as plt

joint_positions={'Wrist': (114.12227630615234, 194.92984008789062, -33.576297760009766), 'Thumb': {'Metacarpal': (93.20418548583984, 188.92361450195312, -6.638791561126709), 'Proximal': (56.51838684082031, 185.61973571777344, 36.488895416259766), 'Intermediate': (33.87064743041992, 181.47789001464844, 58.596858978271484), 'Distal': (17.044540405273438, 178.21524047851562, 72.17910766601562)}, 'Index': {'Metacarpal': (9.964570045471191, 193.6181182861328, 6.220907211303711), 'Proximal': (-23.767433166503906, 191.31741333007812, 14.097612380981445), 'Intermediate': (-47.41802978515625, 186.39828491210938, 19.327804565429688), 'Distal': (-65.100830078125, 178.45306396484375, 22.930696487426758)}, 'Middle': {'Metacarpal': (6.994821071624756, 191.8480987548828, -14.186005592346191), 'Proximal': (-32.73667526245117, 187.37576293945312, -12.683394432067871), 'Intermediate': (-61.7501220703125, 179.938720703125, -12.122316360473633), 'Distal': (-79.50486755371094, 171.73765563964844, -10.99362850189209)}, 'Ring': {'Metacarpal': (11.116713523864746, 186.2623748779297, -37.88296127319336), 'Proximal': (-26.838966369628906, 177.7587890625, -44.05311584472656), 'Intermediate': (-46.97198486328125, 168.7454071044922, -46.69011688232422), 'Distal': (-60.661903381347656, 159.67510986328125, -46.83859634399414)}, 'Pinky': {'Metacarpal': (21.166690826416016, 178.32437133789062, -55.814842224121094), 'Proximal': (-5.993131637573242, 176.6681365966797, -64.40630340576172), 'Intermediate': (-26.108949661254883, 171.97665405273438, -69.29515075683594), 'Distal': (-39.206886291503906, 165.97914123535156, -70.37763214111328)}}

def flatten(l):
    return [item for sublist in l for item in sublist]

def connect_points(a,b,ax):
    x=[a[0],b[0]]
    y=[a[1],b[1]]
    z=[a[2],b[2]]
    ax.plot(x,y,z,color='r')

finger_names = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky']

fig = plt.figure()
ax = plt.axes(projection='3d')
xdata=[joint_positions["Wrist"][0]] + [j[0] for j in flatten([list(finger.values()) for finger in [joint_positions[finger] for finger in finger_names]])]
ydata=[joint_positions["Wrist"][1]] + [j[1] for j in flatten([list(finger.values()) for finger in [joint_positions[finger] for finger in finger_names]])]
zdata=[joint_positions["Wrist"][2]] + [j[2] for j in flatten([list(finger.values()) for finger in [joint_positions[finger] for finger in finger_names]])]


ax.scatter3D(xdata, ydata, zdata, c=zdata, cmap='Greens')
#palm
connect_points(joint_positions["Wrist"],joint_positions["Thumb"]["Metacarpal"],ax)
connect_points(joint_positions["Wrist"],joint_positions["Pinky"]["Metacarpal"],ax)
connect_points(joint_positions["Index"]["Metacarpal"],joint_positions["Thumb"]["Metacarpal"],ax)
connect_points(joint_positions["Index"]["Metacarpal"],joint_positions["Middle"]["Metacarpal"],ax)
connect_points(joint_positions["Ring"]["Metacarpal"],joint_positions["Middle"]["Metacarpal"],ax)
connect_points(joint_positions["Ring"]["Metacarpal"],joint_positions["Pinky"]["Metacarpal"],ax)
for finger in finger_names:
    connect_points(joint_positions[finger]["Metacarpal"],joint_positions[finger]["Proximal"],ax)
    connect_points(joint_positions[finger]["Intermediate"],joint_positions[finger]["Proximal"],ax)
    connect_points(joint_positions[finger]["Intermediate"],joint_positions[finger]["Distal"],ax)
plt.show()