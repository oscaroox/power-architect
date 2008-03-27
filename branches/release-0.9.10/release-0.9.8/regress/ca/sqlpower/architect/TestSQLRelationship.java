/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


public class TestSQLRelationship extends SQLTestCase {

	private SQLTable parentTable;
	private SQLTable childTable1;
	private SQLTable childTable2;
	private SQLRelationship rel1;
	private SQLRelationship rel2;
	private SQLDatabase database;
	
	public TestSQLRelationship(String name) throws Exception {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("-------------Starting New Test "+getName()+" -------------");
		
		database = new SQLDatabase();
		parentTable = new SQLTable(database, "parent", null, "TABLE", true);
		parentTable.addColumn(new SQLColumn(parentTable, "pkcol_1", Types.INTEGER, 10, 0));
		parentTable.addColumn(new SQLColumn(parentTable, "pkcol_2", Types.INTEGER, 10, 0));
		parentTable.addColumn(new SQLColumn(parentTable, "attribute_1", Types.INTEGER, 10, 0));
		database.addChild(parentTable);
		childTable1 = new SQLTable(database, "child_1", null, "TABLE", true);
		childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_1", Types.INTEGER, 10, 0));
		childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_2", Types.INTEGER, 10, 0));
		childTable1.addColumn(new SQLColumn(childTable1, "child_attribute", Types.INTEGER, 10, 0));
		database.addChild(childTable1);
		
		childTable2 = new SQLTable(database, "child_2", null, "TABLE", true);
		childTable2.addColumn(new SQLColumn(childTable2, "child2_pkcol_1", Types.INTEGER, 10, 0));
		childTable2.addColumn(new SQLColumn(childTable2, "child2_pkcol_2", Types.INTEGER, 10, 0));
		childTable2.addColumn(new SQLColumn(childTable2, "child2_attribute", Types.INTEGER, 10, 0));
		database.addChild(childTable2);
		
		rel1 = new SQLRelationship();
		rel1.setIdentifying(true);
		rel1.attachRelationship(parentTable,childTable1,false);
		rel1.setName("rel1");
		rel1.addMapping(parentTable.getColumn(0), childTable1.getColumn(0));
		rel1.addMapping(parentTable.getColumn(1), childTable1.getColumn(1));
	
		rel2 = new SQLRelationship();
		rel2.setName("rel2");
		rel2.attachRelationship(parentTable,childTable2,true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Returns one of the relationships that setUp makes.
	 * Right now, it's rel1.
	 */
	@Override
	protected SQLObject getSQLObjectUnderTest() {
		return rel1;
	}
	
	public void testSetPhysicalName() {
		CountingSQLObjectListener l = new CountingSQLObjectListener();
		rel1.addSQLObjectListener(l);
		
		// ensure all event counts start with 0
		assertEquals(0, l.getInsertedCount());
		assertEquals(0, l.getRemovedCount());
		assertEquals(0, l.getChangedCount());
		assertEquals(0, l.getStructureChangedCount());
		
		rel1.setPhysicalName("test_new_name");
		
		// ensure only dbObjectChanged was called (we omit this check for the remainder of the tests)
		assertEquals(0, l.getInsertedCount());
		assertEquals(0, l.getRemovedCount());
		assertEquals(1, l.getChangedCount());
		assertEquals(0, l.getStructureChangedCount());
		
		assertEquals("new name didn't stick", "test_new_name", rel1.getPhysicalName());
		
		rel1.setPhysicalName("test_new_name");
		assertEquals(1, l.getChangedCount());

		rel1.setPhysicalName("test_actual_new_name");
		assertEquals(2, l.getChangedCount());

		rel1.setPhysicalName(null);
		assertEquals(3, l.getChangedCount());
		assertEquals("new name didn't go back to logical name", rel1.getName(), rel1.getPhysicalName());

		rel1.setPhysicalName(null);
		assertEquals(3, l.getChangedCount());

		// double-check that none of the other event types got fired
		assertEquals(0, l.getInsertedCount());
		assertEquals(0, l.getRemovedCount());
		assertEquals(0, l.getStructureChangedCount());
	}

	public void testReadFromDB() throws Exception {
		Connection con = null;
		Statement stmt = null;
		String lastSQL = null;
		try {
			con = db.getConnection();
			stmt = con.createStatement();

			try {
				stmt.executeUpdate("DROP TABLE relationship_test_child");
			} catch (SQLException ex) {
				System.out.println("Ignoring SQL Exception; assume relationship_test_child didn't exist.");
				System.out.println(ex.getMessage());
			}

			try {
				stmt.executeUpdate("DROP TABLE relationship_test_parent");
			} catch (SQLException ex) {
				System.out.println("Ignoring SQL Exception; assume relationship_test_parent didn't exist.");
				System.out.println(ex.getMessage());
			}

			lastSQL = "CREATE TABLE relationship_test_parent (\n" +
					  " pkcol_1 integer not null,\n" +
					  " pkcol_2 integer not null,\n" +
					  " attribute_1 integer not null)";
			stmt.executeUpdate(lastSQL);

			lastSQL = "CREATE TABLE relationship_test_child (\n" +
			          " parent_pkcol_1 integer not null,\n" +
			          " parent_pkcol_2 integer not null,\n" +
			          " child_attribute_1 integer not null)";
			stmt.executeUpdate(lastSQL);
			
			lastSQL = "ALTER TABLE relationship_test_parent\n" +
			          " ADD CONSTRAINT relationship_test_pk\n" +
			          " PRIMARY KEY (pkcol_1 , pkcol_2)";
			stmt.executeUpdate(lastSQL);
			
			lastSQL = "ALTER TABLE relationship_test_child\n" +
			          " ADD CONSTRAINT relationship_test_fk\n" +
			          " FOREIGN KEY (parent_pkcol_1, parent_pkcol_2)\n" +
			          " REFERENCES relationship_test_parent (pkcol_1 , pkcol_2)";
			stmt.executeUpdate(lastSQL);
			
		} catch (SQLException ex) {
			System.out.println("SQL Statement Failed:\n"+lastSQL+"\nStack trace of SQLException follows:");
			ex.printStackTrace();
			fail("SQL statement failed. See system console for details.");
		} finally {
			try {
				if (stmt != null) stmt.close();
			} catch (SQLException e) {
				System.out.println("Couldn't close statement");
				e.printStackTrace();
			}
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				System.out.println("Couldn't close connection");
				e.printStackTrace();
			}
		}
		
		SQLTable parent = db.getTableByName("relationship_test_parent");
		SQLTable child = db.getTableByName("relationship_test_child");
		
		if (parent == null) {
			parent = db.getTableByName("relationship_test_parent".toUpperCase());
		}
		SQLRelationship rel = (SQLRelationship) parent.getExportedKeys().get(0);
		
		assertEquals("relationship_test_fk", rel.getName().toLowerCase());
		assertSame(parent, rel.getPkTable());
		assertSame(child, rel.getFkTable());
		assertEquals((SQLRelationship.ZERO | SQLRelationship.ONE | SQLRelationship.MANY), rel.getFkCardinality());
		assertEquals(SQLRelationship.ONE, rel.getPkCardinality());
	}

	public void testAllowsChildren() {
		assertTrue(rel1.allowsChildren());
	}

	public void testSQLRelationship() throws ArchitectException {
		SQLRelationship rel = new SQLRelationship();
		assertNotNull(rel.getChildren());
		assertNotNull(rel.getSQLObjectListeners());
	}

	public void testGetMappingByPkCol() throws ArchitectException {
		SQLColumn col = parentTable.getColumnByName("pkcol_1");
		SQLRelationship.ColumnMapping m = rel1.getMappingByPkCol(col);
		assertEquals("pkcol_1", m.getPkColumn().getName());
		assertEquals("child_pkcol_1", m.getFkColumn().getName());

		// check another column (in case it always returns the first mapping or something)
		col = parentTable.getColumnByName("pkcol_2");
		m = rel1.getMappingByPkCol(col);
		assertEquals("pkcol_2", m.getPkColumn().getName());
		assertEquals("child_pkcol_2", m.getFkColumn().getName());
	}
	
	public void testGetNonExistentMappingByPkCol() throws ArchitectException {
		// check a column that's in the PK table but not in the mapping
		SQLColumn col = parentTable.getColumnByName("attribute_1");
		SQLRelationship.ColumnMapping m = rel1.getMappingByPkCol(col);
		assertNull(m);
	}

	/** This was a real regression */
	public void testDeletePkColRemovesFkCol() throws ArchitectException {
		SQLColumn pkcol = parentTable.getColumnByName("pkcol_1");
		assertNotNull("Child col should exist to start", childTable1.getColumnByName("child_pkcol_1"));
		parentTable.removeColumn(pkcol);
		assertNull("Child col should have been removed", childTable1.getColumnByName("child_pkcol_1"));
	}
	
	/**
	 * testing that a column gets hijacked and promoted to the primary key
	 * when the corrisponding pk column is added into the primary key 
	 * 
	 * @throws ArchitectException
	 */
	public void testHijackedColumnGoesToPK() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "hijack", Types.INTEGER, 10, 0);
		SQLColumn fkcol = new SQLColumn(childTable1, "hijack", Types.INTEGER, 10, 0);
		SQLRelationship rel = parentTable.getExportedKeys().get(0);
		childTable1.addColumn(0, fkcol);
		parentTable.addColumn(0, pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		assertNotNull("parent column didn't to go PK", pkcol.getPrimaryKeySeq());
		assertTrue("column didn't get hijacked", rel.containsFkColumn(fkcol));
		
		// this is the point of the test
		assertNotNull("column didn't go to primary key", fkcol.getPrimaryKeySeq());
	}
	
