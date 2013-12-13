package org.raidenjpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.junit.Before;
import org.raidenjpa.entities.A;
import org.raidenjpa.entities.B;
import org.raidenjpa.entities.C;
import org.raidenjpa.util.EntityManagerUtil;
import org.raidenjpa.util.Util;


public class AbstractTestCase {

	@Before
	public void setUp() {
		truncate();
		
		A a = new A("a", 1);
		B b = new B("b");
		C c = new C("c");
		
		EntityManager em = EntityManagerUtil.em();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		a = em.merge(a);
		b = em.merge(b);
		c = em.merge(c);
		
		a.setB(b);
		b.setC(c);
		
		tx.commit();
		em.close();
	}
	
	public void truncate() {
		DataSource ds = null;
		Connection conn = null;
		try {
			ds = (DataSource) new InitialContext().lookup("jdbc/PDocDS");
			conn = ds.getConnection();
			hsqlTruncate(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} finally {
			Util.close(conn);
		}
	}
	
	private void hsqlTruncate(Connection conn) {
		executeSQL(conn, "TRUNCATE SCHEMA public AND COMMIT");
		List<String> sqls = mountRestartAutoIds(conn);
		for (String sql : sqls) {
			executeSQL(conn, sql);
		}
	}

	private static List<String> mountRestartAutoIds(Connection conn) {
		ResultSet rs = null;
		try {
			DatabaseMetaData meta = conn.getMetaData();
			rs = meta.getColumns(null, null, null, null);
			List<String> ret = new ArrayList<String>();
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				String columnName = rs.getString("COLUMN_NAME");
				String isAutoIncrement = rs.getString("IS_AUTOINCREMENT");
				if ("YES".equals(isAutoIncrement)) {
					ret.add("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " RESTART WITH 1");
				}
			}
			return ret;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			Util.close(rs);
		}
	}
	
	private static void executeSQL(Connection conn, String sql) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			Util.close(ps);
		}
	}
}