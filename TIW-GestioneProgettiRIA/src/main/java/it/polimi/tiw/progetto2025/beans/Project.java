package it.polimi.tiw.progetto2025.beans;

import java.util.List;

public class Project
{
	public enum projectState {
		CREATO,
		ASSEGNATO,
		CONCLUSO
	}
    
    private int id;
    private String nomeProgetto;
    private int idResponsabile;
    private int durata;
    private projectState stato;
    private String responsabile;
    private List<WorkPackage> wps;

    public Project() {}

    public int getId() {return id;}
    public String getNomeProgetto() {return nomeProgetto;}
    public int getIdResponsabile() {return idResponsabile;}
    public int getDurata() {return durata;}
    public String getStato() {return stato.toString();}
    public String getResponsabile() {return responsabile;}
    public List<WorkPackage> getWps() {return wps;}

    public void setId(int id) {this.id=id;}
    public void setNomeProgetto(String nomeProgetto) {this.nomeProgetto=nomeProgetto;}
    public void setIdResponsabile(int idResponsabile) {this.idResponsabile=idResponsabile;}
    public void setDurata(int durata) {this.durata=durata;}
    public void setStato(projectState stato) {this.stato=stato;}
    public void setResponsabile(String responsabile) {this.responsabile=responsabile;}
    public void setWps(List<WorkPackage> wps) {this.wps=wps;}
}