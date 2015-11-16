/**
 * 
 */
package com.lidroid.xutils.db.sqlite;

import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lidroid.xutils.db.table.DbModel;
import com.lidroid.xutils.exception.DbException;

/**
 * @author andrewlu
 * 
 */
public interface IDatabase {
	DaoConfig getDaoConfig();

	SQLiteDatabase getDatabase();

	public void close() throws DbException;

	public long count(Class<?> entityType) throws DbException;

	public long count(Selector selector) throws DbException;

	public void createTableIfNotExist(Class<?> entityType) throws DbException;

	public void delete(Class<?> entityType, WhereBuilder whereBuilder)
			throws DbException;

	public void delete(Object entity) throws DbException;

	public void deleteAll(Class<?> entityType) throws DbException;

	public void deleteAll(List<?> entities) throws DbException;

	public void deleteById(Class<?> entityType, Object idValue)
			throws DbException;

	//实现类只能抛出比接口更少的异常或者同等异常.
	public void dropDb() throws DbException;

	public void dropTable(Class<?> entityType) throws DbException;

	public void execNonQuery(SqlInfo sqlInfo) throws DbException;

	public void execNonQuery(String sql) throws DbException;

	public Cursor execQuery(SqlInfo sqlInfo) throws DbException;

	public Cursor execQuery(String sql) throws DbException;

	public <T> List<T> findAll(Class<T> entityType) throws DbException;

	public <T> List<T> findAll(Selector selector) throws DbException;

	public <T> T findById(Class<T> entityType, Object idValue)
			throws DbException;

	public List<DbModel> findDbModelAll(DbModelSelector selector)
			throws DbException;

	public List<DbModel> findDbModelAll(SqlInfo sqlInfo) throws DbException;

	public DbModel findDbModelFirst(DbModelSelector selector)
			throws DbException;

	public DbModel findDbModelFirst(SqlInfo sqlInfo) throws DbException;

	public <T> T findFirst(Class<T> entityType) throws DbException;

	public <T> T findFirst(Selector selector) throws DbException;

	public void replace(Object entity) throws DbException;

	public void replaceAll(List<?> entities) throws DbException;

	public void save(Object entity) throws DbException;

	public void saveAll(List<?> entities) throws DbException;

	public boolean saveBindingId(Object entity) throws DbException;

	public void saveBindingIdAll(List<?> entities) throws DbException;

	public void saveOrUpdate(Object entity) throws DbException;

	public void saveOrUpdateAll(List<?> entities) throws DbException;

	public boolean tableIsExist(Class<?> entityType) throws DbException;

	public void update(Object entity, String... updateColumnNames)
			throws DbException;

	public void update(Object entity, WhereBuilder whereBuilder,
			String... updateColumnNames) throws DbException;

	public void updateAll(List<?> entities, String... updateColumnNames)
			throws DbException;

	public void updateAll(List<?> entities, WhereBuilder whereBuilder,
			String... updateColumnNames) throws DbException;
}
