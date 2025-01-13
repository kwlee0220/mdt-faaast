package mdt.persistence;

import mdt.model.MDTException;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class AssetParameterException extends MDTException {
    private static final long serialVersionUID = 1L;

    public AssetParameterException(String msg) {
        super(msg);
    }

    public AssetParameterException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
