/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lidroid.xutils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.lidroid.xutils.db.sqlite.*;
import com.lidroid.xutils.db.table.DbModel;
import com.lidroid.xutils.db.table.Id;
import com.lidroid.xutils.db.table.Table;
import com.lidroid.xutils.db.table.TableUtils;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.util.IOUtils;
import com.lidroid.xutils.util.LogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DbUtilsEx implements IDatabase {

	// *************************************** create instance
	// ****************************************************

	/**
	 * key: dbName
	 */
	private static HashMap<String, DbUtilsEx> daoMap = new HashMap<String, DbUtilsEx>();

	private SQLiteDatabase database;
	private DaoConfig daoConfig;
	private boolean debug = false;
	private boolean allowTransaction = false;

	private DbUtilsEx(DaoConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("daoConfig may not be null");
		}
		this.database = createDatabase(config);
		this.daoConfig = config;
	}

	private synchronized static DbUtilsEx getInstance(DaoConfig daoConfig) {
		DbUtilsEx dao = daoMap.get(daoConfig.getDbName());
		if (dao == null) {
			dao = new DbUtilsEx(daoConfig);
			daoMap.put(daoConfig.getDbName(), dao);
		} else {
			dao.daoConfig = daoConfig;
		}

		// update the database if needed
		SQLiteDatabase database = dao.database;
		int oldVersion = database.getVersion();
		int newVersion = daoConfig.getDbVersion();
		if (oldVersion != newVersion) {
			if (oldVersion != 0) {
				DbUpgradeListener upgradeListener = daoConfig
						.getDbUpgradeListener();
				if (upgradeListener != null) {
					upgradeListener.onUpgrade(dao, oldVersion, newVersion);
				} else {
					dao.dropDb();
				}
			}
			database.setVersion(newVersion);
		}

		return dao;
	}

	public static DbUtilsEx create(Context context) {
		DaoConfig config = new DaoConfig(context);
		return getInstance(config);
	}

	public static DbUtilsEx create(Context context, String dbName) {
		DaoConfig config = new DaoConfig(context);
		config.setDbName(dbName);
		return getInstance(config);
	}

	public static DbUtilsEx create(Context context, String dbDir, String dbName) {
		DaoConfig config = new DaoConfig(context);
		config.setDbDir(dbDir);
		config.setDbName(dbName);
		return getInstance(config);
	}

	public static DbUtilsEx create(Context context, String dbName,
			int dbVersion, DbUpgradeListener dbUpgradeListener) {
		DaoConfig config = new DaoConfig(context);
		config.setDbName(dbName);
		config.setDbVersion(dbVersion);
		config.setDbUpgradeListener(dbUpgradeListener);
		return getInstance(config);
	}

	public static DbUtilsEx create(Context context, String dbDir,
			String dbName, int dbVersion, DbUpgradeListener dbUpgradeListener) {
		DaoConfig config = new DaoConfig(context);
		config.setDbDir(dbDir);
		config.setDbName(dbName);
		config.setDbVersion(dbVersion);
		config.setDbUpgradeListener(dbUpgradeListener);
		return getInstance(config);
	}

	public static DbUtilsEx create(DaoConfig daoConfig) {
		return getInstance(daoConfig);
	}

	public DbUtilsEx configDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	public DbUtilsEx configAllowTransaction(boolean allowTransaction) {
		this.allowTransaction = allowTransaction;
		return this;
	}

	public SQLiteDatabase getDatabase() {
		return database;
	}

	public DaoConfig getDaoConfig() {
		return daoConfig;
	}

	// *********************************************** operations
	// ********************************************************

	public void saveOrUpdate(Object entity) {
		try {
			beginTransaction();
			createTableIfNotExist(entity.getClass());
			saveOrUpdateWithoutTransaction(entity);

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void saveOrUpdateAll(List<?> entities) {
		if (entities == null || entities.size() == 0)
			return;
		try {
			beginTransaction();

			createTableIfNotExist(entities.get(0).getClass());
			for (Object entity : entities) {
				saveOrUpdateWithoutTransaction(entity);
			}

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void replace(Object entity) {
		try {
			beginTransaction();

			createTableIfNotExist(entity.getClass());
			execNonQuery(SqlInfoBuilder.buildReplaceSqlInfo(this, entity));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void replaceAll(List<?> entities) {
		if (entities == null || entities.size() == 0)
			return;
		try {
			beginTransaction();

			createTableIfNotExist(entities.get(0).getClass());
			for (Object entity : entities) {
				execNonQuery(SqlInfoBuilder.buildReplaceSqlInfo(this, entity));
			}

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void save(Object entity) {
		try {
			beginTransaction();

			createTableIfNotExist(entity.getClass());
			execNonQuery(SqlInfoBuilder.buildInsertSqlInfo(this, entity));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void saveAll(List<?> entities) {
		if (entities == null || entities.size() == 0)
			return;
		try {
			beginTransaction();

			createTableIfNotExist(entities.get(0).getClass());
			for (Object entity : entities) {
				execNonQuery(SqlInfoBuilder.buildInsertSqlInfo(this, entity));
			}

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public boolean saveBindingId(Object entity) {
		boolean result = false;
		try {
			beginTransaction();

			createTableIfNotExist(entity.getClass());
			result = saveBindingIdWithoutTransaction(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
		return result;
	}

	public void saveBindingIdAll(List<?> entities) {
		if (entities == null || entities.size() == 0)
			return;
		try {
			beginTransaction();

			createTableIfNotExist(entities.get(0).getClass());
			for (Object entity : entities) {
				if (!saveBindingIdWithoutTransaction(entity)) {
					// throw new
					// DbException("saveBindingId error, transaction will not commit!");
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	}

	public void deleteById(Class<?> entityType, Object idValue) {
		if (!tableIsExist(entityType))
			return;
		try {
			beginTransaction();

			execNonQuery(SqlInfoBuilder.buildDeleteSqlInfo(this, entityType,
					idValue));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void delete(Object entity) {
		if (!tableIsExist(entity.getClass()))
			return;
		try {
			beginTransaction();

			execNonQuery(SqlInfoBuilder.buildDeleteSqlInfo(this, entity));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void delete(Class<?> entityType, WhereBuilder whereBuilder) {
		if (!tableIsExist(entityType))
			return;
		try {
			beginTransaction();

			execNonQuery(SqlInfoBuilder.buildDeleteSqlInfo(this, entityType,
					whereBuilder));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void deleteAll(List<?> entities) {
		if (entities == null || entities.size() == 0
				|| !tableIsExist(entities.get(0).getClass()))
			return;
		try {
			beginTransaction();

			for (Object entity : entities) {
				execNonQuery(SqlInfoBuilder.buildDeleteSqlInfo(this, entity));
			}

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void deleteAll(Class<?> entityType) {
		delete(entityType, null);
	}

	public void update(Object entity, String... updateColumnNames) {
		if (!tableIsExist(entity.getClass()))
			return;
		try {
			beginTransaction();

			execNonQuery(SqlInfoBuilder.buildUpdateSqlInfo(this, entity,
					updateColumnNames));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void update(Object entity, WhereBuilder whereBuilder,
			String... updateColumnNames) {
		if (!tableIsExist(entity.getClass()))
			return;
		try {
			beginTransaction();

			execNonQuery(SqlInfoBuilder.buildUpdateSqlInfo(this, entity,
					whereBuilder, updateColumnNames));

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void updateAll(List<?> entities, String... updateColumnNames) {
		if (entities == null || entities.size() == 0
				|| !tableIsExist(entities.get(0).getClass()))
			return;
		try {
			beginTransaction();

			for (Object entity : entities) {
				execNonQuery(SqlInfoBuilder.buildUpdateSqlInfo(this, entity,
						updateColumnNames));
			}

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	public void updateAll(List<?> entities, WhereBuilder whereBuilder,
			String... updateColumnNames) {
		if (entities == null || entities.size() == 0
				|| !tableIsExist(entities.get(0).getClass()))
			return;
		try {
			beginTransaction();

			for (Object entity : entities) {
				execNonQuery(SqlInfoBuilder.buildUpdateSqlInfo(this, entity,
						whereBuilder, updateColumnNames));
			}

			setTransactionSuccessful();
		} catch (DbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			endTransaction();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T findById(Class<T> entityType, Object idValue) {
		if (!tableIsExist(entityType))
			return null;

		Table table = Table.get(this, entityType);
		Selector selector = Selector.from(entityType).where(
				table.id.getColumnName(), "=", idValue);

		String sql = selector.limit(1).toString();
		long seq = CursorUtils.FindCacheSequence.getSeq();
		findTempCache.setSeq(seq);
		Object obj = findTempCache.get(sql);
		if (obj != null) {
			return (T) obj;
		}

		Cursor cursor = execQuery(sql);
		if (cursor != null) {
			try {
				if (cursor.moveToNext()) {
					T entity = (T) CursorUtils.getEntity(this, cursor,
							entityType, seq);
					findTempCache.put(sql, entity);
					return entity;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> T findFirst(Selector selector) {
		if (!tableIsExist(selector.getEntityType()))
			return null;

		String sql = selector.limit(1).toString();
		long seq = CursorUtils.FindCacheSequence.getSeq();
		findTempCache.setSeq(seq);
		Object obj = findTempCache.get(sql);
		if (obj != null) {
			return (T) obj;
		}

		Cursor cursor = execQuery(sql);
		if (cursor != null) {
			try {
				if (cursor.moveToNext()) {
					T entity = (T) CursorUtils.getEntity(this, cursor,
							selector.getEntityType(), seq);
					findTempCache.put(sql, entity);
					return entity;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return null;
	}

	public <T> T findFirst(Class<T> entityType) {
		return findFirst(Selector.from(entityType));
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findAll(Selector selector) {
		if (!tableIsExist(selector.getEntityType()))
			return null;

		String sql = selector.toString();
		long seq = CursorUtils.FindCacheSequence.getSeq();
		findTempCache.setSeq(seq);
		Object obj = findTempCache.get(sql);
		if (obj != null) {
			return (List<T>) obj;
		}

		List<T> result = new ArrayList<T>();

		Cursor cursor = execQuery(sql);
		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					T entity = (T) CursorUtils.getEntity(this, cursor,
							selector.getEntityType(), seq);
					result.add(entity);
				}
				findTempCache.put(sql, result);
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return result;
	}

	public <T> List<T> findAll(Class<T> entityType) {
		return findAll(Selector.from(entityType));
	}

	public DbModel findDbModelFirst(SqlInfo sqlInfo) {
		Cursor cursor = execQuery(sqlInfo);
		if (cursor != null) {
			try {
				if (cursor.moveToNext()) {
					return CursorUtils.getDbModel(cursor);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return null;
	}

	public DbModel findDbModelFirst(DbModelSelector selector) {
		if (!tableIsExist(selector.getEntityType()))
			return null;

		Cursor cursor = execQuery(selector.limit(1).toString());
		if (cursor != null) {
			try {
				if (cursor.moveToNext()) {
					return CursorUtils.getDbModel(cursor);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return null;
	}

	public List<DbModel> findDbModelAll(SqlInfo sqlInfo) {
		List<DbModel> dbModelList = new ArrayList<DbModel>();

		Cursor cursor = execQuery(sqlInfo);
		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					dbModelList.add(CursorUtils.getDbModel(cursor));
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return dbModelList;
	}

	public List<DbModel> findDbModelAll(DbModelSelector selector) {
		if (!tableIsExist(selector.getEntityType()))
			return null;

		List<DbModel> dbModelList = new ArrayList<DbModel>();

		Cursor cursor = execQuery(selector.toString());
		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					dbModelList.add(CursorUtils.getDbModel(cursor));
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return dbModelList;
	}

	public long count(Selector selector) {
		Class<?> entityType = selector.getEntityType();
		if (!tableIsExist(entityType))
			return 0;

		Table table = Table.get(this, entityType);
		DbModelSelector dmSelector = selector.select("count("
				+ table.id.getColumnName() + ") as count");
		return findDbModelFirst(dmSelector).getLong("count");
	}

	public long count(Class<?> entityType) {
		return count(Selector.from(entityType));
	}

	private SQLiteDatabase createDatabase(DaoConfig config) {
		SQLiteDatabase result = null;

		String dbDir = config.getDbDir();
		if (!TextUtils.isEmpty(dbDir)) {
			File dir = new File(dbDir);
			if (dir.exists() || dir.mkdirs()) {
				File dbFile = new File(dbDir, config.getDbName());
				result = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			}
		} else {
			result = config.getContext().openOrCreateDatabase(
					config.getDbName(), 0, null);
		}
		return result;
	}

	// ***************************** private operations with out transaction
	// *****************************
	private void saveOrUpdateWithoutTransaction(Object entity)
			throws DbException {
		Table table = Table.get(this, entity.getClass());
		Id id = table.id;
		if (id.isAutoIncrement()) {
			if (id.getColumnValue(entity) != null) {
				execNonQuery(SqlInfoBuilder.buildUpdateSqlInfo(this, entity));
			} else {
				saveBindingIdWithoutTransaction(entity);
			}
		} else {
			execNonQuery(SqlInfoBuilder.buildReplaceSqlInfo(this, entity));
		}
	}

	private boolean saveBindingIdWithoutTransaction(Object entity) {
		Class<?> entityType = entity.getClass();
		Table table = Table.get(this, entityType);
		Id idColumn = table.id;
		try {
			if (idColumn.isAutoIncrement()) {
				execNonQuery(SqlInfoBuilder.buildInsertSqlInfo(this, entity));
				long id = getLastAutoIncrementId(table.tableName);
				if (id == -1) {
					return false;
				}
				idColumn.setAutoIncrementId(entity, id);
				return true;
			} else {
				execNonQuery(SqlInfoBuilder.buildInsertSqlInfo(this, entity));
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// ************************************************ tools
	// ***********************************

	private long getLastAutoIncrementId(String tableName) {
		long id = -1;
		Cursor cursor = execQuery("SELECT seq FROM sqlite_sequence WHERE name='"
				+ tableName + "'");
		if (cursor != null) {
			try {
				if (cursor.moveToNext()) {
					id = cursor.getLong(0);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
		return id;
	}

	public void createTableIfNotExist(Class<?> entityType) {
		try {
			if (!tableIsExist(entityType)) {
				SqlInfo sqlInfo = SqlInfoBuilder.buildCreateTableSqlInfo(this,
						entityType);
				execNonQuery(sqlInfo);
				String execAfterTableCreated = TableUtils
						.getExecAfterTableCreated(entityType);
				if (!TextUtils.isEmpty(execAfterTableCreated)) {
					execNonQuery(execAfterTableCreated);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean tableIsExist(Class<?> entityType) {
		Table table = Table.get(this, entityType);
		if (table.isCheckedDatabase()) {
			return true;
		}

		Cursor cursor = execQuery("SELECT COUNT(*) AS c FROM sqlite_master WHERE type='table' AND name='"
				+ table.tableName + "'");
		if (cursor != null) {
			try {
				if (cursor.moveToNext()) {
					int count = cursor.getInt(0);
					if (count > 0) {
						table.setCheckedDatabase(true);
						return true;
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}

		return false;
	}

	public void dropDb() {
		Cursor cursor = execQuery("SELECT name FROM sqlite_master WHERE type='table' AND name<>'sqlite_sequence'");
		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					try {
						String tableName = cursor.getString(0);
						execNonQuery("DROP TABLE " + tableName);
						Table.remove(this, tableName);
					} catch (Throwable e) {
						LogUtils.e(e.getMessage(), e);
					}
				}

			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(cursor);
			}
		}
	}

	public void dropTable(Class<?> entityType) {
		if (!tableIsExist(entityType))
			return;
		String tableName = TableUtils.getTableName(entityType);
		execNonQuery("DROP TABLE " + tableName);
		Table.remove(this, entityType);
	}

	public void close() {
		String dbName = this.daoConfig.getDbName();
		if (daoMap.containsKey(dbName)) {
			daoMap.remove(dbName);
			this.database.close();
		}
	}

	// /////////////////////////////////// exec sql
	// /////////////////////////////////////////////////////
	private void debugSql(String sql) {
		if (debug) {
			LogUtils.d(sql);
		}
	}

	private Lock writeLock = new ReentrantLock();
	private volatile boolean writeLocked = false;

	private void beginTransaction() {
		if (allowTransaction) {
			database.beginTransaction();
		} else {
			writeLock.lock();
			writeLocked = true;
		}
	}

	private void setTransactionSuccessful() {
		if (allowTransaction) {
			database.setTransactionSuccessful();
		}
	}

	private void endTransaction() {
		if (allowTransaction) {
			database.endTransaction();
		}
		if (writeLocked) {
			writeLock.unlock();
			writeLocked = false;
		}
	}

	public void execNonQuery(SqlInfo sqlInfo) {
		debugSql(sqlInfo.getSql());
		try {
			if (sqlInfo.getBindArgs() != null) {
				database.execSQL(sqlInfo.getSql(), sqlInfo.getBindArgsAsArray());
			} else {
				database.execSQL(sqlInfo.getSql());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void execNonQuery(String sql) {
		debugSql(sql);
		try {
			database.execSQL(sql);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Cursor execQuery(SqlInfo sqlInfo) {
		debugSql(sqlInfo.getSql());
		try {
			return database.rawQuery(sqlInfo.getSql(),
					sqlInfo.getBindArgsAsStrArray());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	public Cursor execQuery(String sql) {
		debugSql(sql);
		try {
			return database.rawQuery(sql, null);
		} catch (Throwable e) {

			e.printStackTrace();
		}
		return null;
	}

	// ///////////////////// temp cache
	// ////////////////////////////////////////////////////////////////
	private final FindTempCache findTempCache = new FindTempCache();

	private class FindTempCache {
		private FindTempCache() {
		}

		/**
		 * key: sql; value: find result
		 */
		private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();

		private long seq = 0;

		public void put(String sql, Object result) {
			if (sql != null && result != null) {
				cache.put(sql, result);
			}
		}

		public Object get(String sql) {
			return cache.get(sql);
		}

		public void setSeq(long seq) {
			if (this.seq != seq) {
				cache.clear();
				this.seq = seq;
			}
		}
	}

}
