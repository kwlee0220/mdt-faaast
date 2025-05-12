package mdt.persistence.asset.jdbc;

import java.time.Duration;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mdt.DefaultElementLocation;
import mdt.persistence.asset.AssetVariableConfig;
import mdt.persistence.asset.jdbc.MultiRowAssetVariableConfig.RowDefConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiRowAssetVariableTest {
	private ObjectMapper m_mapper = new ObjectMapper();

	private static final String JSON
		= "{\"@type\":\"mdt:asset:jdbc:multi-row\","
		+ "\"element\":\"Data:DataInfo.Equipment.EquipmentParameterValues\",\"jdbcConfigKey\":\"postgres\","
		+ "\"readQuery\":\"select query\",\"updateQuery\":\"update query\","
		+ "\"rows\":[{\"key\":\"EquipmentStatus\",\"path\":\"DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue\"},"
		+ "{\"key\":\"CycleTime\",\"path\":\"DataInfo.Equipment.EquipmentParameterValues[1].ParameterValue\"}]}";
	
	@Test
	public void testSerializeMultiRowAssetVariable() throws JsonProcessingException {
		var loc = new DefaultElementLocation("Data", "DataInfo.Equipment.EquipmentParameterValues");
		MultiRowAssetVariableConfig config
			= new MultiRowAssetVariableConfig(loc, "postgres", null, "select query", "update query",
					List.of(new RowDefConfig("EquipmentStatus", "DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue"),
							new RowDefConfig("CycleTime", "DataInfo.Equipment.EquipmentParameterValues[1].ParameterValue")));

		String json = m_mapper.writeValueAsString(config);
		Assert.assertEquals(JSON, json);
	}

	@Test
	public void testDeserializeMultiRowAssetVariable() throws JsonProcessingException {
		MultiRowAssetVariableConfig config
						= (MultiRowAssetVariableConfig)m_mapper.readValue(JSON, AssetVariableConfig.class);

		Assert.assertEquals("Data", config.getElementLocation().getSubmodelIdShort());
		Assert.assertEquals("DataInfo.Equipment.EquipmentParameterValues",
							config.getElementLocation().getElementPath());
		Assert.assertEquals("postgres", config.getJdbcConfigKey());
		Assert.assertEquals(Duration.ZERO, config.getValidPeriod());
		Assert.assertEquals("select query", config.getReadQuery());
		Assert.assertEquals("update query", config.getUpdateQuery());
	}
}
