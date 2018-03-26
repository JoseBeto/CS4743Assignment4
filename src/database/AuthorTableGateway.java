package database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.AuditTrailEntry;
import model.Author;

public class AuthorTableGateway {
	private Connection conn;
	
	public AuthorTableGateway(Connection conn) {
		this.conn = conn;
	}
	
	public void updateAuthor(Author author) throws AppException {
		createAuditTrails(author);
		
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("update author set first_name = ?, last_name = ?, dob = ?, "
					+ "gender = ?, web_site = ? where id = ?");
			st.setString(1, author.getFirstName());
			st.setString(2, author.getLastName());
			st.setDate(3, Date.valueOf(author.getDoB()));
			st.setString(4, author.getGender());
			st.setString(5, author.getWebsite());
			st.setInt(6, author.getId());
			
			if(!author.getLastModified().isEqual(getLastModified(author)))
				throw new AppException("not in sync");
			
			st.executeUpdate();
			
			author.setLastModified(getLastModified(author));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
	}
	
	public LocalDateTime getLastModified(Author author) {
		LocalDateTime lastModified = null;
		
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("select * from author where id = ?");
			st.setInt(1, author.getId());
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				lastModified = rs.getTimestamp("last_modified").toLocalDateTime();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
		
		return lastModified;
	}

	public void addAuthor(Author author) throws AppException {
		PreparedStatement st = null;
		try {
			String statement = "insert into author (first_name, last_name, "
					+ "dob, gender, web_site) values (?, ?, ?, ?, ?)";
			
			st = conn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
			st.setString(1, author.getFirstName());
			st.setString(2, author.getLastName());
			st.setDate(3, Date.valueOf(author.getDoB()));
			st.setString(4, author.getGender());
			st.setString(5, author.getWebsite());
			st.executeUpdate();
			
			ResultSet rs = st.getGeneratedKeys();
			rs.next();
			addAuditEntry(getAuthorById(rs.getInt(1)), "Author added");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
	}
	
	public void deleteAuthor(Author author) throws AppException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("delete from author where id = ?");
			st.setInt(1, author.getId());
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
	}
	
	public ObservableList<Author> getAuthors() throws AppException {
		ObservableList<Author> authors = FXCollections.observableArrayList();
		
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("select * from author order by first_name");
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				Author author = new Author(rs.getString("first_name"), rs.getString("last_name"),
						rs.getDate("dob").toLocalDate(), rs.getString("gender"), 
						rs.getString("web_site"), rs.getTimestamp("last_modified").toLocalDateTime());
				author.setGateway(this);
				author.setId(rs.getInt("id"));
				authors.add(author);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
		return authors;
	}
	
	public Author getAuthorById(int id) {
		PreparedStatement st = null;
		Author author = null;
		
		try {
			st = conn.prepareStatement("select * from author where id = ?");
			st.setInt(1, id);
			ResultSet rs = st.executeQuery();
			
			while(rs.next()) {
				author = new Author(rs.getString("first_name"), rs.getString("last_name"),
						rs.getDate("dob").toLocalDate(), rs.getString("gender"), 
						rs.getString("web_site"), rs.getTimestamp("last_modified").toLocalDateTime());
				author.setGateway(this);
				author.setId(rs.getInt("id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
		return author;
	}
	
	public void createAuditTrails(Author author) throws AppException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("select * from author where id = ?");
			st.setInt(1, author.getId());
			ResultSet rs = st.executeQuery();
			
			while(rs.next()) {
				if(!author.getFirstName().equals(rs.getString("first_name")))
					addAuditEntry(author, "First name changed from " + rs.getString("first_name") + " to " + author.getFirstName());
				if(!author.getLastName().equals(rs.getString("last_name")))
					addAuditEntry(author, "Last name changed from " + rs.getString("last_name") + " to " + author.getLastName());
				if(!author.getDoB().equals(rs.getString("dob")))
					addAuditEntry(author, "Date of birth changed from " + rs.getString("dob") + " to " + author.getDoB());
				if(!author.getGender().equals(rs.getString("gender")))
					addAuditEntry(author, "Gender changed from " + rs.getString("gender") + " to " + author.getGender());
				if(!author.getWebsite().equals(rs.getString("web_site")))
					addAuditEntry(author, "Website changed from " + rs.getString("web_site") + " to " + author.getWebsite());
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
	}
	
	public void addAuditEntry(Author author, String message) throws AppException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("insert into author_audit_trail (author_id, entry_msg)"
					+ " values (?, ?)");
			st.setInt(1, author.getId());
			st.setString(2, message);
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
	}
	
	public ObservableList<AuditTrailEntry> getAuditTrails(Author author) throws AppException {
		ObservableList<AuditTrailEntry> auditTrailEntries = FXCollections.observableArrayList();
		
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("select * from author_audit_trail where author_id = ? order by date_added");
			st.setInt(1, author.getId());
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				AuditTrailEntry auditTrailEntry = new AuditTrailEntry(rs.getTimestamp("date_added"),
						rs.getString("entry_msg"));
				auditTrailEntry.setId(rs.getInt("id"));
				auditTrailEntries.add(auditTrailEntry);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AppException(e);
		} finally {
			try {
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new AppException(e);
			}
		}
		return auditTrailEntries;
	}
}
