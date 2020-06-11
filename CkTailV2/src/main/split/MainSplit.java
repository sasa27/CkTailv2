package main.split;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import dependencies.Dependency;
import model.Event;
import model.Trace;

public class MainSplit {

	public static String log;
	public static String output;
	public static boolean timerMode;
	public static String mode;

	public static HashMap<String, Double> means;
	public static Trace trace;
	public static Regex regex;
	public static Trace logOrigin;
	public static double interval = 1000.0;//in milliseconds 
	//public static double fact = 10.0;

	public static Dependency Dep;

	public static void main(String[] args) {
		final long timebuildingTraces1 = System.currentTimeMillis();
		ArrayList<Trace> T = new ArrayList<Trace>();
		means = new HashMap<String, Double>();
		try {
			MapperOptions.setOptions(args);
		} catch (Exception e) {
			System.err.println("pb option");
			System.exit(3);
		}
		trace = new Trace(new File(log), regex);
		System.out.println("traces built");
		final long timebuildingTraces2 = System.currentTimeMillis();
		final long timesplit1 = System.currentTimeMillis();
		Dep = new Dependency();
		logOrigin = trace;
		System.out.println("mode :" + mode);
		if (mode.equals("classic")) {
			System.out.println("mode classic");
			T.addAll(Split(trace));
		}
		else if (mode.equals("id")) {
			System.out.println("mode id");
			T.addAll(SplitID(trace));
		}
		System.out.println("split done");
		final long timesplit2 = System.currentTimeMillis();
		final long timegenfile1 = System.currentTimeMillis();
		int i = 1;
		File dir = new File(output);
		dir.mkdir();
		dir = new File(output + "/traces");
		dir.mkdir();
		try {
			for(Trace t: T) {
				File f = new File(output+"/traces/"+i);
				t.writeTrace(f);
				i++;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		final long timegenfile2 = System.currentTimeMillis();
		final long timedag1 = System.currentTimeMillis();
		Dep.buildDAG(output);

		System.out.println("Dep : " + Dep.toString());
		System.out.println("DAG :\n " + Dep.getDag());
		System.out.println("Files generated");
		final long timedag2 = System.currentTimeMillis();
		System.out.println("building Traces: " + (timebuildingTraces2 - timebuildingTraces1) + " ms");
		System.out.println("split: " + (timesplit2 - timesplit1) + " ms");
		System.out.println("file generation: " + (timegenfile2 - timegenfile1) + " ms");
		System.out.println("dag generation: " + (timedag2 - timedag1) + " ms");
		System.out.println("Total: " + (timedag2 + timesplit2 + timebuildingTraces2 + timegenfile2 - timegenfile1 -  timebuildingTraces1 - timesplit1 - timedag1) + " ms");
	}

	public static ArrayList<Trace> Split(Trace trace){
		ArrayList<Trace> T = new ArrayList<Trace>();
		int i = 1;
		Event aj = trace.getEvent(i - 1);
		while(aj.isResp()) {
			System.out.println("not a req: " + aj.debug());
			i++;
			if (i >= trace.size()) {
				return T;
			}
			aj = trace.getEvent(i - 1);
		}
		Trace tprime = new Trace();
		HashSet<ArrayList<Event>> SR = new HashSet<ArrayList<Event>>();
		HashSet<Event> OLReq = new HashSet<Event>();//
		Trace tprimeprime = new Trace(); 
		HashSet<String> SA = new HashSet<String>();
		SA.add(aj.getFrom());
		SA.add(aj.getTo());
		//i++;
		eventAnalysis:
			while (i <= trace.size()) {
				aj = trace.getEvent(i - 1);
				updateOLreq(OLReq, aj);
				//case 1
				ArrayList<Event> LReq = C1(SR, aj);	
				if(LReq != null) {
					//System.out.println("case 1 : " + aj.debug());
					tprime.addEvent(aj);
					OLReq.add(LReq.get(LReq.size() -1));
					LReq.remove(LReq.size() - 1);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					i++;
					continue eventAnalysis;	
				}
				//case 2
				if(C2(OLReq, aj)) {
					//System.out.println("case 2 : " + aj.debug());
					tprime.addEvent(aj);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					i++;
					continue eventAnalysis;	
				}
				//case 3
				LReq = C3(SR, aj);
				if (LReq != null /*&& haveResp(trace.subTrace(i, trace.size()), LReq.get(LReq.size() - 1), aj)*/) {
					//System.out.println("case 3 : " + aj.debug());
					tprime.addEvent(aj);
					LReq.add(aj);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					Dep.extend(LReq);
					i++;
					continue eventAnalysis;			
				}
				//case 4
				if (aj.isReq() && SA.contains(aj.getFrom()) && (tprime.isEmpty() || checkTime(tprime.getEvent(0), aj) /*|| checkDependencies(tprime, aj)*/)) {
					//System.out.println("case 4 : " + aj.debug());
					//checkDependencies(tprime, aj); //make dep if not done
					tprime.addEvent(aj);
					ArrayList<Event> LR = new ArrayList<Event>();
					LR.add(aj);
					SR.add(LR);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					Dep.extend(LR);
					i++;
					continue eventAnalysis;	
				}
				//case 5
				if (aj.isInter() && (tprime.isEmpty() || checkTime(tprime.getEvent(0), aj) /*|| checkDependencies(tprime, aj)*/ )) {
					//System.out.println("case 5 : " + aj.debug());
					//checkDependencies(tprime, aj); //make dep if not done
					tprime.addEvent(aj);
					i++;
					continue eventAnalysis;	
				}
				tprimeprime.addEvent(aj);
				//System.out.println("no case : " + aj.debug());
				i++;
			}
		boolean empty = true;
		for (ArrayList<Event> LReq: SR) {
			if (!LReq.isEmpty()) { 
				empty = false; 
			}
		}
		if (empty){
			T.add(tprime);
		}
		else {
			System.out.println("not finished : " + tprime.debug());

		}
		if (!tprimeprime.isEmpty()) {
			T.addAll(Split(tprimeprime));
		}
		return T;
	}

	public static ArrayList<Trace> SplitID(Trace trace){
		ArrayList<Trace> T = sepID(trace);
		HashSet<ArrayList<Event>> SR = new HashSet<ArrayList<Event>>();
		//HashSet<Event> OLReq = new HashSet<Event>();
		HashSet<String> SA = new HashSet<String>();
		for (Trace t : T) {
			int i = 1;
			Event aj = t.getEvent(i - 1);
			//System.out.println("new trace \n");
			/* initialisation */
			//ArrayList<Event> LR1 = new ArrayList<Event>();
			//LR1.add(t.getEvent(0));
			//SR.add(LR1);
			//Dep.extend(LR1);
			SA.add(aj.getFrom());
			SA.add(aj.getTo());
			eventAnalysis:
				while (i <= t.getSize()) {
					aj = t.getEvent(i - 1);
					//SA.add(aj.getFrom());
					//SA.add(aj.getTo());
					//updateOLreq(OLReq, aj);
					//case1
					ArrayList<Event> LReq = C1(SR, aj);	
					if(LReq != null) {
						//System.out.println("case1");
						//OLReq.add(LReq.get(LReq.size() -1));
						LReq.remove(LReq.size() - 1);
						SA.add(aj.getFrom());
						SA.add(aj.getTo());
						i++;
						continue eventAnalysis;	
					}
					//case 3
					LReq = C3(SR, aj);
					if (LReq != null) {
						//System.out.println("case3");
						LReq.add(aj);
						Dep.extend(LReq); 
						SA.add(aj.getFrom());
						SA.add(aj.getTo());
						i++;
						continue eventAnalysis;			
					}
					//case 4
					if (aj.isReq() && SA.contains(aj.getFrom()) && (i == 1 || checkTime(t.getEvent(0), aj) || checkDependencies(t.subTrace(0, i), aj) /*|| checkDependenciesID(t, t.subTrace(0, i), aj)*/)) {
						//checkDependenciesID(t, t.subTrace(0, i), aj); //make dep if not done
						checkDependencies(t.subTrace(0, i), aj);
						//System.out.println("case4");
						ArrayList<Event> LR = new ArrayList<Event>();
						LR.add(aj);
						SR.add(LR);
						SA.add(aj.getFrom());
						SA.add(aj.getTo());
						Dep.extend(LR);
						i++;
						continue eventAnalysis;	
					}
					//no case
					//System.out.println("no case");
					i++;
				}
		}
		return T;
	}

	private static ArrayList<Trace> sepID(Trace trace){
		ArrayList<Trace> res =  new ArrayList<Trace>();
		ArrayList<String> ids = new ArrayList<String>();
		int size = trace.getSize();
		for (int i = 0; i < size; ++i) {
			Event e = trace.getEvent(i);
			String id = e.getSessionID();
			//System.out.println(id);
			if (!ids.contains(id)) {
				ids.add(id);
				res.add(new Trace());
			}
			int index = ids.indexOf(id);
			res.get(index).addEvent(e);
		}		
		return res;
	}

	private static void updateOLreq(HashSet<Event> OLReq, Event ai) {
		HashSet<Event> Lr = new HashSet<Event>();
		if (ai.isResp()) {
			for (Event e : OLReq) {
				if (e.getTo().equals(ai.getFrom())) {
					Lr.add(e);
				}
			}
		}
		@SuppressWarnings("unchecked")
		HashSet<Event> save = (HashSet<Event>) OLReq.clone();
		for (Event e : save) {
			if (e.getFrom().equals(ai.getFrom()) | e.getTo().equals(ai.getFrom())) {
				OLReq.remove(e);
			}
		}
		OLReq.addAll(Lr);
	}


	/* check if the request aprime  has a response next */
	private static boolean haveResp(Trace t, Event last, Event ai) {
		for (int i = 0; i < t.size();++i) {
			Event aprime = t.getEvent(i);
			if (!aprime.isReq() && aprime.getFrom().equals(last.getTo()) && aprime.getTo().equals(last.getFrom())) {
				return haveResp(t.subTrace(i, t.size() - 1), ai);
			}
		}
		return false;
	}

	private static boolean haveResp(Trace t,  Event ai) {
		for (int i = 0; i < t.size();++i) {
			Event aprime = t.getEvent(i);
			if (!aprime.isReq() && aprime.getFrom().equals(ai.getTo()) && aprime.getTo().equals(ai.getFrom())) {
				return true;
			}
		}
		return false;
	}


	private static ArrayList<Event> C1(HashSet<ArrayList<Event>> SR, Event ai){
		if (ai.isReq()) {
			return null;
		}
		ArrayList<Event> res = null;
		for (ArrayList<Event> LReq : SR) {
			if (!LReq.isEmpty() && ai.getFrom().equals(LReq.get(LReq.size() - 1).getTo()) &&
					ai.getTo().equals(LReq.get(LReq.size() - 1).getFrom())) {
				if (res == null) {
					res = LReq;
				}
				else {
					return null;
				}
			}
		}
		return res;
	}

	private static boolean C2(HashSet<Event> OLReq, Event ai){
		if (ai.isReq()) {
			return false;
		}
		for (Event e : OLReq) {
			if (ai.getFrom().equals(e.getTo()) &&
					ai.getTo().equals(e.getFrom())){
				return true;
			}
		}
		return false;
	}

	private static ArrayList<Event> C3(HashSet<ArrayList<Event>> SR, Event ai){
		if (!ai.isReq()) {
			return null;
		}
		for (ArrayList<Event> LReq : SR) {
			if (!LReq.isEmpty() && ai.getFrom().equals(LReq.get(LReq.size() - 1).getTo()) && !pendingRequest(ai, SR))	{
				return LReq;
			}
		}
		return null;
	}

	/* return true if from aj has a pending request */
	public static boolean pendingRequest(Event aj, HashSet<ArrayList<Event>> SR) {
		for (ArrayList<Event> LReq: SR) {
			for (Event e: LReq) {
				if (e.getFrom().equals(aj.getFrom())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean checkTime(Event first, Event aj) {
		double diff = aj.getDate(regex).getTime() - first.getDate(regex).getTime();

		if (diff > 0 && diff < interval) { 
			return true;
		}
		return false;
	}

	/* check for data dependencies */
	public static boolean checkDependencies(Trace t, Event aj) {
		ArrayList<Event> chain = new ArrayList<Event>();
		Trace sub = logOrigin.subTrace(0,logOrigin.indexOf(aj));//
		ArrayList<Event> dependency = new ArrayList<Event>();
		int c = 0;
		ArrayList<Event> begin = sub.getEvery(aj.getFrom(), aj.getTo());
		begin.add(aj);
		for (@SuppressWarnings("unused") Event e: begin) {
			ArrayList<Event> dep = checkDependencies(sub, aj, chain);//
			if (!dep.isEmpty()) {
				c++;
				dependency.addAll(dep);
				if (c > 1) {
					return false;
				}
			}
		}
		if (c == 1 && t.containsAll(dependency) && !aj.getTo().equals(dependency.get(0).getFrom())) {
			if (!aj.isInter()) {
				//System.out.println("haj:" + aj.toString());
				//System.out.println("here:" + dependency.toString());
				ArrayList<String> ld = new ArrayList<String>();
				ld.add(aj.getTo());
				ld.add(dependency.get(0).getFrom());
				Dep.add(ld);
				System.out.println("dep:" + ld);
			}
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean checkDependenciesID(Trace origin, Trace t, Event aj) {
		ArrayList<Event> chain = new ArrayList<Event>();
		Trace sub = origin.subTrace(0,origin.indexOf(aj));//
		ArrayList<Event> dependency = new ArrayList<Event>();
		int c = 0;
		ArrayList<Event> begin = sub.getEvery(aj.getFrom(), aj.getTo());
		begin.add(aj);
		for (@SuppressWarnings("unused") Event e: begin) {
			ArrayList<Event> dep = checkDependencies(sub, aj, chain);//
			if (!dep.isEmpty()) {
				c++;
				dependency.addAll(dep);
				if (c > 1) {
					return false;
				}
			}
		}
		if (c == 1 && t.containsAll(dependency) && !aj.getTo().equals(dependency.get(0).getFrom())) {
			if (!aj.isInter()) {
				//System.out.println("haj:" + aj.toString());
				//System.out.println("here:" + dependency.toString());
				ArrayList<String> ld = new ArrayList<String>();
				ld.add(aj.getTo());
				ld.add(dependency.get(0).getFrom());
				Dep.add(ld);
				System.out.println("dep:" + ld);
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	public static ArrayList<Event> checkDependencies(Trace t, Event aj, ArrayList<Event> chain) {
		//System.out.println(t.toString());
		ArrayList<Event> res = new ArrayList<Event>();
		Event aprime = null;
		boolean one = false;
		int j = 0;
		for(int i = t.size() - 1 ; i >= 0; i--) {
			Event e = t.getEvent(i);
			
			if (e.dataSimilarity(aj) && e.getTo().equals(aj.getFrom()) && !e.isInter() && !chain.contains(e)){
				//System.out.println("event:" + aj.toString());
				//System.out.println("dep:" + e.toString());
				if (one) {
					return new ArrayList<Event>();
				}
				aprime = e;
				one = true;
				j = i;
				res.add(e);
				res.addAll(chain);
			}
		}
		if (one) {
			res = checkDependencies(t.subTrace(0, j), aprime, res);
		}
		else {
			res.addAll(chain);
		}
		return res;
	}

	/* get the mean of time between request of comp */
	public static double getMean(String comp) {
		if (means.containsKey(comp)) {
			return means.get(comp);
		}
		else {
			double sum = 0.0;
			double c = 0.0;
			Date d1 = null;
			for (Event e: trace.getSeq()) {
				if ((e.getFrom().equals(comp) || e.getTo().equals(comp)) && e.isReq()) {
					Date d2 = e.getDate(regex);
					if(d1 == null){
						d1 = d2;
						continue;
					}
					else {
						double dif = d2.getTime()-d1.getTime();
						c++;
						sum = sum + dif;
						d1 = d2;
					}
				}
			}
			System.out.println("mean(" + comp + ") : " + sum/c);
			means.put(comp, sum/c);
			return sum/c;
		}
	}

}
