theory Convert_JSON
imports Nano_JSON Inference
begin

definition convert_args :: "json list \<Rightarrow> value list" where
  "convert_args l = map (\<lambda>x. case x of STRING s \<Rightarrow> Str s | NUMBER (INTEGER i) \<Rightarrow> Num i) l"

definition convert_event :: "json \<Rightarrow> (label \<times> value list \<times> value list)" where
  "convert_event e = (case e of 
    (OBJECT [(''label'', STRING l), (''inputs'', ARRAY i), (''outputs'', ARRAY p)]) \<Rightarrow> (String.implode l, convert_args i, convert_args p)
  )"

definition convert_aux :: "json \<Rightarrow> execution" where
  "convert_aux j = (case j of (ARRAY l) \<Rightarrow> map convert_event l)"

definition convert :: "json \<Rightarrow> log" where
  "convert j = (case j of (ARRAY l) \<Rightarrow> map convert_aux l)"

end