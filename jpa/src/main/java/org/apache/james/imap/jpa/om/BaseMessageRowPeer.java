package org.apache.james.imap.jpa.om;

import org.apache.james.imap.jpa.om.map.MessageRowMapBuilder;
import org.apache.torque.NoRowsException;
import org.apache.torque.TooManyRowsException;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import org.apache.torque.map.MapBuilder;
import org.apache.torque.map.TableMap;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.SimpleKey;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Criteria;

import com.workingdogs.village.DataSetException;
import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * 
 * This class was autogenerated by Torque on:
 * 
 * [Sun Dec 09 17:45:09 GMT 2007]
 * 
 */
public abstract class BaseMessageRowPeer extends BasePeer {
    /** Serial version */
    private static final long serialVersionUID = 1197222309712L;

    /** the default database name for this class */
    public static final String DATABASE_NAME;

    /** the table name for this class */
    public static final String TABLE_NAME;

    /**
     * @return the map builder for this peer
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     * @deprecated Torque.getMapBuilder(MessageRowMapBuilder.CLASS_NAME) instead
     */
    public static MapBuilder getMapBuilder() throws TorqueException {
        return Torque.getMapBuilder(MessageRowMapBuilder.CLASS_NAME);
    }

    /** the column name for the mailbox_id field */
    public static final String MAILBOX_ID;

    /** the column name for the uid field */
    public static final String UID;

    /** the column name for the internal_date field */
    public static final String INTERNAL_DATE;

    /** the column name for the size field */
    public static final String SIZE;

    static {
        DATABASE_NAME = "mailboxmanager";
        TABLE_NAME = "message";

        MAILBOX_ID = "message.mailbox_id";
        UID = "message.uid";
        INTERNAL_DATE = "message.internal_date";
        SIZE = "message.size";
        if (Torque.isInit()) {
            try {
                Torque.getMapBuilder(MessageRowMapBuilder.CLASS_NAME);
            } catch (TorqueException e) {
                log.error("Could not initialize Peer", e);
                throw new RuntimeException(e);
            }
        } else {
            Torque.registerMapBuilder(MessageRowMapBuilder.CLASS_NAME);
        }
    }

    /** number of columns for this peer */
    public static final int numColumns = 4;

    /** A class that can be returned by this peer. */
    protected static final String CLASSNAME_DEFAULT = "org.apache.james.mailboxmanager.torque.om.MessageRow";

    /** A class that can be returned by this peer. */
    protected static final Class CLASS_DEFAULT = initClass(CLASSNAME_DEFAULT);

    /**
     * Class object initialization method.
     * 
     * @param className
     *            name of the class to initialize
     * @return the initialized class
     */
    private static Class initClass(String className) {
        Class c = null;
        try {
            c = Class.forName(className);
        } catch (Throwable t) {
            log
                    .error(
                            "A FATAL ERROR has occurred which should not "
                                    + "have happened under any circumstance.  Please notify "
                                    + "the Torque developers <torque-dev@db.apache.org> "
                                    + "and give as many details as possible (including the error "
                                    + "stack trace).", t);

            // Error objects should always be propogated.
            if (t instanceof Error) {
                throw (Error) t.fillInStackTrace();
            }
        }
        return c;
    }

