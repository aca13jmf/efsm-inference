import com.microsoft.z3
import exceptions._
import java.io._
import scala.io.Source
import scala.util.Random
import sys.process._
import Types._
import org.apache.commons.io.FileUtils

import java.util.UUID.randomUUID
import java.util.Collections

import scala.collection.JavaConversions._

import mint.inference.gp.Generator;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.terminals.IntegerVariableAssignmentTerminal;
import mint.inference.gp.tree.terminals.StringVariableAssignmentTerminal;
import mint.inference.gp.tree.terminals.VariableTerminal;
import mint.tracedata.types.BooleanVariableAssignment;
import mint.tracedata.types.IntegerVariableAssignment;
import mint.tracedata.types.StringVariableAssignment;
import mint.tracedata.types.VariableAssignment;
import mint.inference.gp.LatentVariableGP;
import mint.inference.evo.GPConfiguration;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import isabellesal._

import java.util.ArrayList

object Dirties {
  def foldl[A, B](f: A => B => A, b: A, l: List[B]): A =
    l.par.foldLeft(b)(((x, y) => (f(x))(y)))
  // l.foldLeft(b)(((x, y) => (f(x))(y)))

  def toZ3(v: Value.value): String = v match {
    case Value.Numa(n) => s"(Num ${Code_Numeral.integer_of_int(n).toString})"
    case Value.Str(s) => s"""(Str "${s}")"""
  }

  def toZ3(a: VName.vname): String = a match {
    case VName.I(n) => s"i${Code_Numeral.integer_of_nat(n)}"
    case VName.R(n) => s"r${Code_Numeral.integer_of_nat(n)}"
  }

  def toZ3(a: AExp.aexp[VName.vname]): String = a match {
    case AExp.L(v) => s"(Some ${toZ3(v)})"
    case AExp.V(v) => s"${toZ3(v)}"
    case AExp.Plus(a1, a2) => s"(Plus ${toZ3(a1)} ${toZ3(a2)})"
    case AExp.Minus(a1, a2) => s"(Minus ${toZ3(a1)} ${toZ3(a2)})"
    case AExp.Times(a1, a2) => s"(Times ${toZ3(a1)} ${toZ3(a2)})"
  }

  def toZ3Native(v: Value.value): String = v match {
    case Value.Numa(n) => s"${Code_Numeral.integer_of_int(n).toString}"
    case Value.Str(s) => s""""${s}""""
  }

  def toZ3Native(a: AExp.aexp[VName.vname]): String = a match {
    case AExp.L(v) => s"${toZ3Native(v)}"
    case AExp.V(v) => s"${toZ3(v)}"
    case AExp.Plus(a1, a2) => s"(+ ${toZ3Native(a1)} ${toZ3Native(a2)})"
    case AExp.Minus(a1, a2) => s"(- ${toZ3Native(a1)} ${toZ3Native(a2)})"
    case AExp.Times(a1, a2) => s"(* ${toZ3Native(a1)} ${toZ3Native(a2)})"
  }

  def toZ3(g: GExp.gexp[VName.vname]): String = g match {
    case GExp.Bc(a) => a.toString()
    case GExp.Eq(a1, a2) => s"(Eq ${toZ3(a1)} ${toZ3(a2)})"
    case GExp.Gt(a1, a2) => s"(Gt ${toZ3(a1)} ${toZ3(a2)})"
    case GExp.In(v, l) => l.slice(0, 2).map(x => s"(Eq ${toZ3(v)} (Some ${toZ3(x)}))").fold("false")((x, y) => s"(Or ${x} ${y})")
    case GExp.Nor(g1, g2) => {
      s"(Nor ${toZ3(g1)} ${toZ3(g2)})"
    }
  }

  var sat_memo = scala.collection.immutable.Map[GExp.gexp[VName.vname], Boolean](GExp.Bc(true) -> true, GExp.Bc(false) -> false)

  def check(z3String: String, expected: z3.Status = z3.Status.SATISFIABLE): Boolean = {
    val ctx = new z3.Context()
    val solver = ctx.mkSimpleSolver()
    solver.fromString(z3String)
    val sat = solver.check()
    ctx.close()

    return sat == expected
  }

  def satisfiable(g: GExp.gexp[VName.vname]): Boolean = {
    if (sat_memo isDefinedAt g)
      return sat_memo(g)
    else {
      var z3String = Config.z3Head
      z3String += GExp.enumerate_vars(g).map(v => s"(declare-const ${toZ3(v)} (Option Value))").foldLeft("")(((x, y) => x + y + "\n"))
      z3String += s"\n(assert (= true ${toZ3(g)}))"

      val sat = check(z3String)
      sat_memo = sat_memo + (g -> sat)
      return sat
    }
  }

