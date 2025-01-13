package mdt.persistence;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
@JsonSerialize(using = AssetParameterConfig.Serializer.class)
@JsonDeserialize(using = AssetParameterConfig.Deserializer.class)
public interface AssetParameterConfig {
	public String getSubmodel();
	public String getPath();
	
	public void serialize(JsonGenerator gen) throws IOException;
	
	@SuppressWarnings("serial")
	public static class Deserializer extends StdDeserializer<AssetParameterConfig> {
		public Deserializer() {
			this(null);
		}
		public Deserializer(Class<?> vc) {
			super(vc);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public AssetParameterConfig deserialize(JsonParser parser, DeserializationContext ctxt)
			throws IOException, JacksonException {
			JsonNode node = parser.getCodec().readTree(parser);
			
			try {
				String configClassName = node.get("@class").asText() + "Config";
				Class configClass = Class.forName(configClassName);
				Method parse = configClass.getMethod("parseJson", JsonNode.class);
				return (AssetParameterConfig)parse.invoke(null, node);
			}
			catch ( ClassNotFoundException | NoSuchMethodException | SecurityException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				throw new IOException("Failed to parse AssetParameterConfig, cause=" + e);
			}
		}
	}

	@SuppressWarnings("serial")
	public static class Serializer extends StdSerializer<AssetParameterConfig> {
		private Serializer() {
			this(null);
		}
		private Serializer(Class<AssetParameterConfig> cls) {
			super(cls);
		}
		
		@Override
		public void serialize(AssetParameterConfig config, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
			config.serialize(gen);
		}
	}
}