    /**
     * Get the list of objects for a ResultSet. Please not that your resultset
     * MUST return columns in the right order. You can use getFieldNames() in
     * BaseObject to get the correct sequence.
     * 
     * @param results
     *            the ResultSet
     * @return the list of objects
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List resultSet2Objects(java.sql.ResultSet results)
            throws TorqueException {
        try {
            QueryDataSet qds = null;
            List rows = null;
            try {
                qds = new QueryDataSet(results);
                rows = getSelectResults(qds);
            } finally {
                if (qds != null) {
                    qds.close();
                }
            }

            return populateObjects(rows);
        } catch (SQLException e) {
            throw new TorqueException(e);
        } catch (DataSetException e) {
            throw new TorqueException(e);
        }
    }

    /**
     * Method to do inserts.
     * 
     * @param criteria
     *            object used to create the INSERT statement.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static ObjectKey doInsert(Criteria criteria) throws TorqueException {
        return BaseMessageRowPeer.doInsert(criteria, (Connection) null);
    }

    /**
     * Method to do inserts. This method is to be used during a transaction,
     * otherwise use the doInsert(Criteria) method. It will take care of the
     * connection details internally.
     * 
     * @param criteria
     *            object used to create the INSERT statement.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static ObjectKey doInsert(Criteria criteria, Connection con)
            throws TorqueException {
        correctBooleans(criteria);

        setDbName(criteria);

        if (con == null) {
            return BasePeer.doInsert(criteria);
        } else {
            return BasePeer.doInsert(criteria, con);
        }
    }

    /**
     * Add all the columns needed to create a new object.
     * 
     * @param criteria
     *            object containing the columns to add.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void addSelectColumns(Criteria criteria)
            throws TorqueException {
        criteria.addSelectColumn(MAILBOX_ID);
        criteria.addSelectColumn(UID);
        criteria.addSelectColumn(INTERNAL_DATE);
        criteria.addSelectColumn(SIZE);
    }

    /**
     * changes the boolean values in the criteria to the appropriate type,
     * whenever a booleanchar or booleanint column is involved. This enables the
     * user to create criteria using Boolean values for booleanchar or
     * booleanint columns
     * 
     * @param criteria
     *            the criteria in which the boolean values should be corrected
     * @throws TorqueException
     *             if the database map for the criteria cannot be obtained.
     */
    public static void correctBooleans(Criteria criteria)
            throws TorqueException {
        correctBooleans(criteria, getTableMap());
    }

