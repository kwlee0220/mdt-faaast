package mdt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@JsonInclude(Include.NON_NULL)
@Getter
public class ElementColumnConfig {
	private final String name;
	private final String submodel;
	private final String path;
	
	@JsonCreator
	public ElementColumnConfig(@JsonProperty("name") String name,
								@JsonProperty("submodel") String submodel,
								@JsonProperty("path") String path) {
		this.name = name;
		this.submodel = submodel;
		this.path = path;
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s/%s", this.name, this.submodel, this.path);
	}
}
