package cn.muzin.chameleon;

import java.util.List;

/**
 * @author sirius
 * @since 2021/10/22
 */
public class AStruct extends CStruct {

    private String name;
    private String age;
    private String weight;
    private String height;
    private String idcard;
    private String school;
    private String profile;
    private String photo;

    private AInnerStruct inner;

    private List<AInnerStruct> innerList;

    private Integer ttt;

    private List<Long> signList;

    private List<Long> strList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getIdcard() {
        return idcard;
    }

    public void setIdcard(String idcard) {
        this.idcard = idcard;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

//    public List<String> getSignList() {
//        return signList;
//    }
//
//    public void setSignList(List<String> signList) {
//        this.signList = signList;
//    }


    public AInnerStruct getInner() {
        return inner;
    }

    public void setInner(AInnerStruct inner) {
        this.inner = inner;
    }

    public Integer getTtt() {
        return ttt;
    }

    public void setTtt(Integer ttt) {
        this.ttt = ttt;
    }

    public List<Long> getSignList() {
        return signList;
    }

    public void setSignList(List<Long> signList) {
        this.signList = signList;
    }

    public List<Long> getStrList() {
        return strList;
    }

    public void setStrList(List<Long> strList) {
        this.strList = strList;
    }

    public List<AInnerStruct> getInnerList() {
        return innerList;
    }

    public void setInnerList(List<AInnerStruct> innerList) {
        this.innerList = innerList;
    }
}
