package mdt.ksx9101;

import java.util.Map;

import lombok.Getter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Getter
public class JpaConfiguration {
	private JdbcConfiguration jdbc;
	private Map<String,String> properties;
}
