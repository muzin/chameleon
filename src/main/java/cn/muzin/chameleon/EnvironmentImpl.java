package cn.muzin.chameleon;

import cn.muzin.chameleon.exception.ChameleonTrainException;

/**
 * EnvironmentImpl Demo
 * @author sirius
 * @since 2021/10/22
 */
public class EnvironmentImpl implements Environment {

    private Chameleon chameleon;

    private Class sourceClass;

    private Class destClass;

    public EnvironmentImpl(){
        throw new ChameleonTrainException("EnvironmentImpl cannot be created.");
    }

    public Chameleon getChameleon() {
        return chameleon;
    }

    public void setChameleon(Chameleon chameleon) {
        this.chameleon = chameleon;
    }

    public Class getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(Class sourceClass) {
        this.sourceClass = sourceClass;
    }

    public Class getDestClass() {
        return destClass;
    }

    public void setDestClass(Class destClass) {
        this.destClass = destClass;
    }

    public void transform(Object sourceObj, Object destObj, boolean adaptationStructureMismatch) {

        throw new ChameleonTrainException("transform cannot be invoke.");

        // 只处理 getter/setter 方法
        // 实现：
        // 1. 类型相同，直接转换
        // 1.1 类型为Collection<?>且泛型类相同，直接转换
        // 1.2 类型为Collection<?>且泛型类不同，转换泛型类，再转换
        // 1.3 类型为Collection<?>且泛型类不同，目标结果是String，原结果不为空的情况下，将原结果toString
        // 2. 类型不同 转换类型 再转换
        // 2.1 如果目标结果是String，原结果不为空的情况下，将原结果toString


        // 暂时 集合仅支持 List类型


        // * 待定 引入不同类型转换适配器
        // BigDecimal -> String

        // 代码实现：
        //
        // AStruct source = (AStruct) sourceObj;
        // BStruct dest = (BStruct) destObj;

        // 1. 类型相同，直接转换
        // dest.setWriteField(source.getReadField());


        // 1.1 类型为Collection<?>且泛型类相同，直接转换



        // 1.2 类型为Collection<?>且泛型类不同，转换泛型类，再转换


        // 2. 类型不同, 转换类型 再转换
        // Type readField = source.getReadField();
        // if(readField != null && adaptationStructureMismatch) {
        //      WriteField newWriteField = this.chameleon.transform(
        //              readField,
        //              WriteField.class,
        //              adaptationStructureMismatch
        //              );
        //      dest.setWriteField(newWriteField);
        // }

        // 2.1 如果目标结果是String，原结果不为空的情况下，将原结果toString
        // dest.setWriteField(source.getReadField() != null ? source.getReadField().toString() : null);

    }

}
