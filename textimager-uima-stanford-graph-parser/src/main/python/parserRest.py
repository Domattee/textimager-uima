import json

import sys
#PARSER_PATH = '.'
#sys.path.insert(0, PARSER_PATH)

from flask import Flask
from flask import request
import StringIO
import mainParser
import os
import datetime
import re
import copy
import multiprocessing as mp

SAVE_DIR = 'models/de/'
TEMP_FILE_BASE = "temp_"

# CONVERSION_TABLE = {
#     "PUNCT" : {"PunctType=Brck" : "$(", "PunctType=Comm": "$,", "PunctType=Peri": "$."},
#     "ADJ" : {"_" : "ADJA", "Variant=Short" : "ADJD"},
#     "ADV" : {"_" : "ADV"},
#     "ADP" : {"AdpType=Post": "APPO", "AdpType=Prep" : "APPR", "AdpType=Prep|PronType=Art": "APPRART"},
#     "APZR": ["ADP", "AdpType=Circ"],
#     "ART": ["DET", "PronType=Art"],
#     "CARD": ["NUM", "NumType=Card"],
#     "FM": ["X", "Foreign=Foreign"],
#     "ITJ": ["INTJ", "_"],
#     "KOKOM": ["CONJ", "ConjType=Comp"],
#     "KON": ["CONJ", "_"],
#     "KOUI": ["SCONJ", "_"],
#     "KOUS": ["SCONJ", "_"],
#     "NE": ["PROPN", "_"],
#     "NN": ["NOUN", "_"],
#     "PAV": ["ADV", "PronType=Dem"],
#     "PDAT": ["DET", "PronType=Dem"],
#     "PDS": ["PRON", "PronType=Dem"],
#     "PIAT": ["DET", "PronType=Ind,Neg,Tot"],
#     "PIDAT": ["DET", "AdjType=Pdt|PronType=Ind,Neg,Tot"],
#     "PIS": ["PRON", "PronType=Ind,Neg,Tot"],
#     "PPER": ["PRON", "PronType=Prs"],
#     "PPOSAT": ["DET", "Poss=Yes|PronType=Prs"],
#     "PPOSS": ["PRON", "Poss=Yes|PronType=Prs"],
#     "PRELAT": ["DET", "PronType=Rel"],
#     "PRELS": ["PRON", "PronType=Rel"],
#     "PRF": ["PRON", "PronType=Prs|Reflex=Yes"],
#     "PTKA": ["PART", "_"],
#     "PTKANT": ["PART", "PartType=Res"],
#     "PTKNEG": ["PART", "Negative=Neg"],
#     "PTKVZ": ["PART", "PartType=Vbp"],
#     "PTKZU": ["PART", "PartType=Inf"],
#     "PWAT": ["DET", "PronType=Int"],
#     "PWAV": ["ADV", "PronType=Int"],
#     "PWS": ["PRON", "PronType=Int"],
#     "TRUNC": ["X", "Hyph=Yes"],
#     "VAFIN": ["AUX", "Mood=Ind|VerbForm=Fin"],
#     "VAIMP": ["AUX", "Mood=Imp|VerbForm=Fin"],
#     "VAINF": ["AUX", "VerbForm=Inf"],
#     "VAPP": ["AUX", "Aspect=Perf|VerbForm=Part"],
#     "VMFIN": ["VERB", "Mood=Ind|VerbForm=Fin|VerbType=Mod"],
#     "VMINF": ["VERB", "VerbForm=Inf|VerbType=Mod"],
#     "VMPP": ["VERB", "Aspect=Perf|VerbForm=Part|VerbType=Mod"],
#     "VVFIN": ["VERB", "Mood=Ind|VerbForm=Fin"],
#     "VVIMP": ["VERB", "Mood=Imp|VerbForm=Fin"],
#     "VVINF": ["VERB", "VerbForm=Inf"],
#     "VVIZU": ["VERB", "VerbForm=Inf"],
#     "VVPP": ["VERB", "Aspect=Perf|VerbForm=Part"],
#     "XY": ["X", "_"]
#     }