    /**
     * Create a new object of type cls from a resultset row starting from a
     * specified offset. This is done so that you can select other rows than
     * just those needed for this object. You may for example want to create two
     * objects from the same row.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static MessageRow row2Object(Record row, int offset, Class cls)
            throws TorqueException {
        try {
            MessageRow obj = (MessageRow) cls.newInstance();
            MessageRowPeer.populateObject(row, offset, obj);
            obj.setModified(false);
            obj.setNew(false);

            return obj;
        } catch (InstantiationException e) {
            throw new TorqueException(e);
        } catch (IllegalAccessException e) {
            throw new TorqueException(e);
        }
    }

    /**
     * Populates an object from a resultset row starting from a specified
     * offset. This is done so that you can select other rows than just those
     * needed for this object. You may for example want to create two objects
     * from the same row.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void populateObject(Record row, int offset, MessageRow obj)
            throws TorqueException {
        try {
            obj.setMailboxId(row.getValue(offset + 0).asLong());
            obj.setUid(row.getValue(offset + 1).asLong());
            obj.setInternalDate(row.getValue(offset + 2).asUtilDate());
            obj.setSize(row.getValue(offset + 3).asInt());
        } catch (DataSetException e) {
            throw new TorqueException(e);
        }
    }

    /**
     * Method to do selects.
     * 
     * @param criteria
     *            object used to create the SELECT statement.
     * @return List of selected Objects
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List doSelect(Criteria criteria) throws TorqueException {
        return populateObjects(doSelectVillageRecords(criteria));
    }

    /**
     * Method to do selects within a transaction.
     * 
     * @param criteria
     *            object used to create the SELECT statement.
     * @param con
     *            the connection to use
     * @return List of selected Objects
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List doSelect(Criteria criteria, Connection con)
            throws TorqueException {
        return populateObjects(doSelectVillageRecords(criteria, con));
    }

    /**
     * Grabs the raw Village records to be formed into objects. This method
     * handles connections internally. The Record objects returned by this
     * method should be considered readonly. Do not alter the data and call
     * save(), your results may vary, but are certainly likely to result in hard
     * to track MT bugs.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List doSelectVillageRecords(Criteria criteria)
            throws TorqueException {
        return BaseMessageRowPeer.doSelectVillageRecords(criteria,
                (Connection) null);
    }

    /**
     * Grabs the raw Village records to be formed into objects. This method
     * should be used for transactions
     * 
     * @param criteria
     *            object used to create the SELECT statement.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List doSelectVillageRecords(Criteria criteria, Connection con)
            throws TorqueException {
        if (criteria.getSelectColumns().size() == 0) {
            addSelectColumns(criteria);
        }
        correctBooleans(criteria);

        setDbName(criteria);

        // BasePeer returns a List of Value (Village) arrays. The array
        // order follows the order columns were placed in the Select clause.
        if (con == null) {
            return BasePeer.doSelect(criteria);
        } else {
            return BasePeer.doSelect(criteria, con);
        }
    }

    /**
     * The returned List will contain objects of the default type or objects
     * that inherit from the default.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List populateObjects(List records) throws TorqueException {
        List results = new ArrayList(records.size());

        // populate the object(s)
        for (int i = 0; i < records.size(); i++) {
            Record row = (Record) records.get(i);
            results.add(MessageRowPeer.row2Object(row, 1, MessageRowPeer
                    .getOMClass()));
        }
        return results;
    }

    /**
     * The class that the Peer will make instances of. If the BO is abstract
     * then you must implement this method in the BO.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static Class getOMClass() throws TorqueException {
        return CLASS_DEFAULT;
    }

    /**
     * Method to do updates.
     * 
     * @param criteria
     *            object containing data that is used to create the UPDATE
     *            statement.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doUpdate(Criteria criteria) throws TorqueException {
        BaseMessageRowPeer.doUpdate(criteria, (Connection) null);
    }

    /**
     * Method to do updates. This method is to be used during a transaction,
     * otherwise use the doUpdate(Criteria) method. It will take care of the
     * connection details internally.
     * 
     * @param criteria
     *            object containing data that is used to create the UPDATE
     *            statement.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doUpdate(Criteria criteria, Connection con)
            throws TorqueException {
        Criteria selectCriteria = new Criteria(DATABASE_NAME, 2);
        correctBooleans(criteria);

        selectCriteria.put(MAILBOX_ID, criteria.remove(MAILBOX_ID));

        selectCriteria.put(UID, criteria.remove(UID));

        setDbName(criteria);

        if (con == null) {
            BasePeer.doUpdate(selectCriteria, criteria);
        } else {
            BasePeer.doUpdate(selectCriteria, criteria, con);
        }
    }

    /**
     * Method to do deletes.
     * 
     * @param criteria
     *            object containing data that is used DELETE from database.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doDelete(Criteria criteria) throws TorqueException {
        MessageRowPeer.doDelete(criteria, (Connection) null);
    }

    /**
     * Method to do deletes. This method is to be used during a transaction,
     * otherwise use the doDelete(Criteria) method. It will take care of the
     * connection details internally.
     * 
     * @param criteria
     *            object containing data that is used DELETE from database.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doDelete(Criteria criteria, Connection con)
            throws TorqueException {
        correctBooleans(criteria);

        setDbName(criteria);

        if (con == null) {
            BasePeer.doDelete(criteria);
        } else {
            BasePeer.doDelete(criteria, con);
        }
    }

    /**
     * Method to do selects
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List doSelect(MessageRow obj) throws TorqueException {
        return doSelect(buildSelectCriteria(obj));
    }

    /**
     * Method to do inserts
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doInsert(MessageRow obj) throws TorqueException {
        doInsert(buildCriteria(obj));
        obj.setNew(false);
        obj.setModified(false);
    }

    /**
     * @param obj
     *            the data object to update in the database.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doUpdate(MessageRow obj) throws TorqueException {
        doUpdate(buildCriteria(obj));
        obj.setModified(false);
    }

    /**
     * @param obj
     *            the data object to delete in the database.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doDelete(MessageRow obj) throws TorqueException {
        doDelete(buildSelectCriteria(obj));
    }

    /**
     * Method to do inserts. This method is to be used during a transaction,
     * otherwise use the doInsert(MessageRow) method. It will take care of the
     * connection details internally.
     * 
     * @param obj
     *            the data object to insert into the database.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doInsert(MessageRow obj, Connection con)
            throws TorqueException {
        doInsert(buildCriteria(obj), con);
        obj.setNew(false);
        obj.setModified(false);
    }

    /**
     * Method to do update. This method is to be used during a transaction,
     * otherwise use the doUpdate(MessageRow) method. It will take care of the
     * connection details internally.
     * 
     * @param obj
     *            the data object to update in the database.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doUpdate(MessageRow obj, Connection con)
            throws TorqueException {
        doUpdate(buildCriteria(obj), con);
        obj.setModified(false);
    }

    /**
     * Method to delete. This method is to be used during a transaction,
     * otherwise use the doDelete(MessageRow) method. It will take care of the
     * connection details internally.
     * 
     * @param obj
     *            the data object to delete in the database.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doDelete(MessageRow obj, Connection con)
            throws TorqueException {
        doDelete(buildSelectCriteria(obj), con);
    }

    /**
     * Method to do deletes.
     * 
     * @param pk
     *            ObjectKey that is used DELETE from database.
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doDelete(ObjectKey pk) throws TorqueException {
        BaseMessageRowPeer.doDelete(pk, (Connection) null);
    }

    /**
     * Method to delete. This method is to be used during a transaction,
     * otherwise use the doDelete(ObjectKey) method. It will take care of the
     * connection details internally.
     * 
     * @param pk
     *            the primary key for the object to delete in the database.
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static void doDelete(ObjectKey pk, Connection con)
            throws TorqueException {
        doDelete(buildCriteria(pk), con);
    }

    /** Build a Criteria object from an ObjectKey */
    public static Criteria buildCriteria(ObjectKey pk) {
        Criteria criteria = new Criteria();
        SimpleKey[] keys = (SimpleKey[]) pk.getValue();
        criteria.add(MAILBOX_ID, keys[0]);
        criteria.add(UID, keys[1]);
        return criteria;
    }

