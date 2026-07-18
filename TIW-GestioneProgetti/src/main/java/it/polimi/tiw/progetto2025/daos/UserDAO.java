package it.polimi.tiw.progetto2025.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import it.polimi.tiw.progetto2025.beans.User;
import it.polimi.tiw.progetto2025.beans.exceptions.CheckAuthException;
import it.polimi.tiw.progetto2025.beans.exceptions.DBException;

/**
 * Data Access Object (DAO) per la gestione della persistenza e dell'autenticazione
 * degli utenti all'interno del sistema.
 */
public class UserDAO
{
	private Connection connection;

	public UserDAO(Connection connection)
	{
		this.connection=connection;
	}

	/**
     * Registra un nuovo utente nel database.
     * @param utente l'oggetto User contenente i dati anagrafici e le credenziali da inserire
     * @throws RuntimeException incapsula una SQLException qualora lo username sia già presente o vi siano violazioni di vincoli
     */
    public void createUser(User utente) {
        // Esplicitazione dei campi per garantire robustezza rispetto a modifiche dello schema DDL
        String query="INSERT INTO "+MyDAO.USER_TABLE+" (username, password, nome, cognome, admin, foto) VALUES (?, ?, ?, ?, ?, ?)";
        String hashedPassword=UUID.nameUUIDFromBytes(utente.getPassword().getBytes()).toString();
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) {	
            pstmt.setString(1, utente.getUsername());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, utente.getNome());
            pstmt.setString(4, utente.getCognome());
            pstmt.setBoolean(5, utente.isAdmin());
            pstmt.setString(6, utente.getProfilePicturePath());
            
