package cn.muzin.chameleon.pair;

/**
 * 一对一结构对
 *
 * 保存一个 AStruct 和 BStruct 的关系
 * @author sirius
 * @since 2021/10/26
 */
public class StructToOnePair extends StructPair {

    private Class struct;

    public StructToOnePair(Class mainStruct, Class bStruct){
        this.setMainStruct(mainStruct);
        this.struct = bStruct;
    }

    public Class getStruct() {
        return struct;
    }

    public void setStruct(Class struct) {
        this.struct = struct;
    }

}
