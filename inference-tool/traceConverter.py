#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Dec 10 13:25:34 2019

@author: michael
"""

import re
import json
import random

root = "/home/michael/eclipse-workspace/concurrency/"
newRoot = "/home/michael/Documents/efsm-inference/inference-tool/experimental-data/"

#file = "liftDoors2"
#outfile = "liftDoors30"

numTraces = 5
file = "new.log"
outfile = "spaceInvaders"
outfile += str(numTraces)

x = 0
aliens = 1
shields = 2


desired_inputs = {
    "start": [x, aliens, shields],
    "alienHit": [aliens],
    "addAlien": [],
    "moveWest": [x],
    "moveEast": [x],
    "launchMissile": [],
    "shieldHit": [shields],
    "win": [],
    "lose": []
}

desired_outputs = desired_inputs


def varname(obj, namespace=globals()):
    return [name for name in namespace if namespace[name] is obj][0]


def getTypes(f):
    typeDecl = re.compile("(\w+) +((\w+:[\w\[:\]]+ *)+)")
    typeFun = {'N': lambda x: int(float(x)), 'S': str, 'NI': int, 'I': int}
    types = {}
    line = f.readline().strip()  # Strip off "types"
    line = f.readline().strip()
    global typeHead
    while line != "trace":
        typeHead += line + "\n"
        match = typeDecl.search(line)
        types[match.group(1)] = [typeFun[x.split(":")[1].split("[")[0]] for x in match.group(2).split(" ")]
        line = f.readline().strip()
    return types


def trim(traces, numEvents):
    return [[event for event in trace[:numEvents]] for trace in traces]


typeHead = "types\n"


def print_original_trace(f, traces):
    print(typeHead, file=f, end="")
    for trace in traces:
        print("trace", file=f)
        for (label, inputs) in trace:
            print(label, " ".join([str(x) for x in inputs]), file=f)


def format_trace(trace):
    labels = [label for label, inputs in trace[:-1]]
    inputs = [inputs for label, inputs in trace[:-1]]
    outputs = [inputs for label, inputs in trace[1:]]
    return [{
            'label': label,
            'inputs': [p for x, p in enumerate(inputs) if x in desired_inputs[label]],
            'outputs': [p for x, p in enumerate(outputs) if x in desired_outputs[label]]
            } for label, inputs, outputs in zip(labels, inputs, outputs)]


def obfuscate_inputs(trace, obfuscated_inputs):
    labels = [label for label, inputs in trace[:-1]]
    inputs = [inputs for label, inputs in trace[:-1]]
    outputs = [inputs for label, inputs in trace[1:]]
    return [{
            'label': label,
            'inputs': [n for i, n in enumerate(inputs) if i in desired_inputs[label] and i not in obfuscated_inputs],
            'outputs': [p for x, p in enumerate(outputs) if x in desired_outputs[label]]
            } for label, inputs, outputs in zip(labels, inputs, outputs)]


with open(root+file) as f:
    types = getTypes(f)

    eventRE = re.compile("(\w+) (.*)")

    traces = []
    trace = []

    for line in f.readlines():
        line = line.strip()
        if line == "":
            continue
        if line == "trace":
            if trace != []:
                traces.append(trace)
                trace = []
            continue
        match = eventRE.search(line)
        label = match.group(1)
        inputs = [valueOf(i) for valueOf, i in zip(types[label], match.group(2).split(" "))]
        trace.append((label, inputs))
    traces.append(trace)

traces = [trace for trace in traces if len(trace) >= 5]
print(len(traces), "traces in total")
for x in enumerate([len(t) for t in traces]):
    print(x)

traces = random.sample(traces, 2*numTraces)

io_traces = [format_trace(t) for t in traces]

with open(newRoot+outfile+"-original-train", 'w') as f:
    print_original_trace(f, traces[:numTraces])

with open(newRoot+outfile+"-original-test", 'w') as f:
    print_original_trace(f, traces[numTraces:])

with open(newRoot+outfile+"-train.json", 'w') as f:
    print("[\n" + ",  \n".join(["  [\n    " + ",\n    ".join([json.dumps(event) for event in trace]) + "\n  ]" for trace in io_traces[:numTraces]]) + "\n]", file=f)

with open(newRoot+outfile+"-test.json", 'w') as f:
    print("[\n" + ",  \n".join(["  [\n    " + ",\n    ".join([json.dumps(event) for event in trace]) + "\n  ]" for trace in io_traces[numTraces:]]) + "\n]", file=f)

for var in [item for sublist in desired_outputs.values() for item in sublist]:
    print("sbatch bessemer-run.sh 873365 958765 27335 "+outfile+f"-obfuscated-{varname(var)} gp")
    obfuscated_inputs = [var]
    obfuscated_traces = [obfuscate_inputs(t, obfuscated_inputs) for t in traces]
    
    with open(newRoot+outfile+f"-obfuscated-{varname(var)}-train.json", 'w') as f:
        print("[\n" + ",  \n".join(["  [\n    " + ",\n    ".join([json.dumps(event) for event in trace]) + "\n  ]" for trace in obfuscated_traces[:numTraces]]) + "\n]", file=f)
    
    with open(newRoot+outfile+f"-obfuscated-{varname(var)}-test.json", 'w') as f:
        print("[\n" + ",  \n".join(["  [\n    " + ",\n    ".join([json.dumps(event) for event in trace]) + "\n  ]" for trace in obfuscated_traces[numTraces:]]) + "\n]", file=f)