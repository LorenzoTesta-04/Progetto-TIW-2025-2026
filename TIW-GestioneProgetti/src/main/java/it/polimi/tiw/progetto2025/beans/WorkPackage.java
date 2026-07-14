package it.polimi.tiw.progetto2025.beans;

import java.util.ArrayList;
import java.util.List;

public class WorkPackage 
{
    private int idProgetto;
    private int codiceWP;
    private String titolo;
    private int meseInizio;
    private int meseFine; 
    private List<Task> tasks;

    public WorkPackage() {}

    public WorkPackage(int idProgetto, int codiceWP, String titolo, int meseInizio, int meseFine) {
        this.idProgetto=idProgetto;
        this.codiceWP=codiceWP;
        this.titolo=titolo;
        this.meseInizio=meseInizio;
        this.meseFine=meseFine;
        this.tasks=new ArrayList<>();
    }

    public int getIdProgetto() {return idProgetto;}
    public int getCodiceWP() {return codiceWP;}
    public String getTitolo() {return titolo;}
    public int getMeseInizio() {return meseInizio;}
    public int getMeseFine() {return meseFine;}
    public List<Task> getTasks() {return this.tasks;}
    
    public void setIdProgetto(int idProgetto) {this.idProgetto=idProgetto;}
    public void setCodiceWP(int codiceWP) {this.codiceWP=codiceWP;}
    public void setTitolo(String titolo) {this.titolo=titolo;}
    public void setMeseInizio(int meseInizio) {this.meseInizio=meseInizio;}
    public void setMeseFine(int meseFine) {this.meseFine=meseFine;}
    public void setTasks(List<Task> tasks) {this.tasks=tasks;}
}