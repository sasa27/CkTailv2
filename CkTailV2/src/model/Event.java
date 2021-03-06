package model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;

import main.split.Regex;


public class Event {

	public static String from = "Host";
	public static String to = "Dest";

	private String session;
	
	private String label;
	private String separator = "|||";
	private ArrayList<String> params;
	private ArrayList<String> paramsSess;
	public String date;  //public for debug only


	public Event(String line, Matcher m) {
		date = m.group("date");
		label = m.group("label");
		params = new ArrayList<String>();
		paramsSess = new ArrayList<String>();
		int n = 1;
		try {
			while(m.group("param" + n) != null) {
				if (m.group("param" + n).contains("session=")) {
					//System.out.println(m.group("param" + n));
					session = m.group("param" + n);
				}
				else {
					params.add(m.group("param" + n));
					
				}
				paramsSess.add(m.group("param" + n)); 
				n++;
			}
		}catch(IllegalArgumentException e) {
			//end of while
		}
	}



	public String getLabel() {
		return label;
	}

	public ArrayList<String> getparams() {
		return params;
	}
	
	public ArrayList<String> getparamsSess() {
		return paramsSess;
	}
	
	public String getFrom() {
		String res = "";
		for (String param:params) {
			if (param.startsWith(from + "=")){
				return param.substring(from.length() + 1);
			}
		}
		System.err.println("no From in :" + this.toString());
		System.exit(3);
		return res;
	}

	public String  getTo() {
		String res = "";
		for (String param:params) {
			if (param.startsWith(to + "=")){
				return param.substring(to.length() + 1);
			}
		}
		System.err.println("no From in :" + this.toString());
		System.exit(3);
		return res;
	}

	public Date getDate(Regex regex) {
		SimpleDateFormat sdf = regex.getSDF();
		Date d = null;
		try {
			d = sdf.parse(date);
		} catch (ParseException e) {
			System.err.println("problem with date format");
			System.exit(3);
		}
		return d;
	}

	public String toString() {
		String res = label + "(";
		//res = res + "date=" + date + ";";//for debug only
		for (String param: params) {
			res = res + param + separator;
		}
		res = res.substring(0, res.length()- separator.length());
		res = res +")";
		return res;
	}

	public String debug() {
		String res = label + "(";
		res = res + date;
		res = res + params.get(0);
		res = res + params.get(1);
		res = res +")\n";
		return res;
	}

	public boolean dataSimilarity(Event ai) {
		ArrayList<String> paramsi = ai.getparamsSess();
		ArrayList<String> paramsj = this.getparamsSess();
		for (String parami: paramsi) {
			if (!(parami.contains(from) || parami.contains(to))) {
				for (String paramj: paramsj) {
					if (paramj.equals(parami)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	//only for our cases
	public boolean isReq() {
		if (!this.toString().contains("esponse") & !this.toString().contains("Resp") & !isInter()) {
			//System.out.println(this);
			return true;
		}
		else {
			return false;
		}
	}

	public boolean isResp() {
		if (this.toString().contains("esponse") | this.toString().contains("Resp")) {
			return true;
		}
		else {
			return false;
		}
	}


	public boolean isInter() {
		if (!this.toString().contains("Host=") | !this.toString().contains("Dest=")) {
			return true;
		}
		if (getFrom().equals(getTo())) {
			//System.out.println(this);
			return true;
		}
		else {
			return false;
		}
	}


	public String getSessionID() {
		//String res = session.substring(this.toString().indexOf("session=") + 8);
		return session;
	}


}

