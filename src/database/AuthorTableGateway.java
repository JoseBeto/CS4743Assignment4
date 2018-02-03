package database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Author;

public class AuthorTableGateway {
	private Connection conn;
	
	public AuthorTableGateway(Connection conn) {
		this.conn = conn;
	}
	
	public void updateAuthor(Author author) throws AppException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("update author set first_name = ?, last_name = ?, date = ?, "
					+ "gender = ?, website = ? where id = ?");
			st.setString(1, author.getFirstName());
			st.setString(2, author.getLastName());
			st.setDate(3, author.getDoB());
			st.setString(4, author.getGender());
			st.setString(5, author.getWebsite());
			st.setInt(6, author.getId());
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
						rs.getDate("dob").toLocalDate(), rs.getString("gender"));
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
}
