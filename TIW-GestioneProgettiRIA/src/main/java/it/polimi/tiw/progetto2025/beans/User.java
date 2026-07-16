package it.polimi.tiw.progetto2025.beans;


public class User 
{
	public User() {}
	
	private int ID;
	private String username;
	private String password;
	private String nome;
	private String cognome;
	private String profilePicturePath;
	private boolean isAdmin=false;
	private boolean isManager=false;
	private boolean isCollaborator=false;
	
	public int getID() {return this.ID;}
	public String getUsername() {return this.username;}
	public String getPassword() {return this.password;}
	public String getNome() {return this.nome;}
	public String getCognome() {return cognome;}
	public String getProfilePicturePath() {return profilePicturePath;}
	public boolean isAdmin() {return isAdmin;}
	public boolean isManager() {return isManager;}
	public boolean isCollaborator() {return isCollaborator;}
	
	public void setID(int ID) {this.ID=ID;}
	public void setUsername(String username) {this.username=username;}
	public void setPassword(String password) {this.password=password;}
	public void setNome(String nome) {this.nome=nome;}
	public void setCognome(String cognome) {this.cognome=cognome;}
	public void setProfilePicturePath(String profilePicturePath) {this.profilePicturePath=profilePicturePath;}
	public void setAdmin(boolean admin) {this.isAdmin=admin;}
	public void setManager(boolean manager) {this.isManager=manager;}
	public void setCollaborator(boolean collaborator) {this.isCollaborator=collaborator;}
}