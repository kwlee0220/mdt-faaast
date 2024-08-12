package mdt.ksx9101;

import lombok.Data;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Data
public class JdbcConfiguration {
	private String driverClassName;
	private String jdbcUrl;
	private String user;
	private String password;
	private int maxPoolSize = 0;
}