            pstmt.executeUpdate();
        } 
        catch (SQLException e) 
        { 
            throw new RuntimeException("Impossibile creare l'utente (Username esistente o errore di persistenza): "+e.getMessage(), e);
        }
    }
	
    /**
     * Autentica un utente nel sistema verificando la corrispondenza di username e password.
     * Se l'utente non è amministratore, ne determina dinamicamente i ruoli operativi (Manager/Collaboratore).
     * 
     * @param username lo username inserito nel form di login
     * @param password la password in chiaro inserita nel form di login
     * @return l'oggetto User accreditato e configurato con i relativi ruoli
     * @throws DBException in caso di errori strutturali o di connettività del database
     * @throws CheckAuthException qualora le credenziali inserite siano errate o non corrispondano ad alcun utente
     */
    public User checkAuth(String username, String password) throws DBException, CheckAuthException 
    {
        String hashedPassword=UUID.nameUUIDFromBytes(password.getBytes()).toString();
        String query="SELECT id, username, nome, cognome, admin, foto FROM "+MyDAO.USER_TABLE+" WHERE password=? AND username=?";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, username);
            
            try(ResultSet result=pstmt.executeQuery()) 
            {
                if(result.next()) 
                {
                    User user=new User();
                    user.setID(result.getInt("id"));
                    user.setUsername(result.getString("username"));
                    user.setNome(result.getString("nome"));
                    user.setCognome(result.getString("cognome"));
                    user.setProfilePicturePath(result.getString("foto"));
                    
                    if(result.getBoolean("admin")) user.setAdmin(true);
                     else 
                     {
                        // Ottimizzazione: Chiamate separate isolate in metodi ad alte prestazioni
                        user.setCollaborator(checkIfCollaborator(user.getID()));
                        user.setManager(checkIfManager(user.getID()));
                    }
        
                    return user;
                } 
                else 
                {
                    throw new CheckAuthException();
                }
            }
        } 
        catch (SQLException e) 
        {
            throw new DBException(e.getMessage());
        }
    }
	
    /**
     * Verifica in modo ottimizzato se l'utente è manager di almeno un progetto corrente.
     * Utilizza una sottoquery condizionale EXISTS per arrestare la scansione al primo riscontro positivo.
     * @param userId identificativo dell'utente da controllare
     * @return {@code true} se l'utente possiede la responsabilità di almeno un progetto, {@code false} altrimenti
     * @throws SQLException se si verifica un errore di accesso al database
     */
    private boolean checkIfManager(int userId) throws SQLException 
    {
        String query="SELECT 1 FROM "+MyDAO.PROJECT_TABLE+" WHERE idResponsabile=? LIMIT 1";
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, userId);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                return rs.next(); // true se trova almeno un record
            }
        }
    }
	
    /**
     * Verifica in modo ottimizzato se l'utente figura come collaboratore operativo.
     * Sfrutta un controllo di presenza rapido sulla tabella delle ore rendicontate.
     * 
     * @param userId identificativo dell'utente da controllare
     * @return {@code true} se l'utente ha ore registrate in un task, {@code false} altrimenti
     * @throws SQLException se si verifica un errore di accesso al database
     */
    private boolean checkIfCollaborator(int userId) throws SQLException 
    {
        String query="SELECT 1 FROM "+MyDAO.ORE_LAVORATE_TABLE+" WHERE idCollaboratore=? LIMIT 1";
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, userId);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                return rs.next();
            }
        }
    }
	
    /**
     * Estrae la lista completa di tutti gli utenti registrati che non sono amministratori.
     * Il risultato viene ordinato alfabeticamente per cognome e poi per nome.
     * @return lista di oggetti User non amministratori
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<User> findAllCollaborators() throws SQLException 
    {
        List<User> list=new ArrayList<>();
        String query="SELECT id, username, nome, cognome FROM "+MyDAO.USER_TABLE+" WHERE admin=false ORDER BY cognome, nome ASC";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query);
             ResultSet rs=pstmt.executeQuery()) 
        {
            while (rs.next()) 
            {
                User u=new User();
                u.setID(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setNome(rs.getString("nome"));
                u.setCognome(rs.getString("cognome"));
                list.add(u);
            }
        }
        
        return list;
    }
	
    /**
     * Recupera l'elenco dei soli collaboratori operativi che afferiscono ai progetti 
     * supervisionati da un determinato Project Manager.
     * @param idManager identificativo del manager incaricato dei progetti
     * @return lista di utenti (collaboratori) distinti legati operativamente al manager
     * @throws SQLException se si verifica un errore durante l'esecuzione della JOIN multilivello
     */
    public List<User> findManagedCollaborators(int idManager) throws SQLException 
    {
        List<User> list=new ArrayList<>();
        String query="SELECT DISTINCT u.id, u.nome, u.cognome " +
                       "FROM "+MyDAO.USER_TABLE+" u " +
                       "JOIN "+MyDAO.ORE_LAVORATE_TABLE+" ol ON u.id=ol.idCollaboratore " +
                       "JOIN "+MyDAO.TASK_TABLE+" t ON ol.idTask=t.idTask " +
                       "JOIN "+MyDAO.WORK_PACKAGE_TABLE+" wp ON t.idWp=wp.idWp " +
                       "JOIN "+MyDAO.PROJECT_TABLE+" p ON wp.idProgetto=p.id " +
                       "WHERE p.idResponsabile=? "+
                       "ORDER BY u.cognome, u.nome ASC";
    	
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idManager);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                while (rs.next()) 
                {
                    User u=new User();
                    u.setID(rs.getInt("id"));
                    u.setNome(rs.getString("nome"));
                    u.setCognome(rs.getString("cognome"));
                    list.add(u);
                }
            }
        }
    	
        return list;
    }

    /**
     * Estrae le informazioni anagrafiche essenziali di un singolo utente partendo dal suo ID.
     * @param idUtente identificativo univoco dell'utente da cercare
     * @return un oggetto User popolato con i dati corrispondenti, oppure vuoto se non trovato
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public User getUser(int idUtente) throws SQLException 
    {
        User user=new User();
        String query="SELECT id, username, nome, cognome, foto FROM "+MyDAO.USER_TABLE+" WHERE id=?";
	    
        try(PreparedStatement pstmt=connection.prepareStatement(query)) 
        {
            pstmt.setInt(1, idUtente);
            try(ResultSet rs=pstmt.executeQuery()) 
            {
                if(rs.next()) 
                {
                    user.setID(rs.getInt("id"));
                    user.setNome(rs.getString("nome"));
                    user.setCognome(rs.getString("cognome"));
                    user.setUsername(rs.getString("username"));
                    user.setProfilePicturePath(rs.getString("foto"));
                }
            }
        }
	    
        return user;
    }
    
    /**
     * Estrae l'elenco di tutti gli utenti con ruolo di tecnico.
     * @return lista di utenti non amministratori (tecnici)
     * @throws SQLException se si verifica un errore di accesso al database
     */
    public List<User> findAllTechnicians() throws SQLException 
    {
        List<User> list=new ArrayList<>();
        String query="SELECT id, nome, cognome FROM "+MyDAO.USER_TABLE+" WHERE admin=0";
        
        try(PreparedStatement pstmt=connection.prepareStatement(query); ResultSet rs=pstmt.executeQuery()) 
        {
            while (rs.next()) 
            {
                User u=new User();
                u.setID(rs.getInt("id"));
                u.setNome(rs.getString("nome"));
                u.setCognome(rs.getString("cognome"));
                list.add(u);
            }
        }
        
        return list;
    }
}
