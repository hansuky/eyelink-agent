package com.m2u.eyelink.collector.common.elasticsearch;

public interface TableFactory {

	  /**
	   * Creates a new HTableInterface.
	   *
	   * @param config HBaseConfiguration instance.
	   * @param tableName name of the HBase table.
	   * @return HTableInterface instance.
	   */
	  Table getTable(TableName tableName);


	  /**
	   * Release the HTable resource represented by the table.
	   * @param table
	   */
	  void releaseTable(final Table table);
	}