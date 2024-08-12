package mdt.assetconnection.simulation;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.common.provider.config.AbstractMultiFormatOperationProviderConfig;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SimulationOperationProviderConfig extends AbstractMultiFormatOperationProviderConfig {
	private File m_workingDir;
	private List<String> m_commandPrefix;
	private Duration m_timeout;
	private Duration m_sessionRetainTimeout;
	
	public File getWorkingDirectory() {
		return m_workingDir;
	}
	public void setWorkingDirectory(String path) {
		m_workingDir = (path != null) ? new File(path) : null;
	}
	
	public List<String> getCommandPrefix() {
		return m_commandPrefix;
	}
	public void setCommandPrefix(List<String> prefix) {
		m_commandPrefix = prefix;
	}
	
	public long getTimeout() {
		return m_timeout.toMillis();
	}
	public void setTimeout(Duration timeout) {
		m_timeout = timeout;
	}
	public void setTimeout(String timeoutStr) {
		m_timeout = Duration.parse(timeoutStr);
	}
	
	public long getSessionRetainTimeout() {
		return m_sessionRetainTimeout.toMillis();
	}
	public void setSessionRetainTimeout(Duration timeout) {
		m_sessionRetainTimeout = timeout;
	}
	public void setSessionRetainTimeout(String timeoutStr) {
		m_sessionRetainTimeout = Duration.parse(timeoutStr);
	}
	
    @Override
    public int hashCode() {
        return Objects.hash(m_workingDir, m_commandPrefix, m_timeout, m_sessionRetainTimeout);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimulationOperationProviderConfig other = (SimulationOperationProviderConfig)obj;
        return super.equals(other)
                && Objects.equals(this.m_workingDir, other.m_workingDir)
                && Objects.equals(this.m_commandPrefix, other.m_commandPrefix)
                && Objects.equals(this.m_timeout, other.m_timeout)
                && Objects.equals(this.m_sessionRetainTimeout, other.m_sessionRetainTimeout);
    }
}
