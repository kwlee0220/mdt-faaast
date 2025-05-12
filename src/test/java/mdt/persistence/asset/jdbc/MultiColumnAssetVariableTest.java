package mdt.persistence.asset.jdbc;

import java.time.Duration;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mdt.DefaultElementLocation;
import mdt.persistence.asset.AssetVariableConfig;
import mdt.persistence.asset.jdbc.MultiColumnAssetVariableConfig.ColumnConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiColumnAssetVariableTest {
	private ObjectMapper m_mapper = new ObjectMapper();

	private static final String JSON
		= "{\"@type\":\"mdt:asset:jdbc:multi-column\","
			+ "\"element\":\"Data:DataInfo.Equipment.EquipmentParameterValues\","
			+ "\"jdbcConfigKey\":\"postgres\",\"table\":\"test_table\",\"whereClause\":\"where id = 1\","
			+ "\"columns\":[{\"column\":\"equipment_status\",\"path\":\"DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue\"},"
						+ "{\"column\":\"cycle_time\",\"path\":\"DataInfo.Equipment.EquipmentParameterValues[1].ParameterValue\"}]}";
	
	@Test
	public void testSerializeJdbcSimpleAssetVariable() throws JsonProcessingException {
		var loc = new DefaultElementLocation("Data", "DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue");
		MultiColumnAssetVariableConfig config
			= new MultiColumnAssetVariableConfig(loc, "postgres", null, "test_table", "where id = 1",
												List.of(new ColumnConfig("equipment_status", "DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue"),
														new ColumnConfig("cycle_time", "DataInfo.Equipment.EquipmentParameterValues[1].ParameterValue")));
		
		String json = m_mapper.writeValueAsString(config);
		Assert.assertEquals(JSON, json);
	}

	@Test
	public void testDeserializeMultiColumnAssetVariable() throws JsonProcessingException {
		MultiColumnAssetVariableConfig config
						= (MultiColumnAssetVariableConfig)m_mapper.readValue(JSON, AssetVariableConfig.class);

		Assert.assertEquals("Data", config.getElementLocation().getSubmodelIdShort());
		Assert.assertEquals("DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue",
							config.getElementLocation().getElementPath());
		Assert.assertEquals("postgres", config.getJdbcConfigKey());
		Assert.assertEquals(Duration.ZERO, config.getValidPeriod());
		Assert.assertEquals("select equipment_status, cycle_time from test_table where id = 1", config.getReadQuery());
		Assert.assertEquals("update test_table set equipment_status = ?, cycle_time = ? where id = 1", config.getUpdateQuery());
	}
}
