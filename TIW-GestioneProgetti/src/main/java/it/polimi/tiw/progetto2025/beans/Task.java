package it.polimi.tiw.progetto2025.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {

    private int id;
    private int idWp;
    private String nomeTask;
    private String descrizione;
    private Map<Integer, Integer> orePreviste=new HashMap<>();
    private Map<Integer, Integer> oreLavorate=new HashMap<>();
    private List<Integer> collaboratori=new ArrayList<>();
    private int numeroOrdine;

    public Task() {}

    public int getId() {return id;}
    public int getIdWp() {return idWp;}
    public String getNomeTask() {return nomeTask;}
    public String getDescrizione() {return descrizione;}
    public Map<Integer, Integer> getOrePreviste() {return orePreviste;}
    public Map<Integer, Integer> getOreLavorate() {return oreLavorate;}
    public List<Integer> getCollaboratori() {return collaboratori;}
    public int getNumeroOrdine() {return numeroOrdine;}
    public int getMeseInizio() 
    {
    	if(orePreviste==null || orePreviste.isEmpty()) return 0;
    	return Collections.min(orePreviste.keySet());
    }
    
    public int getMeseFine()
    {
    	if(orePreviste==null || orePreviste.isEmpty()) return 0;
    	return Collections.max(orePreviste.keySet());
    }
    
    public int getOrePrevisteTotali() 
    {
    	if(orePreviste==null || orePreviste.isEmpty()) return 0;
    	
    	int oreTotali=0;
    	
    	for(Integer o:orePreviste.values())
    		oreTotali+=o;
    	
    	return oreTotali;
    }
    
    public int getOreLavorateTotali()
    {
    	if(oreLavorate==null || oreLavorate.isEmpty()) return 0;
    	
    	int oreTotali=0;
    	
    	for(Integer o:oreLavorate.values())
    		oreTotali+=o;
    	
    	return oreTotali;
    }
    
    public void setId(int id) {this.id=id;}
    public void setIdWp(int idWp) {this.idWp=idWp;}
    public void setNomeTask(String nomeTask) {this.nomeTask=nomeTask;}
    public void setDescrizione(String descrizione) {this.descrizione=descrizione;}
    public void setOrePreviste(Map<Integer, Integer> orePrecedenti) {this.orePreviste=orePrecedenti;}
    public void setOreLavorate(Map<Integer, Integer> orePrecedenti) {this.oreLavorate=orePrecedenti;}
	public void setCollaboratori(List<Integer> idCollaboratoriAssegnati) {this.collaboratori=idCollaboratoriAssegnati;}
	public void setNumeroOrdine(int numeroOrdine) {this.numeroOrdine=numeroOrdine;}
}