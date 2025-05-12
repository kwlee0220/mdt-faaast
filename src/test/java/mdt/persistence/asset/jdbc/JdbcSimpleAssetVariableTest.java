package mdt.persistence.asset.jdbc;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mdt.DefaultElementLocation;
import mdt.persistence.asset.AssetVariableConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class JdbcSimpleAssetVariableTest {
	private ObjectMapper m_mapper = new ObjectMapper();

	private static final String JSON
		= "{\"@type\":\"mdt:asset:jdbc:simple\","
			+ "\"element\":\"Data:DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue\","
			+ "\"jdbcConfigKey\":\"postgres\",\"readQuery\":\"select data from table\"}";
	
	@Test
	public void testSerializeJdbcSimpleAssetVariable() throws JsonProcessingException {
		var loc = new DefaultElementLocation("Data", "DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue");
		SimpleJdbcAssetVariableConfig config
			= new SimpleJdbcAssetVariableConfig(loc, "postgres", null, "select data from table", null);
		
		String json = m_mapper.writeValueAsString(config);
		Assert.assertEquals(JSON, json);
	}

	@Test
	public void testDeserializeJdbcSimpleAssetVariable() throws JsonProcessingException {
		SimpleJdbcAssetVariableConfig config
						= (SimpleJdbcAssetVariableConfig)m_mapper.readValue(JSON, AssetVariableConfig.class);
		
		Assert.assertEquals("Data", config.getElementLocation().getSubmodelIdShort());
		Assert.assertEquals("DataInfo.Equipment.EquipmentParameterValues[0].ParameterValue",
							config.getElementLocation().getElementPath());
		Assert.assertEquals("postgres", config.getJdbcConfigKey());
		Assert.assertEquals(Duration.ZERO, config.getValidPeriod());
		Assert.assertEquals("select data from table", config.getReadQuery());
		Assert.assertNull(config.getUpdateQuery());
	}
}
