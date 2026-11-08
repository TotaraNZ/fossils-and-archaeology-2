"""Rig specs for the legacy FlammaRilva Fossils & Archaeology models.

Bone sides follow the animal: ``_L`` is its left (+x in GeckoLib space, -x in legacy space),
regardless of the legacy part names, which are inconsistent between models. Explicit pivots
are legacy model coordinates (y down, ground at 24, before the rig offset is applied).
"""
from rig import Bone, Rig

ROOT = Bone("Root", None, [], (0, 24, 0))

STEGOSAURUS = Rig("stegosaurus", "ModelStegosaurus", offset=(-0.5, 0, 0),
                  colors={"base": (98, 112, 70), "belly": (176, 168, 120), "accent": (170, 72, 48)}, bones=[
    ROOT,
    Bone("Body", "Root", ["H1", "T3"], "center", role="body"),
    Bone("Neck", "Body", ["H2", "T4"], "back", role="neck"),
    Bone("Head", "Neck", ["H3"], "back", role="head"),
    Bone("Tail1", "Body", ["H4", "T2"], "front", role="tail"),
    Bone("Tail2", "Tail1", ["H5"], "front", role="tail"),
    Bone("Tail3", "Tail2", ["H6", "T1"], "front", role="tail"),
    Bone("FrontLeg_L", "Body", ["D_L_1"], "top", role="front_leg_l"),
    Bone("FrontFoot_L", "FrontLeg_L", ["D_L_2"], "top", role="front_lower_l"),
    Bone("FrontLeg_R", "Body", ["D_R_1"], "top", role="front_leg_r"),
    Bone("FrontFoot_R", "FrontLeg_R", ["D_R_2"], "top", role="front_lower_r"),
    Bone("BackLeg_L", "Body", ["DD_L_1"], "top", role="back_leg_l"),
    Bone("BackFoot_L", "BackLeg_L", ["DD_L_2"], "top", role="back_lower_l"),
    Bone("BackLeg_R", "Body", ["D_D_R_1"], "top", role="back_leg_r"),
    Bone("BackFoot_R", "BackLeg_R", ["DD_R_2"], "top", role="back_lower_r"),
])

BRACHIOSAURUS = Rig("brachiosaurus", "ModelBrachiosaurus",
                    colors={"base": (120, 128, 112), "belly": (184, 180, 160), "accent": (92, 100, 86)}, bones=[
    ROOT,
    Bone("Hips", "Root", ["Lower_Body"], "center", role="body"),
    Bone("Chest", "Hips", ["Body"], (0, 12, 1), role="chest"),
    Bone("Neck1", "Chest", ["Lower_Neck1", "Lower_Neck"], (0, 11, -2), role="neck"),
    Bone("Neck2", "Neck1", ["Neck121211", "Neck12121", "Neck1212"], "bottom", role="neck"),
    Bone("Neck3", "Neck2", ["Neck121", "Neck1211", "Neck12"], "bottom", role="neck"),
    Bone("Neck4", "Neck3", ["Neck1", "Neck"], "bottom", role="neck"),
    Bone("Head", "Neck4", ["Head", "Snout", "Crest"], (0, -4, -10.5), role="head"),
    Bone("Tail1", "Hips", ["Tail"], "front", role="tail"),
    Bone("Tail2", "Tail1", ["Tail1"], "front", role="tail"),
    Bone("Tail3", "Tail2", ["Tail11"], "front", role="tail"),
    Bone("FrontLeg_L", "Chest", ["Front_Thigh1"], "top", role="front_leg_l"),
    Bone("FrontShin_L", "FrontLeg_L", ["Front_Calf2"], "top", role="front_lower_l"),
    Bone("FrontLeg_R", "Chest", ["Front_Thigh"], "top", role="front_leg_r"),
    Bone("FrontShin_R", "FrontLeg_R", ["Front_Calf1"], "top", role="front_lower_r"),
    Bone("BackLeg_L", "Hips", ["Back_Thigh"], "top", role="back_leg_l"),
    Bone("BackShin_L", "BackLeg_L", ["Back_CalfLeft"], "top", role="back_lower_l"),
    Bone("BackLeg_R", "Hips", ["Back_Thigh1"], "top", role="back_leg_r"),
    Bone("BackShin_R", "BackLeg_R", ["Back_CalfRight"], "top", role="back_lower_r"),
])

