package mdt.assetconnection.operation;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
public class ProgramOperationProviderConfig {
	private List<String> command = Lists.newArrayList();
	private File workingDirectory;
	private Duration timeout;
	private Set<String> valueArguments = Sets.newHashSet();
	
	@JsonCreator
	public ProgramOperationProviderConfig(@JsonProperty("command") List<String> command,
											@JsonProperty("workingDirectory") File workingDirectory,
											@JsonProperty("timeout") String timeoutStr,
											@JsonProperty("valueArguments") Set<String> valueArguments) {
		this.command = command;
		this.workingDirectory = workingDirectory;
		this.timeout = Duration.parse(timeoutStr);
		this.valueArguments = valueArguments;
	}
	
	public List<String> getCommand() {
		return command;
	}
	
	@JsonProperty("timeout")
	public String getTimeoutAsString() {
		return this.timeout.toString();
	}
}
