package mdt.ksx9101;

import java.util.Map;

import lombok.Data;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Data
public class JPAConfiguration {
	private JdbcConfiguration jdbc;
	private Map<String,String> properties;
}
