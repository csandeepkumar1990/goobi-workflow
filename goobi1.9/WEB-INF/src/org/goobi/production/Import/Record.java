package org.goobi.production.Import;

import java.util.ArrayList;
import java.util.List;

public class Record {

	private List<String> collections = new ArrayList<String>();
	private String data = "";
	private String id = "";

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(String data) {
		this.data = data;
	}

	/**
	 * @return the data
	 */
	public String getData() {
		return data;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	public void setCollections(List<String> collections) {
		this.collections = collections;
	}

	public List<String> getCollections() {
		return collections;
	}

}