    /** Build a Criteria object from the data object for this peer */
    public static Criteria buildCriteria(MessageRow obj) {
        Criteria criteria = new Criteria(DATABASE_NAME);
        criteria.add(MAILBOX_ID, obj.getMailboxId());
        criteria.add(UID, obj.getUid());
        criteria.add(INTERNAL_DATE, obj.getInternalDate());
        criteria.add(SIZE, obj.getSize());
        return criteria;
    }

    /**
     * Build a Criteria object from the data object for this peer, skipping all
     * binary columns
     */
    public static Criteria buildSelectCriteria(MessageRow obj) {
        Criteria criteria = new Criteria(DATABASE_NAME);
        criteria.add(MAILBOX_ID, obj.getMailboxId());
        criteria.add(UID, obj.getUid());
        criteria.add(INTERNAL_DATE, obj.getInternalDate());
        criteria.add(SIZE, obj.getSize());
        return criteria;
    }

    /**
     * Retrieve a single object by pk
     * 
     * @param pk
     *            the primary key
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     * @throws NoRowsException
     *             Primary key was not found in database.
     * @throws TooManyRowsException
     *             Primary key was not found in database.
     */
    public static MessageRow retrieveByPK(ObjectKey pk) throws TorqueException,
            NoRowsException, TooManyRowsException {
        Connection db = null;
        MessageRow retVal = null;
        try {
            db = Torque.getConnection(DATABASE_NAME);
            retVal = retrieveByPK(pk, db);
        } finally {
            Torque.closeConnection(db);
        }
        return retVal;
    }

    /**
     * Retrieve a single object by pk
     * 
     * @param pk
     *            the primary key
     * @param con
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     * @throws NoRowsException
     *             Primary key was not found in database.
     * @throws TooManyRowsException
     *             Primary key was not found in database.
     */
    public static MessageRow retrieveByPK(ObjectKey pk, Connection con)
            throws TorqueException, NoRowsException, TooManyRowsException {
        Criteria criteria = buildCriteria(pk);
        List v = doSelect(criteria, con);
        if (v.size() == 0) {
            throw new NoRowsException("Failed to select a row.");
        } else if (v.size() > 1) {
            throw new TooManyRowsException("Failed to select only one row.");
        } else {
            return (MessageRow) v.get(0);
        }
    }

    /**
     * Retrieve a multiple objects by pk
     * 
     * @param pks
     *            List of primary keys
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List retrieveByPKs(List pks) throws TorqueException {
        Connection db = null;
        List retVal = null;
        try {
            db = Torque.getConnection(DATABASE_NAME);
            retVal = retrieveByPKs(pks, db);
        } finally {
            Torque.closeConnection(db);
        }
        return retVal;
    }

    /**
     * Retrieve a multiple objects by pk
     * 
     * @param pks
     *            List of primary keys
     * @param dbcon
     *            the connection to use
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static List retrieveByPKs(List pks, Connection dbcon)
            throws TorqueException {
        List objs = null;
        if (pks == null || pks.size() == 0) {
            objs = new LinkedList();
        } else {
            Criteria criteria = new Criteria();
            Iterator iter = pks.iterator();
            while (iter.hasNext()) {
                ObjectKey pk = (ObjectKey) iter.next();
                SimpleKey[] keys = (SimpleKey[]) pk.getValue();
                Criteria.Criterion c0 = criteria.getNewCriterion(MAILBOX_ID,
                        keys[0], Criteria.EQUAL);
                Criteria.Criterion c1 = criteria.getNewCriterion(UID, keys[1],
                        Criteria.EQUAL);
                c0.and(c1);
                criteria.or(c0);
            }
            objs = doSelect(criteria, dbcon);
        }
        return objs;
    }

    /**
     * retrieve object using using pk values.
     * 
     * @param mailboxId
     *            long
     * @param uid
     *            long
     */
    public static MessageRow retrieveByPK(long mailboxId, long uid)
            throws TorqueException {
        Connection db = null;
        MessageRow retVal = null;
        try {
            db = Torque.getConnection(DATABASE_NAME);
            retVal = retrieveByPK(mailboxId, uid, db);
        } finally {
            Torque.closeConnection(db);
        }
        return retVal;
    }