  def gexpImplies(g1: GExp.gexp[VName.vname], g2: GExp.gexp[VName.vname]): Boolean = {
    var z3String = Config.z3Head
    z3String += (GExp.enumerate_vars(g1) ++ GExp.enumerate_vars(g2)).distinct.map(v => s"(declare-const ${toZ3(v)} (Option Value))").foldLeft("")(((x, y) => x + y + "\n"))

    z3String += s"""
        (assert
          (not
            (=>
              (= true ${toZ3(g1)})
              (= true ${toZ3(g2)})
            )
          )
        )
        """

    val sat = check(z3String, z3.Status.UNSATISFIABLE)
    println(s"(=> ${PrettyPrinter.gexpToString(g1)}  ${PrettyPrinter.gexpToString(g2)})")

    return sat
  }

  def randomMember[A](f: FSet.fset[A]): Option[A] = f match {
    case FSet.fset_of_list(l) =>
      if (l == List())
        return None
      if (l.length == 1)
        return Some(l.head)
      else
        Some(Random.shuffle(l).head)
  }

  def addLTL(f: String, e: String) = {
    val lines = Source.fromFile(f).getLines().toList.dropRight(1)
    val pw = new PrintWriter(new File(f))
    (lines :+ (e + "\nEND")).foreach(pw.println)
    pw.close()
  }

  def maxNum(e1: IEFSM, e2: IEFSM = FSet.bot_fset): Int = {
    TypeConversion.toInt(Code_Numeral.integer_of_int(Inference.max_int(FSet.sup_fset(e1, e2))) + 1)
  }

