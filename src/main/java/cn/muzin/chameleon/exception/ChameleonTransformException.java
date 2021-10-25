package cn.muzin.chameleon.exception;

/**
 * @author sirius
 * @since 2021/10/23
 */
public class ChameleonTransformException extends RuntimeException {

    public ChameleonTransformException() {
        super();
    }

    public ChameleonTransformException(String message) {
        super(message);
    }

    public ChameleonTransformException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChameleonTransformException(Throwable cause) {
        super(cause);
    }

}
