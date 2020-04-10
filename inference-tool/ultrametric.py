#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Feb 11 15:05:24 2020

@author: michael
"""

import json
from numpy import mean
import re
import math
import glob
import os
from itertools import takewhile, dropwhile
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import rcParams

state_re = re.compile("INFO  ROOT - states: (\d+)")
transition_re = re.compile("INFO  ROOT - transitions: (\d+)")
transition_re = re.compile("INFO  ROOT - transitions: (\d+)")
runtime_re = re.compile("INFO  ROOT - Completed in (\d+)h (\d+)m (\d+).\d+s")


def total_states():
    with open(root + "log") as f:
        for line in f:
            match = state_re.search(line)
            if match:
                return int(match.group(1))


def total_transitions():
    with open(root + "log") as f:
        for line in f:
            match = transition_re.search(line)
            if match:
                return int(match.group(1))


def total_runtime():
    with open(root + "log") as f:
        for line in f:
            match = runtime_re.search(line)
            if match:
                return ((int(match.group(1)) * 60) +
                        (int(match.group(2))) +
                        int(match.group(3))/60.0)


def match_prefix(expected, actual):
    prefix = []
    for e1, e2 in zip(expected, actual):
        if e1 != e2:
            return (prefix, expected)
        else:
            prefix.append(e1)
    return (prefix, expected)


def levenshtein_distance(s1, s2):
    if len(s1) > len(s2):
        s1, s2 = s2, s1
    distances = range(len(s1) + 1)
    for index2, char2 in enumerate(s2):
        newDistances = [index2+1]
        for index1, char1 in enumerate(s1):
            if char1 == char2:
                newDistances.append(distances[index1])
            else:
                newDistances.append(1 + min((distances[index1],
                                             distances[index1+1],
                                             newDistances[-1])))
        distances = newDistances
    return distances[-1]


def output_square_distance(o1, o2):
    if isinstance(o1, int) and isinstance(o2, int):
        return (o1 - o2) ** 2
    else:
        return levenshtein_distance(str(o1), str(o2)) ** 2


def outputs_distance(O1, O2):
    return sum([output_square_distance(o1, o2) for o1, o2 in zip(O1, O2)])


def to_num(o):
    if isinstance(o, int):
        return o
    else:
        return levenshtein_distance("", str(o))


def match(event):
    return event['expected'] == event['actual']


def split_trace(trace, rejected=None):
    if rejected is not None:
            return (
                    list(takewhile(match, trace)),
                    list(dropwhile(match, trace)),
                    rejected
                   )
    return (list(takewhile(match, trace)), list(dropwhile(match, trace)))


columns = ['states', 'transitions', 'min', 'avg', 'ultra', 'prop',
           'sensitivity', 'rmse', 'nrmse', 'state coverage', 'runtime',
           'transition coverage', 't1', 't2', 't3']

programs = ["liftDoors", "spaceInvaders"]

for program in programs:
    configurations = sorted(["-".join(os.path.basename(f).split("-")[1:]) for f in glob.glob(f"results/{program}*")])
    
    config_data = []
    
    for config in configurations:
        config = f"{program}30-{config}"
        roots = ([d for d in glob.glob(f"results/{config}/{config}-*/")
                  if os.path.isdir(d) and os.path.exists(d + "testLog.json")])
    
        data = pd.DataFrame(columns=columns)
    
        for root in roots:
            info = {}
            with open(root + "testLog.json") as f:
                log = json.loads("".join(f.readlines()))
    
            info['states'] = total_states()
            info['transitions'] = total_transitions()
            info['runtime'] = total_runtime()

            triples = [split_trace(trace['trace'], trace['rejected']) for trace in log]
            info['t1'] = mean([len(x)/(len(x) + len(y) + len(z)) for x, y, z in triples]),
            info['t2'] = mean([len(y)/(len(x) + len(y) + len(z)) for x, y, z in triples]),
            info['t3'] = mean([len(z)/(len(x) + len(y) + len(z)) for x, y, z in triples]),

            lengths = [len(x)/(len(x) + len(y) + len(z)) for x, y, z in triples]
            # lengths = [len(t) for t, _, _ in triples]

            # Minimum number of events before we can tell the models apart - useless
            info['min'] = min(lengths)
            # Average number of events before we can tell the models apart - useless
            info['avg'] = mean(lengths)
            # Ultrametric from the paper - useless
            info['ultra'] = 2**-min(lengths)
            # Mean prop. of the trace got through before we can tell the trace apart
            info['prop'] = mean(
                    [len(p)/(len(p) + len(s1) + len(s2)) for p, s1, s2 in triples])
        
            valid_traces = sum([s1 == [] and s2 == [] for _, s1, s2 in triples])
            info['sensitivity'] = valid_traces/len(log)
    
            rmse = sum([
                        sum([
                                outputs_distance(event['expected'], event['actual'])
                                for event in obj['trace']
                            ])
                        for obj in log
                    ])
            rmse = math.sqrt(rmse)
            info['rmse'] = rmse

            outputs = set()
            for obj in log:
                for event in obj['trace']:
                    outputs = outputs.union([to_num(o) for o in event['expected']])
                    outputs = outputs.union([to_num(o) for o in event['actual']])
            info['nrmse'] = rmse/(max(outputs) - min(outputs))

            states_covered = set()
            for obj in log:
                for event in obj['trace']:
                    states_covered.add(event['currentState'])
                    states_covered.add(event['nextState'])
            info['state coverage'] = len(states_covered)/total_states()
            transitions_covered = set()
            for obj in log:
                for event in obj['trace']:
                    transitions_covered.add(tuple(event['transition']))
            info['transition coverage'] = len(transitions_covered)/total_transitions()
            data = data.append(pd.DataFrame(info, index=[root]))
        config_data.append(data)

    for column in [c for c in columns if c not in ['t1', 't2', 't3']]:
        fig1, ax1 = plt.subplots(figsize=(8, 4))
        ax1.set_title(f"{program} {column}")
        bp = ax1.boxplot(
                [data[column].astype(float) for data in config_data],
                medianprops={"linewidth": 0},
                boxprops={"linewidth": 0},
                whiskerprops={"linewidth": 0},
                capprops={"linewidth": 0}
             )

        # Shift the median lines so they look good on pgf
        for median in bp['medians']:
            x, y = median.get_data()
            ax1.plot(x+0.003, y, color="k", linewidth=1, solid_capstyle="butt", zorder=0)
        
        # Only draw the boxes that have a nonzero size
        for box in bp['boxes']:
            x, y = box.get_data()
            if len(set(y)) > 1:
                ax1.plot(x, y, color="k", linewidth=1, zorder=4)

        for whisker, cap in zip(bp['whiskers'], bp['caps']):
            w_x, w_y = whisker.get_data()
            c_x, c_y = cap.get_data()
            if len(set(w_y)) > 1:
                ax1.plot(w_x, w_y, color="k", linewidth=1)
                ax1.plot(c_x, c_y, color="k", linewidth=1)

        ax1.set_xticklabels(
                [' '.join(config.split('-')) for config in configurations],
                rotation=45,
                fontsize=12,
                ha='right',
                va='top',
                ma='right'
            )

        plt.tight_layout()
        plt.savefig(f"graphs/{program}-{column}.pgf")

    fig1, ax1 = plt.subplots(figsize=(8, 4))
    t1Means = [mean(data['t1']) for data in config_data]
    t2Means = [mean(data['t2']) + t1Means[i] for i, data in enumerate(config_data)]
    t3Means = [mean(data['t3']) + t2Means[i] for i, data in enumerate(config_data)]

    ind = range(len(t1Means))

    p3 = ax1.bar(ind, t3Means, color='red')
    p2 = ax1.bar(ind, t2Means, color='orange')
    p1 = ax1.bar(ind, t1Means, color='green')

    ttl = plt.title(f"{program} traces")
#    ttl.set_position([.5, 1.35])
#    rcParams['axes.titlepad'] = 20 

    ax1.set_xticks(ind)
    ax1.set_xticklabels(
            [' '.join(config.split('-')) for config in configurations],
            rotation=45,
            fontsize=12,
            ha='right',
            va='top',
            ma='right'
        )
    plt.tight_layout()
    plt.legend((p1[0], p2[0], p3[0]),
               ('Correct', 'Incorrect', "Rejected"),
               loc='upper center',
               bbox_to_anchor=(0.5, 1.3),
               ncol=3)
    plt.savefig(f"graphs/{program}-traces.pgf")
    plt.show()

    with open(f"graphs/{program}-graphs.tex", 'w') as f:
        print("\\documentclass{article}", file=f)
        print("\\usepackage{tikz}", file=f)
        print("\\usepackage{a4wide}", file=f)
        print("\\begin{document}", file=f)
        print("\\centering", file=f)
        for column in [c for c in columns + ["traces"] if c not in ['t1', 't2', 't3']]:
            print("\\resizebox{\\textwidth}{!}{\\input{"+program+"-"+column+".pgf}}", file=f)
        print("\\end{document}", file=f)