	/**
	 * testing that a column gets hijacked and promoted to the primary key
	 * when the corrisponding pk column is moved into the primary key from further
	 * down in its column list. 
	 * 
	 * @throws ArchitectException
	 */
	public void testHijackedColumnGoesToPK2() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "hijack", Types.INTEGER, 10, 0);
		SQLColumn fkcol = new SQLColumn(childTable1, "hijack", Types.INTEGER, 10, 0);
		SQLRelationship rel = parentTable.getExportedKeys().get(0);
		childTable1.addColumn( fkcol);
		parentTable.addColumn( pkcol);
		assertNull("pkcol already in the primary key",pkcol.getPrimaryKeySeq());
		pkcol.setPrimaryKeySeq(0);
		
		assertNotNull("parent column didn't to go PK", pkcol.getPrimaryKeySeq());
		assertTrue("column didn't get hijacked", rel.containsFkColumn(fkcol));
		
		// this is the point of the test
		assertNotNull("column didn't go to primary key", fkcol.getPrimaryKeySeq());
	}
	
	public void testFKColManagerRemovesImportedKey() throws ArchitectException {
		assertTrue("Parent table should export rel1",parentTable.getExportedKeys().contains(rel1));
		assertTrue("childTable1 should import rel1",childTable1.getImportedKeys().contains(rel1));
		assertEquals("Child's imported count is whacked out", 1, childTable1.getImportedKeys().size());
		
		assertNotNull("Missing imported key", childTable1.getColumnByName("child_pkcol_1"));
		assertNotNull("Missing imported key", childTable1.getColumnByName("child_pkcol_2"));
		int oldChildColCount = childTable1.getColumns().size();
		
		parentTable.removeExportedKey(rel1);

		assertFalse("Parent table should not export rel1 any more", parentTable.getExportedKeys().contains(rel1));
		assertFalse("childTable1 should not import rel1 any more", childTable1.getImportedKeys().contains(rel1));
				
		// the following tests depend on FKColumnManager behaviour, not UndoManager
		assertEquals("Relationship still attached to child", 0, childTable1.getImportedKeys().size());
		assertNull("Orphaned imported key", childTable1.getColumnByName("child_pkcol_1"));
		assertNull("Orphaned imported key", childTable1.getColumnByName("child_pkcol_2"));
		assertEquals("Child column list should have shrunk by 2", oldChildColCount - 2, childTable1.getColumns().size());
		assertNotNull("Missing exported key", parentTable.getColumnByName("pkcol_1"));
		assertNotNull("Missing exported key", parentTable.getColumnByName("pkcol_2"));
	}
	
	public void testRemovedRelationshipsDontInterfere() throws ArchitectException {
		testFKColManagerRemovesImportedKey();
		
		int oldChildColCount = childTable1.getColumns().size();
		
		SQLColumn pk3 = new SQLColumn(parentTable, "pk3", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pk3);
		pk3.setPrimaryKeySeq(0);
		
		assertEquals("Child table got new col!?!", oldChildColCount, childTable1.getColumns().size());
	}
	
	public void testRemoveChildTable() throws ArchitectException {
		
		assertEquals(3,database.getChildCount());
		assertEquals(2,parentTable.getExportedKeys().size());
		
		database.removeChild(childTable1);
		assertEquals(2,database.getChildCount());
		assertEquals(1,parentTable.getExportedKeys().size());
		
		assertNull("Child table not removed from the database",
				database.getTableByName(childTable1.getName()));
		assertFalse("Parent still contains a reference to a deleted table", 
				parentTable.getExportedKeys().contains(rel1));
		
		database.removeChild(childTable2);
		
		assertNull("Child table 2 not removed from the database",
				database.getTableByName(childTable2.getName()));
		assertFalse("Parent still contains a reference to a deleted table", 
				parentTable.getExportedKeys().contains(rel2));
		
		assertEquals(1,database.getChildCount());
		assertEquals(0,parentTable.getExportedKeys().size());
	}
	
	public void testRemoveParentTable() throws ArchitectException {
		database.removeChild(parentTable);
		assertNull("Child table not removed from the database",database.getTableByName(parentTable.getName()));
		assertFalse("Parent still contains a reference to a deleted table", 
				parentTable.getExportedKeys().contains(rel1));
	}
	
	public void testPKColNameChangeGoesToFKColWhenNamesWereSame() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		pkcol.setName("new name");
		
		assertEquals("fkcol's name didn't update", "new name", fkcol.getName());
	}

	public void testPKColNameChangeDoesntGoToFKColWhenNamesWereDifferent() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		fkcol.setName("custom fk col name");
		pkcol.setName("new name");
		
		assertEquals("fkcol's name didn't update", "custom fk col name", fkcol.getName());
	}

	public void testPKColTypeChangeGoesToFKCol() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		pkcol.setType(Types.BINARY);
		
		assertEquals("fkcol's type didn't update", Types.BINARY, fkcol.getType());
	}
    
    public void testCreateIdentifyingRelationship() throws ArchitectException {
        SQLTable parent = new SQLTable(null, "Parent", null, "TABLE", true);
        SQLTable child = new SQLTable(null, "Child", null, "TABLE", true);
        SQLColumn parentCol1 = new SQLColumn(null, "pk1", Types.INTEGER, 10, 0);
        SQLColumn childCol1 = new SQLColumn(null, "child_attr", Types.INTEGER, 10, 0);
        
        parent.addColumn(parentCol1);
        parentCol1.setPrimaryKeySeq(0);
        
        child.addColumn(childCol1);
        childCol1.setPrimaryKeySeq(null);
        
        SQLRelationship rel = new SQLRelationship();
        rel.setIdentifying(true);
        rel.attachRelationship(parent, child, true);
        
        assertEquals("pk1", parent.getColumn(0).getName());
        assertEquals(new Integer(0), parent.getColumn(0).getPrimaryKeySeq());
        
        assertEquals("pk1", child.getColumn(0).getName());
        assertEquals(new Integer(0), child.getColumn(0).getPrimaryKeySeq());
        assertEquals("child_attr", child.getColumn(1).getName());
        assertEquals(null, child.getColumn(1).getPrimaryKeySeq());
    }
    
    public void testCreateNonIdentifyingRelationship() throws ArchitectException {
        SQLTable parent = new SQLTable(null, "Parent", null, "TABLE", true);
        SQLTable child = new SQLTable(null, "Child", null, "TABLE", true);
        SQLColumn parentCol1 = new SQLColumn(null, "pk1", Types.INTEGER, 10, 0);
        SQLColumn childCol1 = new SQLColumn(null, "child_attr", Types.INTEGER, 10, 0);
        
        parent.addColumn(parentCol1);
        parentCol1.setPrimaryKeySeq(0);
        
        child.addColumn(childCol1);
        childCol1.setPrimaryKeySeq(null);
        
        SQLRelationship rel = new SQLRelationship();
        rel.setIdentifying(false);
        rel.attachRelationship(parent, child, true);
        
        assertEquals("pk1", parent.getColumn(0).getName());
        assertEquals(new Integer(0), parent.getColumn(0).getPrimaryKeySeq());
        
        assertEquals("child_attr", child.getColumn(0).getName());
        assertEquals(null, child.getColumn(0).getPrimaryKeySeq());
        assertEquals("pk1", child.getColumn(1).getName());
        assertEquals(null, child.getColumn(1).getPrimaryKeySeq());
    }

	public void testPKColPrecisionChangeGoesToFKCol() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		pkcol.setPrecision(20);
		
		assertEquals("fkcol's precision didn't update", 20, fkcol.getPrecision());
	}

	/** This is something the undo manager will attempt when you undo deleting a relationship */
	public void testReconnectOldRelationshipWithCustomMapping() throws ArchitectException {
		List<SQLColumn> origParentCols = new ArrayList<SQLColumn>(parentTable.getColumns()); 
		List<SQLColumn> origChild1Cols = new ArrayList<SQLColumn>(childTable1.getColumns());
		
		parentTable.removeExportedKey(rel1);
		rel1.attachRelationship(parentTable, childTable1, false);
		
		assertEquals("Exported key columns disappeared", origParentCols, parentTable.getColumns());
		assertEquals("Imported key columns didn't get put back", origChild1Cols, childTable1.getColumns());
		assertEquals("There are multiple copies of this relationship in the parent's exported keys folder",2,parentTable.getExportedKeys().size());
		assertEquals("There are multiple copies of this relationship in the child's imported keys folder",1,childTable1.getImportedKeys().size());
	}
	
	/** This is something the undo manager will attempt when you undo deleting a relationship */
	public void testReconnectOldRelationshipWithAutoMapping() throws ArchitectException {
		SQLTable myParent = new SQLTable(db, true);
		SQLColumn col;
		myParent.addColumn(col = new SQLColumn(myParent, "pkcol1", Types.VARCHAR, 10, 0));
		col.setPrimaryKeySeq(0);
		myParent.addColumn(col = new SQLColumn(myParent, "pkcol2", Types.VARCHAR, 10, 0));
		col.setPrimaryKeySeq(0);
		
		SQLTable myChild = new SQLTable(db, true);
		
		SQLRelationship myRel = new SQLRelationship();
		myRel.attachRelationship(myParent, myChild, true);
		List<SQLColumn> origParentCols = new ArrayList<SQLColumn>(myParent.getColumns()); 
		List<SQLColumn> origChildCols = new ArrayList<SQLColumn>(myChild.getColumns());

		// the next two lines are what the business model sees from undo/redo
		myParent.removeExportedKey(myRel);
		myRel.attachRelationship(myParent, myChild, false);
		
		assertEquals("Exported key columns disappeared", origParentCols, myParent.getColumns());
		assertEquals("Imported key columns didn't get put back", origChildCols, myChild.getColumns());
		assertEquals("There are multiple copies of this relationship in the parent's export keys folder",1,myParent.getExportedKeys().size());
		assertEquals("There are multiple copies of this relationship in the child's import keys folder",1,myChild.getImportedKeys().size());
	}

		
	public void testMovingPKColOutOfPK() throws ArchitectException {
		SQLColumn col = parentTable.getColumnByName("pkcol_1");
		
		col.setPrimaryKeySeq(null);
		assertTrue("pkcol_1 dropped from the parent table", parentTable.getColumns().contains(col));
	}
	public void testMovingPKColOutOfPKByColIndex() throws ArchitectException {
		SQLColumn col = parentTable.getColumnByName("pkcol_2");
		int index = parentTable.getColumnIndex(col);
		parentTable.changeColumnIndex(index,1,false);
		assertTrue("pkcol_1 dropped from the parent table", parentTable.getColumns().contains(col));
	}
    
    /**
     * The relationship manager was detaching from its whole table whenever
     * one relationship (not necessarily the listening one) was removed
     * from its pk table.  This test checks for that problem.
     */
    public void testRelManagerDoesntDetachEarly() {
        assertTrue(parentTable.getSQLObjectListeners().contains(rel1.getRelationshipManager()));
        assertTrue(parentTable.getSQLObjectListeners().contains(rel2.getRelationshipManager()));
        assertTrue(childTable2.getSQLObjectListeners().contains(rel2.getRelationshipManager()));
        
        parentTable.getExportedKeysFolder().removeChild(rel1);
        
        assertFalse(parentTable.getSQLObjectListeners().contains(rel1.getRelationshipManager()));
        
        // and finally, what we're testing for:
        assertTrue(parentTable.getSQLObjectListeners().contains(rel2.getRelationshipManager()));
        assertTrue(childTable2.getSQLObjectListeners().contains(rel2.getRelationshipManager()));
    }
    
    public void testAutoGeneratedColumnGoesIntoPK() throws ArchitectException {
        SQLColumn mycol = new SQLColumn(null, "my_column", Types.CHAR, 1000000, 0);
        parentTable.addColumn(0, mycol);
        mycol.setPrimaryKeySeq(0);
        assertTrue(mycol.isPrimaryKey());
        assertTrue(rel1.isIdentifying());
        
        // and the point of the test...
        SQLColumn generatedCol = childTable1.getColumnByName("my_column"); 
        System.out.println("Columns of childTable1: "+childTable1.getColumns());
        System.out.println("Column 0 pk value:"+childTable1.getColumn(0).getPrimaryKeySeq());
        assertTrue(childTable1.getColumnIndex(generatedCol) < childTable1.getPkSize());
        assertTrue(generatedCol.isPrimaryKey());
    }
    
    public void testCreateMappingsFiresEvents() throws ArchitectException {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        rel1.addSQLObjectListener(l);
        SQLRelationship.ColumnMapping columnMapping = new SQLRelationship.ColumnMapping();
        columnMapping.setPkColumn(parentTable.getColumn(0));
        columnMapping.setFkColumn(childTable1.getColumn(0));
        rel1.addChild(0,columnMapping);
        assertEquals(1, l.getInsertedCount());
    }
    
    public void testRemoveMappingsFiresEvents() {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        rel1.addSQLObjectListener(l);
        rel1.removeChild(0);
        assertEquals(1, l.getRemovedCount());
    }

    public void testRelationshipManagerRemoveMappingsFiresEvents() throws ArchitectException {
        CountingSQLObjectListener l = new CountingSQLObjectListener();
        rel1.addSQLObjectListener(l);
        parentTable.removeColumn(0);
        assertEquals(1, l.getRemovedCount());
    }
    
    /**
     * This test comes from the post in the forums (post 1670) that foreign keys
     * get left behind when an identifying relationship is removed.
     *
     */
    public void testDeletingIdentifyingRelationshipDoesntStrandKeys() throws ArchitectException {
        database = new SQLDatabase();
        
        SQLTable table1 = new SQLTable(database, "table1", null, "TABLE", true);
        SQLColumn table1PK = new SQLColumn(table1, "pkcol_1", Types.INTEGER, 10, 0);
        table1PK.setPrimaryKeySeq(0);
        table1.addColumn(table1PK);
        
        SQLTable table2 = new SQLTable(database, "table2", null, "TABLE", true);
        SQLColumn table2PK = new SQLColumn(table2, "pkcol_2", Types.INTEGER, 10, 0);
        table2PK.setPrimaryKeySeq(0);
        table2.addColumn(table2PK);
        
        SQLTable table3 = new SQLTable(database, "table3", null, "TABLE", true);
        SQLColumn table3PK = new SQLColumn(table3, "pkcol_3", Types.INTEGER, 10, 0);
        table3PK.setPrimaryKeySeq(0);
        table3.addColumn(table3PK);
        
        SQLRelationship relTable3to2 = new SQLRelationship();
        relTable3to2.setIdentifying(true);
        relTable3to2.attachRelationship(table3,table2,true);
        relTable3to2.setName("relTable3to2");
    
        SQLRelationship relTable2to1 = new SQLRelationship();
        relTable2to1.setName("relTable2to1");
        relTable2to1.attachRelationship(table2,table1,true);
        
        assertTrue("The column pkcol_3 was not added to table1 by the relations correctly", 
                table1.getColumnByName("pkcol_3") != null);
        
        relTable3to2.getPkTable().removeExportedKey(relTable3to2);
        
        //This is what we really want to test
        assertNull("The column created by the relations was not " +
                "removed when the relation was removed", table1.getColumnByName("pkcol_3"));
    }

    /**
     * This is a regression test for the problem where a table's only primary
     * key column has been inherited via a relationship.  The primary key name
     * was coming up null in that case.
     */
    public void testNonNullPrimaryKeyNameWhenInheritingOnlyPKColumn() throws Exception {
        database = new SQLDatabase();
        
        SQLTable table1 = new SQLTable(database, "table1", null, "TABLE", true);
        SQLColumn table1PK = new SQLColumn(table1, "pkcol_1", Types.INTEGER, 10, 0);
        table1PK.setPrimaryKeySeq(0);
        table1.addColumn(table1PK);
        
        SQLTable table2 = new SQLTable(database, "table2", null, "TABLE", true);
        SQLRelationship relTable1to2 = new SQLRelationship();
        relTable1to2.setIdentifying(true);
        relTable1to2.setName("one_to_two_fk");
        relTable1to2.attachRelationship(table1, table2, true);

        assertEquals("pkcol_1", table2.getColumn(0).getName());
        assertTrue(table2.getColumn(0).isPrimaryKey());
        assertNotNull(table2.getColumn(0).getPrimaryKeySeq());

        assertNotNull(table2.getPrimaryKeyIndex());
        assertNotNull(table2.getPrimaryKeyName());
    }
}
