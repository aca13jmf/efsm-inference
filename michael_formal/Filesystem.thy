theory Filesystem
imports EFSM
begin

(* Takes a user ID and stores it in r1 *)
definition login :: "transition" where
"login \<equiv> \<lparr>
        Label = ''login'',
        Arity = 1,
        Guard = [],
        Outputs = [],
        Updates = [
                    (''r1'', (V ''i1'')) (* Store the user ID in r1 *)
                  ]
      \<rparr>"

(* Logs out the current user *)
definition logout :: "transition" where
"logout \<equiv> \<lparr>
        Label = ''logout'',
        Arity = 0,
        Guard = [], (* No guards *)
        Outputs = [],
        Updates = [ (* Two updates: *)
                    (''r1'', (V ''r1'')), (* Value of r1 remains unchanged *)
                    (''r2'', (V ''r2'')), (* Value of r2 remains unchanged *)
                    (''r3'', (V ''r3''))  (* Value of r3 remains unchanged *)
                  ]
      \<rparr>"

definition "write" :: "transition" where
"write \<equiv> \<lparr>
        Label = ''write'',
        Arity = 1,
        Guard = [], (* No guards *)
        Outputs = [],
        Updates = [ 
                    (''r1'', (V ''r1'')), (* Value of r1 remains unchanged *)
                    (''r2'', (V ''i1'')), (* Write the input to r2 *)
                    (''r3'', (V ''r1''))  (* Store the writer in r3 *)
                  ]
      \<rparr>"

definition read_success :: "transition" where
"read_success \<equiv> \<lparr>
        Label = ''read'',
        Arity = 0,
        Guard = [Eq (V ''r1'') (V ''r3'')], (* No guards *)
        Outputs = [(V ''r2'')],
        Updates = [ (* Two updates: *)
                    (''r1'', (V ''r1'')), (* Value of r1 remains unchanged *)
                    (''r2'', (V ''r2'')), (* Value of r2 remains unchanged *)
                    (''r3'', (V ''r3''))  (* Value of r3 remains unchanged *)
                  ]
      \<rparr>"

definition read_fail :: "transition" where
"read_fail \<equiv> \<lparr>
        Label = ''read'',
        Arity = 0,
        Guard = [Ne (V ''r1'') (V ''r3'')], (* No guards *)
        Outputs = [(N 0)],
        Updates = [ 
                    (''r1'', (V ''r1'')), (* Value of r1 remains unchanged *)
                    (''r2'', (V ''r2'')), (* Value of r2 remains unchanged *)
                    (''r3'', (V ''r3''))  (* Value of r3 remains unchanged *)
                  ]
      \<rparr>"

definition filesystem :: "efsm" where
"filesystem \<equiv> \<lparr> 
          S = [1,2],
          s0 = 1,
          T = \<lambda> (a,b) .
              if (a,b) = (1,2) then [login]
              else if (a,b) = (2,1) then [logout]
              else if (a,b) = (2,2) then [write, read_success, read_fail]
              else []
         \<rparr>"

lemmas fs_simp = filesystem_def login_def logout_def write_def read_success_def read_fail_def

primrec all :: "'a list \<Rightarrow> ('a \<Rightarrow> bool) \<Rightarrow> bool" where
  "all [] _ = True" |
  "all (h#t) f = (if f h then all t f else False)"

(* step :: efsm \<Rightarrow> statename \<Rightarrow> registers \<Rightarrow> label \<Rightarrow> inputs \<Rightarrow> (statename \<times> outputs \<times> registers) option *)
(* observe_trace :: "efsm \<Rightarrow> statename \<Rightarrow> registers \<Rightarrow> trace \<Rightarrow> observation" where *)

(* noChangeOwner: THEOREM filesystem |- G(cfstate /= NULL_STATE) => FORALL (owner : UID): G((label=write AND r_1=owner) => F(G((label=read AND r_1/=owner) => X(op_1_read_0 = accessDenied)))); *)

lemma aux_write: "concat (map (\<lambda>s'. concat (map (\<lambda>t. if Label t = ''write'' \<and>
                                                 (\<exists>y. find (\<lambda>x. x = t) (T filesystem (2, s')) = Some y) \<and>
                                                 Suc 0 = Arity t \<and> apply_guards (Guard t) (\<lambda>x. if x = index 1 then content else join_ir [] r (1 + 1) x) r
                                              then [(s', t)] else [])
                                      (T filesystem (2, s'))))
                   (S filesystem)) = [(2, write)]"
  by (simp add: fs_simp)

lemma joinir: "(\<lambda>x. if x = index 1 then content else join_ir [] r (1 + 1) x) = (\<lambda>x. if x = ''i1'' then content else r x)"
  apply (rule ext)
  by (simp add: index_def shows_stuff)

lemma aux1: "(r ''r3'' \<noteq> r ''r1'') \<Longrightarrow> step filesystem 2 r ''read'' [] = Some (2, [0], r)"
  apply (simp add: fs_simp step_def index_def shows_stuff join_def)
  apply (rule ext)
  by simp

lemma aux2: "step filesystem 2 r ''write'' [content] = Some (2, [], (\<lambda>x. if x = ''r1'' \<or> x = ''r3'' then r ''r1'' else (if x = ''r2'' then content else r x)))"
  apply (simp add: step_def aux_write)
  apply safe
   apply (simp add: write_def)
  apply (rule ext)
  by (simp add: joinir write_def)

lemma "(r ''r3'' \<noteq> r ''r1'') \<Longrightarrow> valid_trace filesystem t \<Longrightarrow> all (observe_trace filesystem 2 r t) (\<lambda>e. e = [] \<or> e = [0])"
proof (induction t)
  case Nil
  then show ?case by simp
next
  case (Cons a t)
  then show ?case
    apply simp
    apply (cases "step filesystem 2 r (fst a) (snd a)")
     apply simp
    apply simp
    apply safe
    apply (cases "a = (''read'', [])")
     apply (simp add: aux1)
     apply (cases "a = (''write'', [content])")
    apply simp
      apply (simp add: aux2)
    



qed


end