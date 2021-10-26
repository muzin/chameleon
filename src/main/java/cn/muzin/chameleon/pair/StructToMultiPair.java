package cn.muzin.chameleon.pair;

import java.util.*;

/**
 * 一对多结构对
 * @author sirius
 * @since 2021/10/26
 */
public class StructToMultiPair extends StructPair {

    private Set<Class> structSet;

    public StructToMultiPair(){ }

    public StructToMultiPair(Class mainStruct){
        super.setMainStruct(mainStruct);
    }

    public void setMainStruct(Class mainStruct) {
        super.setMainStruct(mainStruct);
    }

    public Class getMainStruct(){
        return super.getMainStruct();
    }

    public void setStructList(Set<Class> structSet) {
        this.structSet = structSet;
    }

    public void addStruct(Class struct) {
        Set<Class> structList = getStructSet();
        structList.add(struct);
    }

    public void addStructs(Collection<Class> structs) {
        Set<Class> structList = getStructSet();
        structList.addAll(structs);
    }

    public Set<Class> getStructSet(){
        if(structSet == null){
            structSet = new HashSet<>();
        }
        return structSet;
    }

}