  // We're checking for the existence of a trace that gets us to the right
  // states in the respective machines such that we can take t2 so we check
  // the global negation of this to see if there's a counterexample
  def alwaysDifferentOutputsDirectSubsumption[A](e1: IEFSM, e2: IEFSM, s1: Nat.nat, s2: Nat.nat, t2: Transition.transition_ext[A]): Boolean = {
    Log.root.debug("alwaysDifferentOutputsDirectSubsumption")
    if (Config.config.skip) {
      return true
    }
    val f = "intermediate_" + randomUUID.toString().replace("-", "_")
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(e1), "e1", Inference.tm(e2), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME o to o_e1 IN e1) || (RENAME o to o_e2 IN e2);\n" +
      s"canTake: THEOREM composition |- G(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)} => NOT(input_sequence ! size?(i) = ${Code_Numeral.integer_of_nat(Transition.Arity(t2))} AND ${efsm2sal.guards2sal(Transition.Guard(t2))}));")
    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{${maxNum(e1, e2)}}!canTake'").!!
    if (!output.toString.startsWith("Counterexample")) {
      Log.root.warn(s"""Path failure:\n
      G(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)} => NOT(input_sequence ! size?(I) = ${Code_Numeral.integer_of_nat(Transition.Arity(t2))} AND ${efsm2sal.guards2sal(Transition.Guard(t2))}));\n
      sal-smc --assertion='${f}{${maxNum(e1, e2)}}!canTake'""")
    }
    FileUtils.deleteQuietly(new File(s"salfiles/${f}.sal"))
    return (output.toString.startsWith("Counterexample"))
  }

  // Check that whenever we're in state s, register r is always undefined
  def initiallyUndefinedContextCheck(e: TransitionMatrix, r: Nat.nat, s: Nat.nat): Boolean = {
    Log.root.debug("initiallyUndefinedContextCheck")
    val f = "intermediate_" + randomUUID.toString().replace("-", "_")
    TypeConversion.efsmToSALTranslator(e, f)

    addLTL("salfiles/" + f + ".sal", s"  initiallyUndefined: THEOREM MichaelsEFSM |- G(cfstate = ${TypeConversion.salState(s)} => r__${Code_Numeral.integer_of_nat(r)} = None);")

    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{${Code_Numeral.integer_of_int(EFSM.max_int(e)) + 1}}!initiallyUndefined'").!!
    if (output.toString != "proved.\n") {
      print(output)
    }
    FileUtils.deleteQuietly(new File(s"salfiles/${f}.sal"))
    return (output.toString == "proved.\n")
  }

  // We're looking to confirm that traces which get us to s1 in e1 and s2 in e2
  // always leave register r (in e2) holding value v
  def generaliseOutputContextCheck(
    v: Value.value,
    r: Nat.nat,
    s1: Nat.nat,
    s2: Nat.nat,
    e1: IEFSM,
    e2: IEFSM): Boolean = {
    Log.root.debug("generaliseOutputContextCheck")
    val f = "intermediate_" + randomUUID.toString().replace("-", "_")
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(e1), "e1", Inference.tm(e2), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME o to o_e1 IN e1) || (RENAME o to o_e2 IN e2);\n" +
      s"checkRegValue: THEOREM composition |- G(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)} => r__${Code_Numeral.integer_of_nat(r)}.2 = Some(${TypeConversion.salValue(v)}));")
    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{${maxNum(e1, e2)}}!checkRegValue'").!!
    if (output.toString != "proved.\n") {
      print(output)
    }
    FileUtils.deleteQuietly(new File(s"salfiles/${f}.sal"))
    return (output.toString == "proved.\n")
  }

  // Here we check to see if globally we can never be in both states
  // If there's a counterexample then there exists a trace which gets us to
  // both states. This should trivially hold.
  def acceptsAndGetsUsToBoth(
    a: IEFSM,
    b: IEFSM,
    s1: Nat.nat,
    s2: Nat.nat): Boolean = {
    // Log.root.debug("acceptsAndGetsUsToBoth - " + FSet.size_fset(Inference.S(b)))

    if (Config.config.skip)
      return true
    val f = "intermediate_" + randomUUID.toString().replace("-", "_")
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(a), "e1", Inference.tm(b), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME o to o_e1 IN e1) || (RENAME o to o_e2 IN e2);\n" +
      s"getsUsToBoth: THEOREM composition |- G(NOT(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)}));")
    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{${maxNum(a, b)}}!getsUsToBoth'").!!
    FileUtils.deleteQuietly(new File(s"salfiles/${f}.sal"))
    if (!output.toString.startsWith("Counterexample")) {
      Log.root.warn(s"""Path failure:\n
        getsUsToBoth: THEOREM composition |- G(NOT(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)}));\n
        sal-smc --assertion='${f}{${maxNum(a, b)}}!getsUsToBoth'""")
    }
    return (output.toString.startsWith("Counterexample"))
  }

  // Confirm the existance of a trace which gets us to the correct respective
  // states but produces a context in which register r holds the wrong value
  def diffOutputsCtx[A, B](
    e1: IEFSM,
    e2: IEFSM,
    s1: Nat.nat,
    s2: Nat.nat,
    t1: Transition.transition_ext[A],
    t2: Transition.transition_ext[B]): Boolean = {
    Log.root.debug("diffOutputsCtx")
    if (Transition.Outputs(t1) == Transition.Outputs(t2))
      return false
    val f = "intermediate_" + randomUUID.toString().replace("-", "_")
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(e1), "e1", Inference.tm(e2), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME o to o_e1 IN e1) || (RENAME o to o_e2 IN e2);\n" +
      s"""diffOutputs: THEOREM composition |- G(
          NOT(
            cfstate.1 = ${TypeConversion.salState(s1)} AND
            cfstate.2 = ${TypeConversion.salState(s2)} AND
            input_sequence ! size?(i) = ${Code_Numeral.integer_of_nat(Transition.Arity(t1))} AND ${efsm2sal.guards2sal(Transition.Guard(t1))} AND
            input_sequence ! size?(i) = ${Code_Numeral.integer_of_nat(Transition.Arity(t2))} AND ${efsm2sal.guards2sal(Transition.Guard(t2))} AND
            X(o_e1 /= o_e2)
          )
        );""")
    val cmd = s"cd salfiles; sal-smc --assertion='${f}{${maxNum(e1, e2)}}!diffOutputs'"
    val output = Seq("bash", "-c", cmd).!!
    FileUtils.deleteQuietly(new File(s"salfiles/${f}.sal"))
    return (output.toString.startsWith("Counterexample"))
  }

  def canStillTake[A, B](
    e1: IEFSM,
    e2: IEFSM,
    s1: Nat.nat,
    s2: Nat.nat,
    t1: Transition.transition_ext[Unit],
    t2: Transition.transition_ext[Unit]): Boolean = {
    Log.root.debug("canStillTake")
    // return true // TODO: Delete this

    val f = "intermediate_" + randomUUID.toString().replace("-", "_")
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(e1), "e1", Inference.tm(e2), "e2", f, false)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME o to o_e1 IN e1) || (RENAME o to o_e2 IN e2);\n" +
      s"""canStillTake: THEOREM composition |- G(
                NOT(
                    cfstate.1 = ${TypeConversion.salState(s1)} =>
                    cfstate.2 = ${TypeConversion.salState(s2)} =>
                    ((input_sequence ! size?(i) = ${Code_Numeral.integer_of_nat(Transition.Arity(t2))} AND ${efsm2sal.guards2sal_num(Transition.Guard(t2), Nat.Nata(2))}) =>
                  (input_sequence ! size?(i) = ${Code_Numeral.integer_of_nat(Transition.Arity(t1))} AND ${efsm2sal.guards2sal_num(Transition.Guard(t1), Nat.Nata(1))}))
                )
              );""")
    val cmd = s"cd salfiles; sal-smc --assertion='${f}{${maxNum(e1, e2)}}!canStillTake'"
    val output = Seq("bash", "-c", cmd).!!
    FileUtils.deleteQuietly(new File(s"salfiles/${f}.sal"))
    return (output.toString.startsWith("Counterexample"))
  }

  def scalaDirectlySubsumes(
    e1: IEFSM,
    e2: IEFSM,
    s1: Nat.nat,
    s2: Nat.nat,
    t1: Transition.transition_ext[Unit],
    t2: Transition.transition_ext[Unit]): Boolean = {
    return false
    // Log.root.debug(s"Does ${PrettyPrinter.show(t1)} directly subsume ${PrettyPrinter.show(t2)}? (y/N)")
    // val subsumes = scala.io.StdIn.readLine() == "y"
    // subsumes
  }

  def getTypes(i: List[Value.value]): List[String] = i.map {
    case Value.Numa(_) => "Int"
    case Value.Str(_) => "Str"
  }

  def getTypes(r: Map[Nat.nat, Option[Value.value]]): List[String] = {
    val keys = r.keySet.toList.map(x => Code_Numeral.integer_of_nat(x))
    keys.sorted
    keys.map(key => r(Nat.Nata(key)) match {
      case Some(Value.Numa(_)) => "Int"
      case Some(Value.Str(_)) => "String"
      case None => throw new IllegalArgumentException("Got none from a map")
    })
  }

  def sortedValues(r: Map[Nat.nat, Option[Value.value]]): List[String] = {
    val keys = r.keySet.toList.map(x => Code_Numeral.integer_of_nat(x))
    keys.sorted
    keys.map(key => r(Nat.Nata(key)) match {
      case Some(Value.Numa(Int.int_of_integer(n))) => n.toString
      case Some(Value.Str(s)) => "\"" + s + "\""
      case None => throw new IllegalArgumentException("Got none from a map")
    })
  }

  var guardMap = Map[List[((List[Value.value], Map[Nat.nat, Option[Value.value]]), Boolean)], Option[GExp.gexp[VName.vname]]]()

  def findDistinguishingGuard(
    g1: (List[(List[Value.value], Map[Nat.nat, Option[Value.value]])]),
    g2: (List[(List[Value.value], Map[Nat.nat, Option[Value.value]])])): Option[(GExp.gexp[VName.vname], GExp.gexp[VName.vname])] = {

      println("findDistinguishingGuard")

    val ioPairs = (g1 zip List.fill(g1.length)(true)) ++ (g1 zip List.fill(g1.length)(false))

    if (guardMap isDefinedAt ioPairs) guardMap(ioPairs) match {
      case None => return None
      case Some(g) => return Some((g, GExp.gNot(g)))
    }

    println("Not yet defined")


    BasicConfigurator.resetConfiguration();
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.OFF);

    val gpGenerator: Generator = new Generator(new java.util.Random(Config.config.guardSeed))

    gpGenerator.addTerminals(GP.boolTerms);
    gpGenerator.addFunctions(GP.intNonTerms);
    gpGenerator.addFunctions(GP.boolNonTerms)

    var intVarVals = List(0l, 1l, 2l)
    var stringVarVals = List[String]()

    var intTerms = List[VariableTerminal[_]]()

    // No supported stringNonTerms
    var stringTerms = List[VariableTerminal[_]]()

    var stringVarNames = List[String]()
    var intVarNames = List[String]()

    val trainingSet = new HashSetValuedHashMap[java.util.List[VariableAssignment[_]], VariableAssignment[_]]()

    // g1 needs to be true
    for ((inputs, registers) <- g1) {
      var scenario = List[VariableAssignment[_]]()
      for ((ip, ix) <- inputs.zipWithIndex) ip match {
        case Value.Numa(n) => {
          intVarVals = TypeConversion.toLong(n) :: intVarVals
          intVarNames = s"i${ix}" :: intVarNames
          scenario = (new IntegerVariableAssignment(s"i${ix}", TypeConversion.toLong(n))) :: scenario
        }
        case Value.Str(s) => {
          stringVarVals = s :: stringVarVals
          stringVarNames = s"i${ix}" :: stringVarNames
          scenario = (new StringVariableAssignment(s"i${ix}", s)) :: scenario
        }
      }
      for ((r, v) <- registers) v match {
        case None => {}
        case Some(Value.Numa(n)) => {
          intVarVals = TypeConversion.toLong(n) :: intVarVals
          intVarNames = s"r${PrettyPrinter.show(r)}" :: intVarNames
          scenario = (new IntegerVariableAssignment(s"r${PrettyPrinter.show(r)}", TypeConversion.toLong(n))) :: scenario
        }
        case Some(Value.Str(s)) => {
          stringVarVals = s :: stringVarVals
          stringVarNames = s"r${PrettyPrinter.show(r)}" :: stringVarNames
          scenario = (new StringVariableAssignment(s"r${PrettyPrinter.show(r)}", s)) :: scenario
        }
      }
      trainingSet.put(scenario, new BooleanVariableAssignment("g1", true))
    }

    // g2 needs to be false if g1 is true
    for ((inputs, registers) <- g2) {
      var scenario = List[VariableAssignment[_]]()
      for ((ip, ix) <- inputs.zipWithIndex) ip match {
        case Value.Numa(n) => {
          intVarVals = TypeConversion.toLong(n) :: intVarVals
          intVarNames = s"i${ix}" :: intVarNames
          scenario = (new IntegerVariableAssignment(s"i${ix}", TypeConversion.toLong(n))) :: scenario
        }
        case Value.Str(s) => {
          stringVarVals = s :: stringVarVals
          stringVarNames = s"i${ix}" :: stringVarNames
          scenario = (new StringVariableAssignment(s"i${ix}", s)) :: scenario
        }
      }
      for ((r, v) <- registers) v match {
        case None => {}
        case Some(Value.Numa(n)) => {
          intVarVals = TypeConversion.toLong(n) :: intVarVals
          intVarNames = s"r${PrettyPrinter.show(r)}" :: intVarNames
          scenario = (new IntegerVariableAssignment(s"r${PrettyPrinter.show(r)}", TypeConversion.toLong(n))) :: scenario
        }
        case Some(Value.Str(s)) => {
          stringVarVals = s :: stringVarVals
          stringVarNames = s"r${PrettyPrinter.show(r)}" :: stringVarNames
          scenario = (new StringVariableAssignment(s"r${PrettyPrinter.show(r)}", s)) :: scenario
        }
      }
      trainingSet.put(scenario, new BooleanVariableAssignment("g2", false))
    }

    intTerms = intVarNames.distinct.map(intVarName => new IntegerVariableAssignmentTerminal(intVarName, false)) ++
      intVarVals.distinct.map(intVarVal => new IntegerVariableAssignmentTerminal(intVarVal)) ++
      intTerms
    stringTerms = stringVarNames.distinct.map(stringVarName => new StringVariableAssignmentTerminal(new StringVariableAssignment(stringVarName), false, false)) ++
      stringVarVals.distinct.map(stringVarVal => new StringVariableAssignmentTerminal(new StringVariableAssignment(stringVarVal, stringVarVal), true, false)) ++
      stringTerms

    gpGenerator.addTerminals(intTerms)
    gpGenerator.addTerminals(stringTerms)

    Log.root.debug("Guard training set: " + trainingSet)
    Log.root.debug("  Terminals: " + gpGenerator.getTerminals())

    println("Guard training set: " + trainingSet)
    println("  Terminals: " + gpGenerator.getTerminals())

    // If any of the guards need to simultaneously be true and false then stop
    if (trainingSet.keys().stream().anyMatch(x => trainingSet.get(x).size() > 1))
      return None

    var gp = new LatentVariableGP(gpGenerator, trainingSet, new GPConfiguration(50, 0.9f, 1f, 5, 2));

    try {
      val best: Node[VariableAssignment[_]] = gp.evolve(50).asInstanceOf[Node[VariableAssignment[_]]]
      Log.root.debug("  Best guard is: " + best.simp())

      val ctx = new z3.Context()
      val gexp = TypeConversion.gexpFromZ3(best.toZ3(ctx))
      ctx.close
      if (gp.isCorrect(best)) {
        println("  Best guard is: " + best.simp())
        Log.root.debug("  Best guard is correct")
        guardMap = guardMap + (ioPairs -> Some(gexp))
        return Some((gexp, GExp.gNot(gexp)))
      } else {
        guardMap = guardMap + (ioPairs -> None)
        return None
      }
    } catch {
      case e: java.lang.IllegalArgumentException => {
        e.printStackTrace
        guardMap = guardMap + (ioPairs -> None)
        return None
      }
    }
  }

  def getUpdate(
    r: Nat.nat,
    values: List[Value.value],
    train: List[(List[Value.value], (Map[Nat.nat, Option[Value.value]], Map[Nat.nat, Option[Value.value]]))]): Option[AExp.aexp[VName.vname]] = {

    Log.root.debug("  Getting update")

    val r_index = TypeConversion.toInt(r)
    val ioPairs = (train.map {
      case (inputs, (aregs, pregs)) => pregs(r) match {
        case None => throw new IllegalStateException("Got None from registers")
        case Some(v) => ((inputs, aregs.filterKeys(_ == r)), v)
      }
    }).distinct

    BasicConfigurator.resetConfiguration();
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.OFF);

    val gpGenerator: Generator = new Generator(new java.util.Random(Config.config.updateSeed))
    gpGenerator.addFunctions(GP.intNonTerms);

    var (intTerms, stringTerms) = GP.getValueTerminals(values)

    val trainingSet = new HashSetValuedHashMap[java.util.List[VariableAssignment[_]], VariableAssignment[_]]()
    var stringVarNames = List[String]()
    var intVarNames = List[String]()

    for (t <- ioPairs) t match {
      case ((inputs, anteriorRegs), updatedReg) => {
        var scenario = List[VariableAssignment[_]]()
        for ((ip, ix) <- inputs.zipWithIndex) ip match {
          case Value.Numa(n) => {
            intVarNames = s"i${ix}" :: intVarNames
            scenario = (new IntegerVariableAssignment(s"i${ix}", TypeConversion.toLong(n))) :: scenario
          }
          case Value.Str(s) => {
            stringVarNames = s"i${ix}" :: stringVarNames
            scenario = (new StringVariableAssignment(s"i${ix}", s)) :: scenario
          }
        }
        for ((k: Nat.nat, v: Option[Value.value]) <- anteriorRegs) v match {
          case None => throw new IllegalStateException("Got None from registers")
          case Some(Value.Numa(n)) => {
            if (k == r) {
              intVarNames = s"r${TypeConversion.toInt(k)}" :: intVarNames
              scenario = (new IntegerVariableAssignment(s"r${TypeConversion.toInt(k)}", TypeConversion.toLong(n))) :: scenario
            }
          }
          case Some(Value.Str(s)) => {
            if (k == r) {
              stringVarNames = s"r${TypeConversion.toInt(k)}" :: stringVarNames
              scenario = (new StringVariableAssignment(s"r${TypeConversion.toInt(k)}", s)) :: scenario
            }
          }
        }
        updatedReg match {
          case Value.Numa(n) => {
            intVarNames = s"r${r_index}" :: intVarNames
            trainingSet.put(scenario, new IntegerVariableAssignment("r" + r_index, TypeConversion.toLong(n)))
          }
          case Value.Str(s) => {
            stringVarNames = s"r${r_index}" :: stringVarNames
            trainingSet.put(scenario, new StringVariableAssignment("r" + r_index, s))
          }
        }
      }
    }

    for (intVarName <- intVarNames.distinct) {
      intTerms = (new IntegerVariableAssignmentTerminal(intVarName, false)) :: intTerms
    }

    for (stringVarName <- stringVarNames.distinct) {
      stringTerms = (new StringVariableAssignmentTerminal(new StringVariableAssignment(stringVarName), false, false)) :: stringTerms
    }

    gpGenerator.addTerminals(intTerms)
    gpGenerator.addTerminals(stringTerms)

    Log.root.debug("    Update training set: " + trainingSet)
    // Log.root.debug("  Int terminals: " + intTerms)
    // Log.root.debug("  String terminals: " + stringTerms)

    if (trainingSet.keys().stream().anyMatch(x => x.size() == 0 && trainingSet.get(x).size() > 1)) {
      Log.root.debug("    Multiple updates for no input")
      return None
    }

    var gp = new LatentVariableGP(gpGenerator, trainingSet, new GPConfiguration(100, 0.9f, 1f, 5, 2));

    val best = gp.evolve(100).asInstanceOf[Node[VariableAssignment[_]]]

    Log.root.debug("    Best update is: " + best)

    if (gp.isCorrect(best)) {
      Log.root.debug("    Best update is correct")
      return Some((TypeConversion.toAExp(best)))
    } else {
      return None
    }
  }

  def getOutput(
    maxReg: Nat.nat,
    values: List[Value.value],
    inputs: List[List[Value.value]],
    registers: List[Map[Nat.nat, Option[Value.value]]],
    outputs: List[Value.value],
    latentVariable: Boolean = false): Option[(AExp.aexp[VName.vname], Map[VName.vname, String])] = {
    Log.root.debug("Getting Output...")

    if (outputs.distinct.length == 1) {
      Log.root.debug("  Singleton literal output")
      return Some(AExp.L(outputs(0)), Map())
    }

    val r_index = TypeConversion.toInt(maxReg) + 1

    val ioPairs = (inputs zip registers zip outputs).distinct

    BasicConfigurator.resetConfiguration();
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.OFF);

    val gpGenerator: Generator = new Generator(new java.util.Random(Config.config.outputSeed))
    gpGenerator.addFunctions(GP.intNonTerms);

    var (intTerms, stringTerms) = GP.getValueTerminals(values)

    val trainingSet = new HashSetValuedHashMap[java.util.List[VariableAssignment[_]], VariableAssignment[_]]()
    var stringVarNames = List[String]()
    var intVarNames = List[String]()
    var latentInt = true

    for (t <- ioPairs) t match {
      case ((inputs, anteriorRegs), output) => {
        var scenario = List[VariableAssignment[_]]()
        for ((ip, ix) <- inputs.zipWithIndex) ip match {
          case Value.Numa(n) => {
            intVarNames = s"i${ix}" :: intVarNames
            scenario = (new IntegerVariableAssignment(s"i${ix}", TypeConversion.toLong(n))) :: scenario
          }
          case Value.Str(s) => {
            stringVarNames = s"i${ix}" :: stringVarNames
            scenario = (new StringVariableAssignment(s"i${ix}", s)) :: scenario
          }
        }
        for ((k: Nat.nat, v: Option[Value.value]) <- anteriorRegs) v match {
          case None => throw new IllegalStateException("Got None from registers")
          case Some(Value.Numa(n)) => {
            intVarNames = s"r${TypeConversion.toInt(k)}" :: intVarNames
            scenario = (new IntegerVariableAssignment(s"r${TypeConversion.toInt(k)}", TypeConversion.toLong(n))) :: scenario
          }
          case Some(Value.Str(s)) => {
            stringVarNames = s"r${TypeConversion.toInt(k)}" :: stringVarNames
            scenario = (new StringVariableAssignment(s"r${TypeConversion.toInt(k)}", s)) :: scenario
          }
        }
        output match {
          case Value.Numa(n) => {
            intVarNames = s"r${r_index}" :: intVarNames
            trainingSet.put(scenario, new IntegerVariableAssignment("o", TypeConversion.toLong(n)))
          }
          case Value.Str(s) => {
            latentInt = false
            stringVarNames = s"r${r_index}" :: stringVarNames
            trainingSet.put(scenario, new StringVariableAssignment("o", s))
          }
        }
      }
    }

    if (!latentVariable) {
      Log.root.debug("  No latent variable")
      intVarNames = intVarNames.filter(_ != s"r${r_index}")
      stringVarNames = stringVarNames.filter(_ != s"r${r_index}")
    } else {
      Log.root.debug(s"  Latent variable: r$r_index")
    }

    for (intVarName <- intVarNames.distinct) {
      if (intVarName.startsWith("i"))
        intTerms = (new IntegerVariableAssignmentTerminal(intVarName, false)) :: intTerms
      else
        intTerms = (new IntegerVariableAssignmentTerminal(intVarName, true)) :: intTerms
    }

    for (stringVarName <- stringVarNames.distinct) {
      if (stringVarName.startsWith("i"))
        stringTerms = (new StringVariableAssignmentTerminal(new StringVariableAssignment(stringVarName), false, false)) :: stringTerms
      else
        stringTerms = (new StringVariableAssignmentTerminal(new StringVariableAssignment(stringVarName), false, true)) :: stringTerms
    }

    gpGenerator.addTerminals(intTerms)
    gpGenerator.addTerminals(stringTerms)

    Log.root.debug("  Output training set: " + trainingSet)
    Log.root.debug("  Int terminals: " + intTerms)
    // Log.root.debug("  String terminals: " + stringTerms)

    // If we have a key that's empty but returns more than one value then we need a latent variable
    if ((!latentVariable) && trainingSet.keys().stream().anyMatch(x => x.size() == 0 && trainingSet.get(x).size() > 1)) {
      if (latentInt) {
        val best = new IntegerVariableAssignmentTerminal(f"r$r_index", true).asInstanceOf[Node[VariableAssignment[_]]]
        Log.root.debug("  Secret best output is: " + best)
        return Some((TypeConversion.toAExp(best), getTypes(best)))
      }
      else {
        val best = new StringVariableAssignmentTerminal(new StringVariableAssignment(f"r$r_index"), false, true).asInstanceOf[Node[VariableAssignment[_]]]
        Log.root.debug("  Secret best output is: " + best)
        return Some((TypeConversion.toAExp(best), getTypes(best)))
      }
    }

    var gp = new LatentVariableGP(gpGenerator, trainingSet, new GPConfiguration(100, 0.9f, 1f, 3, 2));
    gp.setSeeds(intTerms)

    val best = gp.evolve(100).asInstanceOf[Node[VariableAssignment[_]]]

    Log.root.debug("  Best output is: " + best)

    if (gp.isCorrect(best)) {
      Log.root.debug("  Best output is correct")
      return Some((TypeConversion.toAExp(best), getTypes(best)))
    } else {
      return getOutput(maxReg, values, inputs, registers, outputs, true)
    }
  }

  def getTypes(best: Node[VariableAssignment[_]]): Map[VName.vname, String] = {
    var types = Map[VName.vname, String]()

    for (v <- asScalaSet(best.varsInTree)) {
      if (!v.isConstant)
        types = types + (TypeConversion.vnameFromString(v.getName) -> v.typeString)
    }
    return types
  }

    def getRegs(
    types: Map[VName.vname, String],
    i: List[Value.value],
    f: AExp.aexp[VName.vname],
    v: Value.value): Map[Nat.nat, Option[Value.value]] = {
    val expVars: List[VName.vname] = Lista.sorted_list_of_set(AExp.enumerate_vars(f))
    val definedVars = (0 to i.length).map(i => VName.I(Nat.Nata(i)))
    val undefinedVars = expVars.filter(v => !definedVars.contains(v))

    // Log.root.debug("\noutputFun: " + PrettyPrinter.aexpToString(f, true) + " = " + PrettyPrinter.valueToString(v))
    // Log.root.debug("  expVars: " + expVars)
    // Log.root.debug("  undefinedVars: " + undefinedVars)

    var inputs: String = ""
    for (v <- expVars) {
      inputs += f"(${PrettyPrinter.vnameToString(v)} ${TypeConversion.expandTypeString(types(v))})"
    }
    var z3String: String = f"(define-fun f (${inputs}) ${TypeConversion.typeString(v)} \n  ${toZ3Native(f)}\n)\n"
    for (v <- undefinedVars) {
      z3String += f"(declare-const ${PrettyPrinter.vnameToString(v)} ${TypeConversion.expandTypeString(types(v))})\n"
    }
    val args = expVars.zipWithIndex.map {
      case (v: VName.vname, k: Int) =>
        if (definedVars.contains(v)) {
          PrettyPrinter.valueToString(i(k))
        } else {
          PrettyPrinter.vnameToString(v)
        }
    }

    if (args.length == 0) {
      val assertion: String = "(assert (= " + PrettyPrinter.valueToString(v) + " f))"
      z3String += assertion
    } else {
      val assertion: String = "(assert (= " + PrettyPrinter.valueToString(v) + " (f " + args.mkString(" ") + ")))"
      z3String += assertion
    }

    val ctx = new z3.Context()
    val solver = ctx.mkSimpleSolver()

    solver.fromString(z3String)
    solver.check()
    val model: z3.Model = solver.getModel

    var regs: Map[Nat.nat, Option[Value.value]] = Map()
    for (f <- model.getConstDecls) {
      val constInterp = model.getConstInterp(f)
      regs = regs + (Nat.Nata(BigInt(f.getName.toString.substring(1).toInt)) -> Some(TypeConversion.toValue(model.getConstInterp(f))))
    }
    ctx.close()
    regs
  }
}