DILOPHOSAURUS = Rig("dilophosaurus", "Modeldil",
                    colors={"base": (150, 140, 70), "belly": (210, 196, 140), "accent": (196, 70, 40)}, bones=[
    ROOT,
    Bone("Hips", "Root", ["LowerBody"], "center", role="body"),
    Bone("Chest", "Hips", ["UpperBody"], "back", role="chest"),
    Bone("Neck", "Chest", ["Neck"], "back", role="neck"),
    Bone("Head", "Neck", ["Head", "Jaw1", "Crest", "Crest2", "Crest3", "Crest4"], (0, 7, -11), role="head"),
    Bone("Jaw", "Head", ["Jaw2"], "back", role="jaw"),
    Bone("Frill", "Head", ["Frill1", "Frill2"], (0, 7.5, -10), role="frill"),
    Bone("Tail1", "Hips", ["Tail1"], "front", role="tail"),
    Bone("Tail2", "Tail1", ["Tail2"], "front", role="tail"),
    Bone("Leg_L", "Hips", ["Thigh2"], "top", role="back_leg_l"),
    Bone("Shin_L", "Leg_L", ["Heel2"], "top", role="back_lower_l"),
    Bone("Foot_L", "Shin_L", ["Feet2"], "top", role="foot_l"),
    Bone("Leg_R", "Hips", ["Thigh1"], "top", role="back_leg_r"),
    Bone("Shin_R", "Leg_R", ["Heel1"], "top", role="back_lower_r"),
    Bone("Foot_R", "Shin_R", ["Feet1"], "top", role="foot_r"),
    Bone("Arm_L", "Chest", ["UpperArm2"], "top", role="arm_l"),
    Bone("Forearm_L", "Arm_L", ["LowerArm2"], "top", role="forearm_l"),
    Bone("Arm_R", "Chest", ["UpperArm1"], "top", role="arm_r"),
    Bone("Forearm_R", "Arm_R", ["LowerArm1"], "top", role="forearm_r"),
])

VELOCIRAPTOR = Rig("velociraptor", "ModelRaptor",
                   colors={"base": (128, 92, 60), "belly": (204, 176, 132), "accent": (60, 44, 32)}, bones=[
    ROOT,
    Bone("Hips", "Root", ["noumenon2"], "center", role="body"),
    Bone("Chest", "Hips", ["noumenon1"], "back", role="chest"),
    Bone("Neck", "Chest", ["head1"], "bottom", role="neck"),
    Bone("Head", "Neck", ["head2", "head3_up"], (0, 4, -5), role="head"),
    Bone("Jaw", "Head", ["head3_down"], "back", role="jaw"),
    Bone("Tail1", "Hips", ["noumenon3"], "front", role="tail"),
    Bone("Tail2", "Tail1", ["noumenon4"], "front", role="tail"),
    Bone("Leg_L", "Hips", ["thigh_R2"], "top", role="back_leg_l"),
    Bone("Shin_L", "Leg_L", ["leg_R2"], "top", role="back_lower_l"),
    Bone("Foot_L", "Shin_L", ["horn_1_R"], "top", role="foot_l"),
    Bone("Claw_L", "Foot_L", ["right_nail_1", "right_nail_2"], "back", role="claw_l"),
    Bone("Leg_R", "Hips", ["thigh_L2"], "top", role="back_leg_r"),
    Bone("Shin_R", "Leg_R", ["leg_L2"], "top", role="back_lower_r"),
    Bone("Foot_R", "Shin_R", ["horn_2_L"], "top", role="foot_r"),
    Bone("Claw_R", "Foot_R", ["left_nail_1", "left_nail_2"], "back", role="claw_r"),
    Bone("Arm_L", "Chest", ["thigh_R1"], "top", role="arm_l"),
    Bone("Forearm_L", "Arm_L", ["leg_R1"], "top", role="forearm_l"),
    Bone("Arm_R", "Chest", ["thigh_L1"], "top", role="arm_r"),
    Bone("Forearm_R", "Arm_R", ["leg_L1"], "top", role="forearm_r"),
])

