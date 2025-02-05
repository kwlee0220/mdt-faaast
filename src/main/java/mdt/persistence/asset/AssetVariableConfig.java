package mdt.persistence.asset;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@JsonSerialize(using = AssetVariableConfig.Serializer.class)
@JsonDeserialize(using = AssetVariableConfig.Deserializer.class)
public interface AssetVariableConfig {
	/**
	 * 연결된 SubmodelElement가 포함된 Submodel의 idShort을 반환한다.
	 * 
	 * @return Submodel idShort.
	 */
	public String getSubmodelIdShort();
	
	/**
	 * 연결된 SubmodelElement가 포함된 SubmodelElement의 idShort 경로를 반환한다.
	 * 
	 * @return SubmodelElement idShort 경로.
	 */
	public String getElementPath();
	
	/**
	 * SubmodelElement의 값의 최대 유효기간을 반환한다.
	 * <p>
	 * 이 기간이 지나지 않은 상태의 {@link #load} 호출은 이전에 로드된 값이 반환된다.
	 * 
	 * @return 최대 유효 기간.
	 */
	public Duration getValidPeriod();
	
	/**
	 * 주어진 {@link JsonGenerator}에 객체를 사용하여 본 {@code ConnectedElementConfig} 객체를
	 * JSON 형식으로 직렬화한다.
	 * 
	 * @param gen	Json serialization에 사용할 {@link JsonGenerator} 객체.
	 * @throws IOException	직렬화 실패시.
	 */
	public void serialize(JsonGenerator gen) throws IOException;
	
	@SuppressWarnings("serial")
	public static class Deserializer extends StdDeserializer<AssetVariableConfig> {
		public Deserializer() {
			this(null);
		}
		public Deserializer(Class<?> vc) {
			super(vc);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public AssetVariableConfig deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JacksonException {
			JsonNode node = parser.getCodec().readTree(parser);
			
			try {
				String configClassName = node.get("@class").asText() + "Config";
				Class configClass = Class.forName(configClassName);
				Method parse = configClass.getMethod("parseJson", JsonNode.class);
				return (AssetVariableConfig)parse.invoke(null, node);
			}
			catch ( ClassNotFoundException | NoSuchMethodException | SecurityException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				throw new IOException("Failed to parse AssetParameterConfig, cause=" + e);
			}
		}
	}

	@SuppressWarnings("serial")
	public static class Serializer extends StdSerializer<AssetVariableConfig> {
		private Serializer() {
			this(null);
		}
		private Serializer(Class<AssetVariableConfig> cls) {
			super(cls);
		}
		
		@Override
		public void serialize(AssetVariableConfig config, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
			config.serialize(gen);
		}
	}
}
