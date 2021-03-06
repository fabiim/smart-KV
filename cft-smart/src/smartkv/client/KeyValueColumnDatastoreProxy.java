/**
 * 
 */
package smartkv.client;

import java.util.Collection;
import java.util.Map;

/**
 * @author fabiim
 * 
 */

public interface KeyValueColumnDatastoreProxy extends TableDataStoreProxy{
	
	/**
	 * Insert a mapping from the specified key to the specified value and returns the previously existent value (if any). 
	 * <p>
	 * After a successful call to this method the specified table contains the specified key, and the mapped value can be obtained with the {@link #get} method.
	 * If the specified key already is present in the table, then the existent mapping will be replaced by the newly specified one and the old one will be returned. 
	 *     
	 * @param tableName the name of the table where the operation will be performed
	 * @param key the key to which to associated the value to
	 * @return The previously existent value associated with this key if the mapping is successfully created; <br/> 
	 *         <code>null</code> otherwise (note that it will be false when no table with the specified name exists or the previously existent value is <code>null</code>, the latter reason being implementation dependent).
	 */
	public Map<String,byte[]> put(String tableName, byte[] key, Map<String,byte[]> value);

	
	/**
	 * Return the value mapped by the specified key.
	 * @param tableName the name of the table where the operation will be performed
	 * @param key       the key whose associated value is to be returned 
	 * @return the byte array associated with the key if the key is present;   <br/>
	 *         <code>null</code> otherwise (note that it will be null when no table with the specified name exists). 
	 *         
	 */
	public  Map<String,byte[]> getValue(String tableName, byte[] key);
	

	/**
	 * Insert a mapping from the specified key to the specified value.  
	 * <p>
	 * This method differs from {@link put} only in the returned result. The <code>put</code> method returns the previously existent value (if any) while
	 * this method returns a <code>boolean</code> used to evaluate the success of this operation. As such this method should exhibit lower network usage given that 
	 * the data store does not return the previously existent mapped value. 
	 * 
	 * After a successful call to this method the specified table contains the specified key, and the mapped value can be obtained with the {@link #get} method.
	 * If the specified key already is present in the table, then the existent mapping will be replaced by the newly specified one.
	 *     
	 * @param tableName the name of the table where the operation will be performed
	 * @param key the key to which to associated the value to
	 * @return <code>true</code> if the mapping is inserted; <br/>
	 *         <code>false</code> otherwise (note that it will be false when no table with the specified name exists).
	 */
	public boolean insert(String tableName, byte[] key, Map<String,byte[]> value);
	

	/**
	 * Remove the existent specified mapping and return the existent value (if any). 
	 *  
	 * @param tableName the name of the table where the operation will be performed
	 * @param key the key to desired mapping
	 * @return <code>true</code> if the value  is replaced; <br/>
	 *         <code>false</code> otherwise (note that it will be false when no table with the specified name exists or the condition does not hold).
	 */
	public  Map<String,byte[]> removeValue(String tableName,byte[] key);

	/**
	 * <b>Atomically</b> replaces the entry for a key if currently mapped to the specified value.
	 * 
	 *  <p>
	 *  This method replaces the the current existent entry associated with the specified key if atomically both: <code>containsKey(tableName,key)</code> is <code>true</code> and 
	 *  <code>get(tableName,key).equals(oldValue)</code>.  
	 *  If so this method is successful, returns <code>true</code> and the <code>key</code> is currently mapped to <code>newValue</code>. Otherwise <code>false</code> is returned.   
	 * @param tableName the name of the table where the operation will be performed
	 * @param key  key with which the specified values are associated 
	 * @param oldValue  the expected current value associated with the specified key
	 * @param newValue  the value to be associated with the specified key
	 * @return <code>true</code> if the value  is replaced; <br/>
	 *         <code>false</code> otherwise (note that it will be false when no table with the specified name exists).
	 */
	public  boolean replace(String tableName, byte[] key,
			Map<String,byte[]> oldValue, Map<String,byte[]> newValue);
	
	/**
	 * Atomically removes the entry for a key in a table only if currently mapped to a given value.
	 * <p>
	 * For this operation to be successful (and return <code>true</code>) then there must be an existent mapping from <code>key</code> to some <code>value</code> such that 
	 * <code>value.equals(expectedValue)</code> is <code>true</code> 
	 * 
	 * @param tableName the name of the table where the operation will be performed
	 * @param key key with which the specified value is associated
	 * @param expectedValue the expected value 

	 * @return <code>true</code> if the value  is replaced; <br/>
	 *         <code>false</code> otherwise (note that it will be false when no table with the specified name exists or the condition does not hold).
	 */
	public  boolean remove(String tableName, byte[] key, Map<String,byte[]> expectedValue);
	
	/**
	 * Atomically inserts the specified mapping in a table only if  the mapping is present already.
	 * 
	 *   
	 * @param tableName the name of the table where the operation will be performed
	 * @param key key to associated the value with
	 * @param value the value to be inserted 
	 * @return The previously existent value associated with this key if the mapping is successfully deleted; <br/> 
	 *         <code>null</code> otherwise (note that it will be false when no table with the specified name exists or the previously table did not contain the specified mapping.
	 */
	public  Map<String,byte[]> putIfAbsent(String tableName, byte[] key, Map<String,byte[]> value);
	
	//FIXME : doc. What happens when setColumn when key is inexistent? 
	public boolean setColumn(String tableName, byte[] key, String columnName , byte[] value); 
	public byte[] getColumn(String tableName, byte[] key, String columnName);
	
	//FIXME  -URGENTE!!!!! 
	public Collection<Map<String,byte[]>> valueS(String tableName);


	/**
	 * @param tableName
	 * @param key
	 * @param columnName
	 * @return
	 */
	byte[] getColumnByReference(String tableName, byte[] key, String columnName);


	/**
	 * @param tableName
	 * @param serializeKey
	 * @return
	 */
	public Map<String, byte[]> getValueByReference(String tableName,
			byte[] serializeKey);
	
	
	
}