    /**
     * retrieve object using using pk values.
     * 
     * @param mailboxId
     *            long
     * @param uid
     *            long
     * @param con
     *            Connection
     */
    public static MessageRow retrieveByPK(long mailboxId, long uid,
            Connection con) throws TorqueException {

        Criteria criteria = new Criteria(5);
        criteria.add(MAILBOX_ID, mailboxId);
        criteria.add(UID, uid);
        List v = doSelect(criteria, con);
        if (v.size() == 1) {
            return (MessageRow) v.get(0);
        } else {
            throw new TorqueException("Failed to select one and only one row.");
        }
    }

    /**
     * selects a collection of MessageRow objects pre-filled with their
     * MailboxRow objects.
     * 
     * This method is protected by default in order to keep the public api
     * reasonable. You can provide public methods for those you actually need in
     * MessageRowPeer.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    protected static List doSelectJoinMailboxRow(Criteria criteria)
            throws TorqueException {
        return doSelectJoinMailboxRow(criteria, null);
    }

    /**
     * selects a collection of MessageRow objects pre-filled with their
     * MailboxRow objects.
     * 
     * This method is protected by default in order to keep the public api
     * reasonable. You can provide public methods for those you actually need in
     * MessageRowPeer.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    protected static List doSelectJoinMailboxRow(Criteria criteria,
            Connection conn) throws TorqueException {
        setDbName(criteria);

        MessageRowPeer.addSelectColumns(criteria);
        int offset = numColumns + 1;
        MailboxRowPeer.addSelectColumns(criteria);

        criteria.addJoin(MessageRowPeer.MAILBOX_ID, MailboxRowPeer.MAILBOX_ID);

        correctBooleans(criteria);

        List rows;
        if (conn == null) {
            rows = BasePeer.doSelect(criteria);
        } else {
            rows = BasePeer.doSelect(criteria, conn);
        }

        List results = new ArrayList();

        for (int i = 0; i < rows.size(); i++) {
            Record row = (Record) rows.get(i);

            Class omClass = MessageRowPeer.getOMClass();
            MessageRow obj1 = (MessageRow) MessageRowPeer.row2Object(row, 1,
                    omClass);
            omClass = MailboxRowPeer.getOMClass();
            MailboxRow obj2 = (MailboxRow) MailboxRowPeer.row2Object(row,
                    offset, omClass);

            boolean newObject = true;
            for (int j = 0; j < results.size(); j++) {
                MessageRow temp_obj1 = (MessageRow) results.get(j);
                MailboxRow temp_obj2 = (MailboxRow) temp_obj1.getMailboxRow();
                if (temp_obj2.getPrimaryKey().equals(obj2.getPrimaryKey())) {
                    newObject = false;
                    temp_obj2.addMessageRow(obj1);
                    break;
                }
            }
            if (newObject) {
                obj2.initMessageRows();
                obj2.addMessageRow(obj1);
            }
            results.add(obj1);
        }
        return results;
    }

    /**
     * Returns the TableMap related to this peer.
     * 
     * @throws TorqueException
     *             Any exceptions caught during processing will be rethrown
     *             wrapped into a TorqueException.
     */
    public static TableMap getTableMap() throws TorqueException {
        return Torque.getDatabaseMap(DATABASE_NAME).getTable(TABLE_NAME);
    }

    private static void setDbName(Criteria crit) {
        // Set the correct dbName if it has not been overridden
        // crit.getDbName will return the same object if not set to
        // another value so == check is okay and faster
        if (crit.getDbName() == Torque.getDefaultDB()) {
            crit.setDbName(DATABASE_NAME);
        }
    }

}