PTEROSAUR = Rig("pterosaur", "ModelPterosaurGround",
                colors={"base": (150, 110, 90), "belly": (220, 200, 180), "accent": (200, 70, 50)}, bones=[
    ROOT,
    Bone("Body", "Root", ["Body"], (0, 18, 2), role="body"),
    Bone("Tail", "Body", ["Tail"], (0, 20, 5), role="tail"),
    Bone("Leg_L", "Body", ["right_leg"], (-1, 22, 2), role="back_leg_l"),
    Bone("Leg_R", "Body", ["Left_leg"], (1, 22, 2), role="back_leg_r"),
    Bone("Neck1", "Body", ["Neck_1"], (0, 15, 0), role="neck"),
    Bone("Neck2", "Neck1", ["Neck_2"], (0, 14, -3), role="neck"),
    Bone("Head", "Neck2", ["Head", "crown", "upper_mouth"], (0, 14, -4), role="head"),
    Bone("Jaw", "Head", ["lower_mouth"], (0, 13.5, -8), role="jaw"),
    Bone("Wing_L", "Body", ["Right_wing_1"], (-2, 16, 1), role="wing_l"),
    Bone("WingTip_L", "Wing_L", ["Right_wing_2"], (-6.9, 20, -4), role="wingtip_l"),
    Bone("Wing_R", "Body", ["Left_wing_1"], (2, 16, 1), role="wing_r"),
    Bone("WingTip_R", "Wing_R", ["Left_wing_2"], (6.9, 20, -4), role="wingtip_r"),
])

MOSASAURUS = Rig("mosasaurus", "ModelMosasaurus", offset=(-1, 0, 0),
                 colors={"base": (64, 90, 110), "belly": (190, 200, 196), "accent": (40, 58, 72)}, bones=[
    ROOT,
    Bone("Body", "Root", ["Body"], "center", role="body"),
    Bone("Head", "Body", ["Head", "Upper_Jaws", "Upper_Teeth"], "back", role="head"),
    Bone("Jaw", "Head", ["Lower_jaw"], "back", role="jaw"),
    Bone("Tail1", "Body", ["Tail_1"], "front", role="tail"),
    Bone("Tail2", "Tail1", ["Tail_2", "Tail_2_dec"], "front", role="tail"),
    Bone("Tail3", "Tail2", ["Tail_3", "Tail_3_dec"], "front", role="tail"),
    Bone("FrontFlipper_L", "Body", ["right_arm"], (-3, 20, 0), role="front_flipper_l"),
    Bone("FrontFlipper_R", "Body", ["Left_arm"], (5, 20, 0), role="front_flipper_r"),
    Bone("BackFlipper_L", "Body", ["Right_Leg"], (-2, 20, 7), role="back_flipper_l"),
    Bone("BackFlipper_R", "Body", ["Left_Leg"], (4, 20, 7), role="back_flipper_r"),
])

PLESIOSAURUS = Rig("plesiosaurus", "ModelPlesiosaur",
                   colors={"base": (70, 96, 104), "belly": (200, 206, 196), "accent": (48, 66, 72)}, bones=[
    ROOT,
    Bone("Body", "Root", ["Body"], "center", role="body"),
    Bone("Neck1", "Body", ["Neck1"], (0, 17, 0), role="neck"),
    Bone("Neck2", "Neck1", ["Neck2"], (0, 16, -4), role="neck"),
    Bone("Neck3", "Neck2", ["Neck3"], (0, 12.73, -8), role="neck"),
    Bone("Neck4", "Neck3", ["Neck4"], (0, 10, -11), role="neck"),
    Bone("Head", "Neck4", ["head"], (0, 9, -15), role="head"),
    Bone("Tail1", "Body", ["tail1"], (0, 20, 0), role="tail"),
    Bone("Tail2", "Tail1", ["tail2"], "front", role="tail"),
    Bone("Tail3", "Tail2", ["tail3"], "front", role="tail"),
    Bone("FrontFlipper_L", "Body", ["right_arm"], (-3, 21, -3), role="front_flipper_l"),
    Bone("FrontFlipper_R", "Body", ["left_arm"], (3, 21, -3), role="front_flipper_r"),
    Bone("BackFlipper_L", "Body", ["right_leg"], (-3, 21, 4), role="back_flipper_l"),
    Bone("BackFlipper_R", "Body", ["left_leg"], (3, 21, 4), role="back_flipper_r"),
])

ALL = [STEGOSAURUS, BRACHIOSAURUS, DILOPHOSAURUS, VELOCIRAPTOR, PTEROSAUR, MOSASAURUS, PLESIOSAURUS]