CONVERSION_TABLE = {
    "$(": ["PUNCT", "PunctType=Brck"],
    "$,": ["PUNCT", "PunctType=Comm"],
    "$.": ["PUNCT", "PunctType=Peri"],
    "ADJA": ["ADJ", "_"],
    "ADJD": ["ADJ", "Variant=Short"],
    "ADV": ["ADV", "_"],
    "APPO": ["ADP", "AdpType=Post"],
    "APPR": ["ADP", "AdpType=Prep"],
    "APPRART": ["ADP", "AdpType=Prep|PronType=Art"],
    "APZR": ["ADP", "AdpType=Circ"],
    "ART": ["DET", "PronType=Art"],
    "CARD": ["NUM", "NumType=Card"],
    "FM": ["X", "Foreign=Foreign"],
    "ITJ": ["INTJ", "_"],
    "KOKOM": ["CONJ", "ConjType=Comp"],
    "KON": ["CONJ", "_"],
    "KOUI": ["SCONJ", "_"],
    "KOUS": ["SCONJ", "_"],
    "NE": ["PROPN", "_"],
    "NN": ["NOUN", "_"],
    "PAV": ["ADV", "PronType=Dem"],
    "PDAT": ["DET", "PronType=Dem"],
    "PDS": ["PRON", "PronType=Dem"],
    "PIAT": ["DET", "PronType=Ind,Neg,Tot"],
    "PIDAT": ["DET", "AdjType=Pdt|PronType=Ind,Neg,Tot"],
    "PIS": ["PRON", "PronType=Ind,Neg,Tot"],
    "PPER": ["PRON", "PronType=Prs"],
    "PPOSAT": ["DET", "Poss=Yes|PronType=Prs"],
    "PPOSS": ["PRON", "Poss=Yes|PronType=Prs"],
    "PRELAT": ["DET", "PronType=Rel"],
    "PRELS": ["PRON", "PronType=Rel"],
    "PRF": ["PRON", "PronType=Prs|Reflex=Yes"],
    "PTKA": ["PART", "_"],
    "PTKANT": ["PART", "PartType=Res"],
    "PTKNEG": ["PART", "Negative=Neg"],
    "PTKVZ": ["PART", "PartType=Vbp"],
    "PTKZU": ["PART", "PartType=Inf"],
    "PWAT": ["DET", "PronType=Int"],
    "PWAV": ["ADV", "PronType=Int"],
    "PWS": ["PRON", "PronType=Int"],
    "TRUNC": ["X", "Hyph=Yes"],
    "VAFIN": ["AUX", "Mood=Ind|VerbForm=Fin"],
    "VAIMP": ["AUX", "Mood=Imp|VerbForm=Fin"],
    "VAINF": ["AUX", "VerbForm=Inf"],
    "VAPP": ["AUX", "Aspect=Perf|VerbForm=Part"],
    "VMFIN": ["VERB", "Mood=Ind|VerbForm=Fin|VerbType=Mod"],
    "VMINF": ["VERB", "VerbForm=Inf|VerbType=Mod"],
    "VMPP": ["VERB", "Aspect=Perf|VerbForm=Part|VerbType=Mod"],
    "VVFIN": ["VERB", "Mood=Ind|VerbForm=Fin"],
    "VVIMP": ["VERB", "Mood=Imp|VerbForm=Fin"],
    "VVINF": ["VERB", "VerbForm=Inf"],
    "VVIZU": ["VERB", "VerbForm=Inf"],
    "VVPP": ["VERB", "Aspect=Perf|VerbForm=Part"],
    "XY": ["X", "_"]
}


app = Flask(__name__)

#Hacky, better to change bucket_size dynamically with number of inputs in sentences somehow?
print("Initializing german long model")
NETWORK = mainParser.network_init(SAVE_DIR, default={"save_dir" : SAVE_DIR})
print("Initializing german short model")
NETWORK_SHORT = mainParser.network_init(SAVE_DIR, short = True, default={"save_dir" : SAVE_DIR})
print("Successfully Initialized!")

@app.route("/sdp", methods=['POST'])
def sdp():
    text = request.json["dict"]
    #print(text)
    
    
    # Add UPOS Tags and associated Feats
    lines = []
    n_sentences = 0
    for line in text.split("\n"):
        line.strip()
        if line:
            line = line.split("\t")
            xpos = line[4]
            if xpos == "_": continue
            morph = line[5]
            morphs = morph.split("|")
            if xpos in CONVERSION_TABLE:
                upos = CONVERSION_TABLE[xpos][0]
                for feat in CONVERSION_TABLE[xpos][1].split("|"):
                    if feat not in ["_"]:
                        if feat not in morphs:
                            morphs.append(feat)
            else:
                upos = xpos
            if morphs in ["_"]: morphs = ""
            line[5] = "|".join(morphs)
            line[3] = upos
            line = "\t".join(line)
            lines.append(line)
        else:
            lines.append("")
            n_sentences += 1
    text = "\n".join(lines)
    
    if n_sentences < 10:
        network = NETWORK_SHORT
        print("Short input")
    else:
        network = NETWORK
    #print(text)
    
    #os.chdir(PARSER_PATH)
    
    input = TEMP_FILE_BASE + "_".join(re.findall(r"[\d]+", str(datetime.datetime.now()))) + ".conllu"
    with open(input, "w") as f:
        f.write(text.encode('utf-8'))
        f.close()
        
    print("Parsing...")
    #mainParser.parse(SAVE_DIR, files=input, default={'save_dir': SAVE_DIR}, output_dir='.')
    #TODO: in network.py die parse() definition anschauen und versuchen die tensorflow initializierung auch rauszuziehen.
    proc = mp.Process(target = parallel_parse, args = (network, input, "."))
    proc.start()
    proc.join()
    
    
    
    
    sentences = []
    with open(input) as f:
        sentence = []
        for line in f:
            if line not in ["\n", "\r\n"]:
                line = line.split("\t")
                parsed = {}
                if (len(line) != 10): print("Output was misformatted!")
                parsed["ID"] = line[0]
                parsed["TOKEN"] = line[1]
                parsed["LEMMA"] = line[2]
                parsed["UPOS"] = line[3]
                parsed["XPOS"] = line[4]
                parsed["MORPH"] = line[5]
                parsed["HEAD"] = line[6]
                parsed["REL"] = line[7]
                sentence.append(parsed)
            else: 
                sentences.append(sentence)
                sentence = []
    
    #Delete temp_file
    os.remove(input)
    
    return json.dumps({
        #"text": doc.text,
        "sentences": sentences
    })
    
def parallel_parse(network, input, output_dir):
    mainParser.parse_files(network, input, output_dir)