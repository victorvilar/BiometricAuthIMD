/**
* @package: BD
* @class: ConexionBD
* @author: Napster2011
* @version: 1.0
*/
package BD;

/**
* @librerias importadas
* @author: Napster2011
*/
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class ConexionBD {
	/**
	 * @atributos
	 * @author: Napster2011
	 * @access: public
	 */
	public String puerto = "3306";
	public String nomservidor = "localhost";
	public String db = "huellas";
	public String user = "root";
	public String pass = "root";
	Connection conn = null;

	/**
	 * @function: conectar
	 * @author: Napster2011
	 * @description: esta funcion se encarga de conectar la base de datos con el
	 *               servidor
	 * @access: public
	 * @return
	 */
	public Connection conectar() {
		try {
			String ruta = "jdbc:mysql://";
			String servidor = nomservidor + ":" + puerto + "/";
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(ruta + servidor + db, user, pass);
			if (conn != null) {
				System.out.println("Conecci�n a base de datos listo�");
			} else if (conn == null) {
				throw new SQLException();
			}
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		} catch (ClassNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Se produjo el siguiente error: " + e.getMessage());
		} catch (NullPointerException e) {
			JOptionPane.showMessageDialog(null, "Se produjo el siguiente error: " + e.getMessage());
		} finally {
			return conn;
		}
	}

	/**
	 * @function: desconectar
	 * @author: Napster2011
	 * @description: esta funcion se encarga de realizar la desconexion de la
	 *               base de datos con el servidor
	 * @access: public
	 * @return
	 */
	public void desconectar() {
		conn = null;
		System.out.println("Desconexion a base de datos listo�");
	}
}