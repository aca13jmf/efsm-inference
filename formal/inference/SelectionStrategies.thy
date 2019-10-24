section\<open>Selection Strategies\<close>
text\<open>The strategy used to idenfity and prioritise states to be merged plays a big part in how the
final model turns out. This theory file presents a number of different selection strategies.\<close>

theory SelectionStrategies
imports Inference
begin

fun bool2nat :: "bool \<Rightarrow> nat" where
  "bool2nat True = 1" |
  "bool2nat False = 0"

subsection\<open>One of the simplest strategies is to look only at the labels and arities of outgoing
transitions of each state. Pairs of states are ranked by how many transitions with the same label
and arity they have in common.\<close>
definition naive_score :: strategy where
  "naive_score t1ID t2ID e = (let t1 = get_by_ids e t1ID; t2 = get_by_ids e t2ID in bool2nat (Label t1 = Label t2 \<and> Arity t1 = Arity t2))"

(* One point if they're equal *)
definition naive_score_eq :: strategy where
  "naive_score_eq t1ID t2ID e = bool2nat ((get_by_ids e t1ID) = (get_by_ids e t2ID))"

(* One point if one subsumes the other *)
definition naive_score_subsumption :: "strategy" where
  "naive_score_subsumption t1ID t2ID e = (let t1 = get_by_ids e t1ID; t2 = get_by_ids e t2ID; s = origin t1ID e in bool2nat (directly_subsumes e e s s t1 t2) + bool2nat (directly_subsumes e e s s t2 t1))"

(* One point each for equal label, arity, and outputs *)
definition naive_score_outputs :: strategy where
  "naive_score_outputs t1ID t2ID e = (let
    t1 = get_by_ids e t1ID;
    t2 = get_by_ids e t2ID in
    bool2nat (Label t1 = Label t2) + bool2nat (Arity t1 = Arity t2) + bool2nat (Outputs t1 = Outputs t2))"

(* Functions with same label, and input and output arities contribute one point for each guard    *)
(* and output they share. *)
definition naive_score_comprehensive :: strategy where
  "naive_score_comprehensive t1ID t2ID e = (let x = e|t1ID|; y = e|t2ID| in 
                                    if Label x = Label y \<and> Arity x = Arity y then
                                      if length (Outputs x) = length (Outputs y) then
                                        card (set (Guard x) \<inter> set (Guard y)) + length (filter (\<lambda>(p1, p2). p1 = p2) (zip (Outputs x) (Outputs y)))
                                      else 0
                                    else 0)"

(* Functions with same label, and input and output arities contribute one point for each guard    *)
(* and output they share. Transitions which are exactly equal get a very high score. *)
definition naive_score_comprehensive_eq_high :: strategy where
  "naive_score_comprehensive_eq_high t1ID t2ID e = (let x = e|t1ID|; y = e|t2ID| in 
                                           if x = y then
                                             100
                                           else
                                             if Label x = Label y \<and> Arity x = Arity y then
                                               if length (Outputs x) = length (Outputs y) then
                                                 card (set (Guard x) \<inter> set (Guard y)) + length (filter (\<lambda>(p1, p2). p1 = p2) (zip (Outputs x) (Outputs y)))
                                               else 0
                                             else 0)"

(* Orders by the origin state so we should get zipping down the PTA *)
definition origin_states :: strategy where
  "origin_states t1ID t2ID e = (let
    t1Orig = origin t1ID e;
    t2Orig = origin t2ID e in
    if t1Orig = 9 \<and> t2Orig = 58 then 1000 else
    0)"

end