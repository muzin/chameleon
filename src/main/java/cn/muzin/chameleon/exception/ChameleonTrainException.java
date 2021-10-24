package cn.muzin.chameleon.exception;

/**
 * @Author sirius
 * @create 2021/10/23
 */
public class ChameleonTrainException extends RuntimeException {

    public ChameleonTrainException() {
        super();
    }

    public ChameleonTrainException(String message) {
        super(message);
    }

    public ChameleonTrainException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChameleonTrainException(Throwable cause) {
        super(cause);
    }

}
