package cn.muzin.chameleon.pair;

/**
 * @author sirius
 * @since 2021/10/26
 */
public abstract class StructPair {

    private Class mainStruct;

    public void setMainStruct(Class mainStruct) {
        this.mainStruct = mainStruct;
    }

    public Class getMainStruct(){
        return mainStruct;
    }

}
