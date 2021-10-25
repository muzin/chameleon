package cn.muzin.chameleon;

import cn.muzin.chameleon.exception.ChameleonTrainException;
import cn.muzin.chameleon.util.ClassUtils;
import cn.muzin.chameleon.util.VariableUtils;
import com.sun.beans.TypeResolver;
import javassist.*;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sirius
 * @since 2021/10/22
 */
public class EnvironmentAdaptTrainer {

    private static final String CHAMELEON_CLASS_NAME = Chameleon.class.getName();

    private static final String CHAMELEON_CLASS_SIMPLE_NAME = Chameleon.class.getSimpleName();

    private static final String ENVIRONMENT_CLASS_NAME = Environment.class.getName();

    private static final String CLASS_NAME = Class.class.getName();

    private static final String OBJECT_NAME = Object.class.getName();

    private static final String BOOLEAN_CLASS_NAME = Boolean.class.getName();

    private static final String LIST_CLASS_NAME = List.class.getName();

    private static final String ARRAYLIST_CLASS_NAME = ArrayList.class.getName();

    private static final String GET_PREFIX = "get";
    private static final String SET_PREFIX = "set";
    private static final String IS_PREFIX = "is";

    private volatile String tmpDir = null;

    private volatile String packagePrefix = "";

    private Chameleon chameleon;



    public EnvironmentAdaptTrainer(Chameleon chameleon){
        this.chameleon = chameleon;
        this.packagePrefix = chameleon.getPackagePrefix();
        this.tmpDir = chameleon.getTmpdir();
    }

    /**
     * 训练 类型转换 环境
     * @param tClass 源Class
     * @param rClass 目标Class
     * @return Environment集合
     */
    public Map<Class, Environment> train(Class tClass, Class rClass){
        HashMap<Class, Environment> map = new HashMap<>();
        Environment tClassToRClassEnvironment = mockEnvironment(tClass, rClass);
        Environment rClassToTClassEnvironment = mockEnvironment(rClass, tClass);
        map.put(tClass, tClassToRClassEnvironment);
        map.put(rClass, rClassToTClassEnvironment);
        return map;
    }

    private ClassPool getClassPool(){
        ClassPool pool = ClassPool.getDefault();
        return pool;
    }

    /**
     * 模拟类型转换环境
     * @param tClass
     * @param rClass
     * @return
     */
    private Environment mockEnvironment(Class tClass, Class rClass){
        try {
            Class<Environment> environmentClass = generateEnironmentImpl(tClass, rClass);
            Environment environment = environmentClass.newInstance();
            environment.setSourceClass(tClass);
            environment.setDestClass(rClass);
            environment.setChameleon(this.chameleon);
            return environment;
        }catch(Exception e){
            throw new ChameleonTrainException(e);
        }
    }

    /**
     * 生成 环境 实现
     * @param tClass
     * @param rClass
     * @return
     * @throws NotFoundException
     * @throws CannotCompileException
     * @throws IOException
     */
    private Class<Environment> generateEnironmentImpl(Class tClass, Class rClass)
            throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException {
        String tClassSimpleName = tClass.getSimpleName();
        String rClassSimpleName = rClass.getSimpleName();

        String environmentImplClassName =
                packagePrefix + "." + tClassSimpleName + "To" + rClassSimpleName + "EnvironmentImpl";

        ClassPool pool = getClassPool();

        // 创建一个空类
        CtClass cc = pool.makeClass(environmentImplClassName);

        // 实现 environment 接口
        cc.addInterface(pool.get(ENVIRONMENT_CLASS_NAME));

        // 添加 EnvironmentImpl 无参的构造函数
        CtConstructor cons = new CtConstructor(new CtClass[]{}, cc);
        cons.setBody("{}");
        cc.addConstructor(cons);

        // 新增一个字段 private Chameleon chameleon;
        CtField chameleonParam = new CtField(pool.get(CHAMELEON_CLASS_NAME), "chameleon", cc);
        chameleonParam.setModifiers(Modifier.PRIVATE);
        cc.addField(chameleonParam);
        // 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setChameleon", chameleonParam));
        cc.addMethod(CtNewMethod.getter("getChameleon", chameleonParam));

        // 新增一个字段 private Class sourceClass;
        CtField sourceClassParam = new CtField(pool.get(CLASS_NAME), "sourceClass", cc);
        sourceClassParam.setModifiers(Modifier.PRIVATE);
        cc.addField(sourceClassParam);
        // 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setSourceClass", sourceClassParam));
        cc.addMethod(CtNewMethod.getter("getSourceClass", sourceClassParam));

        // 新增一个字段 private Class destClass;
        CtField destClassParam = new CtField(pool.get(CLASS_NAME), "destClass", cc);
        destClassParam.setModifiers(Modifier.PRIVATE);
        cc.addField(destClassParam);
        // 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setDestClass", destClassParam));
        cc.addMethod(CtNewMethod.getter("getDestClass", destClassParam));


        // 创建一个名为transform方法
        String transformCtMethodName = "transform";
        CtClass[] transformCtMethodParams = new CtClass[]{
                pool.get(OBJECT_NAME),
                pool.get(OBJECT_NAME),
                CtClass.booleanType};

        CtMethod transformCtMethod = new CtMethod(CtClass.voidType,
                    transformCtMethodName,
                    transformCtMethodParams,
                    cc);
        transformCtMethod.setModifiers(Modifier.PUBLIC);

        String transformMethodBody = generateTransformMethodBody(tClass, rClass, transformCtMethod);
        transformCtMethod.setBody(transformMethodBody);

        cc.addMethod(transformCtMethod);


        // 这里会将这个创建的类对象编译为.class文件
        String tmpDir = getTmpDir();
        cc.writeFile(tmpDir);

        // 获取 class
        Class<Environment> environmentClass = null;
        try {
            environmentClass = (Class<Environment>) cc.toClass();
        }catch(Exception e){
            environmentImplClassName += "$" + new Object().hashCode();
            if(cc.isFrozen()){
                cc.defrost();
            }
            cc.setName(environmentImplClassName);
            environmentClass = (Class<Environment>) cc.toClass();
        }

        if(cc.isFrozen()){
            cc.defrost();
        }

        // 释放 classPool 中的该类
        cc.detach();

        return environmentClass;
    }

    /**
     * 生成 transform 方法体
     * @param sourceClass
     * @param destClass
     * @return
     */
    private String generateTransformMethodBody(Class sourceClass, Class destClass, CtMethod ctMethod) throws NotFoundException, CannotCompileException {

        String sourceClassSimpleName = sourceClass.getSimpleName();
        String destClassSimpleName = destClass.getSimpleName();
        String sourceClassName = sourceClass.getName();
        String destClassName = destClass.getName();

        ClassPool pool = getClassPool();

        StringBuilder stringBuilder = new StringBuilder();

        String sourceVariableName = "source";
        String destVariableName = "dest";


        // 给 形参 命名
        stringBuilder.append(sourceClassName + " " + sourceVariableName + " = (" + sourceClassName + ") $1;\n");
        stringBuilder.append(destClassName + " " + destVariableName + " = (" + destClassName + ") $2;\n");

        List<Field> sourceClassFields = getAllFields(sourceClass);
        List<Field> destClassFields = getAllFields(destClass);
        Method[] sourceClassMethods = filterPublicMethods(sourceClass.getMethods());
        Method[] destClassMethods = filterPublicMethods(destClass.getMethods());

        Map<String, Field> destClassFieldMap = fieldsToFieldMap(destClassFields);

        for(Field sourceClassField : sourceClassFields){
            String sourceClassFieldName = sourceClassField.getName();
            if(destClassFieldMap.containsKey(sourceClassFieldName)){
                Field destClassField = destClassFieldMap.get(sourceClassFieldName);
                String destClassFieldName = destClassField.getName();

                Method readMethod = getReadMethod(sourceClassField, sourceClass, sourceClassMethods);
                Method writeMethod = getWriteMethod(destClassField, destClass, destClassMethods);

                Class<?> readMethodReturnType = readMethod.getReturnType();
                Class<?> writeMethodParameterType = writeMethod != null
                        ? writeMethod.getParameterTypes()[0]
                        : null;

                String readMethodName = readMethod.getName();
                String writeMethodName = writeMethod.getName();

                // 类型相同
                if(ClassUtils.isAssignable(writeMethodParameterType, readMethodReturnType)){

                    // 如果是集合
                    boolean assignableFromReturnTypeList = readMethodReturnType.isAssignableFrom(List.class);
                    if(assignableFromReturnTypeList) {
                        boolean assignableFromParamTypeList = writeMethodParameterType.isAssignableFrom(List.class);
                        if(!assignableFromParamTypeList){ continue; }

                        Class readMethodGenericReturnType = getGenericClassOfListByGenericReturnType(readMethod);
                        Class writeMethodGenericParameterType = getGenericClassOfListByGenericParameterType(writeMethod);

                        String readMethodGenericReturnTypeName = readMethodGenericReturnType.getName();
                        String writeMethodGenericParameterTypeName = writeMethodGenericParameterType.getName();

                        // 如果 两个泛型类型 相同直接转换
                        if(readMethodGenericReturnType == writeMethodGenericParameterType){
                            // Simple Field Convert
                            simpleAssignValueConvertForTransformMethodBody(stringBuilder,
                                    destVariableName, writeMethodName,
                                    sourceVariableName, readMethodName);
                        }else{
                            // 两个泛型类型不同

                            String chameleonVariableName = VariableUtils.firstCharToLower(CHAMELEON_CLASS_SIMPLE_NAME);
                            String readMethodReturnTypeName = readMethodReturnType.getName();
                            String writeMethodParameterTypeName = writeMethodParameterType.getName();

                            if(writeMethodGenericParameterTypeName.startsWith("java.lang.")){
                                // 如果 目标泛型类型 为 String， 原目标进行 toString
                                if(writeMethodGenericParameterType == String.class){
                                    // collection toString Field Convert
                                    //
                                    //
                                    // Examples:
                                    // Collection<Type> readFieldCollection = source.getReadField();
                                    // if(readFieldCollection != null && adaptationStructureMismatch) {
                                    //      List<Type> newWriteFieldCollection = new ArrayList<Type>();
                                    //      for(Type item : readFieldCollection){
                                    //          newWriteFieldCollection.add(item != null ? item.toString() : null);
                                    //      }
                                    //      dest.setWriteField(newWriteFieldCollection);
                                    // }
                                    stringBuilder.append(LIST_CLASS_NAME + " "
                                            + sourceVariableName + sourceClassFieldName + "Collection = "
                                            + sourceVariableName + "." + readMethodName + "();\n");
                                    stringBuilder.append("if(" + sourceVariableName + sourceClassFieldName + "Collection != null && $3){\n");
                                    stringBuilder.append("\t" + LIST_CLASS_NAME + " new" + destVariableName + destClassFieldName + "Collection = "
                                            + "new " + ARRAYLIST_CLASS_NAME + "();\n");
                                    stringBuilder.append("int " + sourceVariableName + sourceClassFieldName + "CollectionSize"
                                            + " = " + sourceVariableName + sourceClassFieldName + "Collection.size();");
                                    stringBuilder.append("\tfor(int i = 0; i < " + sourceVariableName + sourceClassFieldName + "CollectionSize; i++){ \n");
                                    stringBuilder.append("\t\t" + OBJECT_NAME + " item = " + sourceVariableName + sourceClassFieldName + "Collection.get(i);");
                                    stringBuilder.append("\t\tnew" + destVariableName + destClassFieldName + "Collection.add(item != null ? item.toString() : null);\n");
                                    stringBuilder.append("\t}\n");
                                    stringBuilder.append("\t" + destVariableName + "." + writeMethodName
                                            + "(new" + destVariableName + destClassFieldName + "Collection);\n");
                                    stringBuilder.append("}\n");
                                }else {
                                    // 如果 目标泛型类型 为 其他 java.lang 包下面的类， 跳过
                                    continue;
                                }
                            }else{

                                //
                                // Examples:
                                // Collection<Type> readFieldCollection = source.getReadField();
                                // if(readFieldCollection != null && adaptationStructureMismatch) {
                                //      List<WriteField> newWriteFieldCollection = this.chameleon.transform(
                                //              readField,
                                //              WriteField.class,
                                //              adaptationStructureMismatch
                                //              );
                                //      dest.setWriteField(newWriteField);
                                // }

                                stringBuilder.append(LIST_CLASS_NAME + " "
                                        + sourceVariableName + sourceClassFieldName + "Collection = "
                                        + sourceVariableName + "." + readMethodName + "();\n");
                                stringBuilder.append("if(" + sourceVariableName + sourceClassFieldName + "Collection != null && $3){\n");
                                stringBuilder.append("\t" + LIST_CLASS_NAME
                                        + " new" + destVariableName + destClassFieldName + "Collection = ("
                                        + writeMethodParameterTypeName + ") "
                                        + "$0." + chameleonVariableName + ".transform("
                                        + sourceVariableName + sourceClassFieldName + "Collection, "
                                        + writeMethodGenericParameterTypeName + ".class, "
                                        + "$3);\n");
                                stringBuilder.append("\t" + destVariableName + "." + writeMethodName
                                        + "(new" + destVariableName + destClassFieldName + "Collection);\n");
                                stringBuilder.append("}\n");

                            }

                        }

                    }else{
                        // Simple Field Convert
                        simpleAssignValueConvertForTransformMethodBody(stringBuilder,
                                destVariableName, writeMethodName,
                                sourceVariableName, readMethodName);
                    }

                }else{
                    // 类型不同

                    // 如果 类型 不同，且 type 为 null 跳过
                    if(writeMethodParameterType == null
                            || readMethodReturnType == null){
                        continue;
                    }

                    String chameleonVariableName = VariableUtils.firstCharToLower(CHAMELEON_CLASS_SIMPLE_NAME);
                    String readMethodReturnTypeName = readMethodReturnType.getName();
                    String writeMethodParameterTypeName = writeMethodParameterType.getName();

                    // 如果 WriteField 是 字符串，将 ReadField转换为 toString
                    if(writeMethodParameterTypeName.startsWith("java.lang.String")){
                        // toString Field Convert
                        toStringAssignValueConvertForTransformMethodBody(stringBuilder,
                                destVariableName, writeMethodName,
                                sourceVariableName, readMethodName);
                    }

                    if(!writeMethodParameterTypeName.startsWith("java.lang.")) {
                        // Examples:
                        // Type readField = source.getReadField();
                        // if(readField != null && adaptationStructureMismatch) {
                        //      WriteField newWriteField =
                        //              (WriteField) chameleon.transform(readField, WriteField.class, adaptationStructureMismatch);
                        //      dest.setWriteField(newWriteField);
                        // }
                        stringBuilder.append(readMethodReturnTypeName + " "
                                + sourceVariableName + sourceClassFieldName + " = "
                                + sourceVariableName + "." + readMethodName + "();\n");
                        stringBuilder.append("if(" + sourceVariableName + sourceClassFieldName + " != null && $3){\n");
                        stringBuilder.append("\t" + writeMethodParameterTypeName
                                + " new" + destVariableName + destClassFieldName + " = ("
                                + writeMethodParameterTypeName + ") "
                                + "$0." + chameleonVariableName + ".transform("
                                + sourceVariableName + sourceClassFieldName + ", "
                                + writeMethodParameterTypeName + ".class, "
                                + "$3);\n");
                        stringBuilder.append("\t" + destVariableName + "." + writeMethodName
                                + "(new" + destVariableName + destClassFieldName + ");\n");
                        stringBuilder.append("}\n");
                    }
                }

            }
        }

        String str = stringBuilder.toString();

        return "{\n" + str + "\n}";
    }

    /**
     * 简单赋值 代码
     *
     * Examples:
     *  dest.setWriteField(source.getReadField());
     *
     */
    private void simpleAssignValueConvertForTransformMethodBody(StringBuilder stringBuilder,
                                                          String destVariableName,
                                                          String destMethodName,
                                                          String sourceVariableName,
                                                          String sourceMethodName){
        // Examples: dest.setWriteField(source.getReadField());
        stringBuilder.append(destVariableName + "." + destMethodName + "("
                + sourceVariableName + "." + sourceMethodName + "());\n");
    }

    /**
     * 原结果toString后进行赋值
     *
     * Examples:
     *  dest.setWriteField(source.getReadField() != null ? source.getReadField().toString() : null);
     *
     */
    private void toStringAssignValueConvertForTransformMethodBody(StringBuilder stringBuilder,
                                                                  String destVariableName,
                                                                  String destMethodName,
                                                                  String sourceVariableName,
                                                                  String sourceMethodName){
        // Examples: dest.setWriteField(source.getReadField() != null ? source.getReadField().toString() : null);
        stringBuilder.append(destVariableName + "." + destMethodName + "("
                + sourceVariableName + "." + sourceMethodName + "() != null ? "
                + sourceVariableName + "." + sourceMethodName + "().toString() : null);\n");
    }

    private Map<String, Field> fieldsToFieldMap(List<Field> fields){
        HashMap<String, Field> map = new HashMap<>();
        for(Field field : fields){
            String fieldName = field.getName();
            if(!map.containsKey(fieldName)) {
                map.put(fieldName, field);
            }
        }
        return map;
    }

    private List<Field> getAllFields(Class cls){
        List<Field> allFields = new ArrayList<Field>();
        if(cls == null){ return allFields; }
        Class<?> currentClass = cls;
        while (currentClass != null) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                allFields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    private List<Method> getAllMethods(Class cls){
        List<Method> allMethods = new ArrayList<Method>();
        if(cls == null){ return allMethods; }
        Class<?> currentClass = cls;
        while (currentClass != null) {
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                allMethods.add(method);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allMethods;
    }

    private Method[] filterPublicMethods(Method[] methods){
        List<Method> allMethods = new ArrayList<Method>();
        for(Method method : methods){
            if(Modifier.isPublic(method.getModifiers())){
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[0]);
    }

    private Method getReadMethod(Field field, Class clazz, Method[] methods){
        String fieldName = field.getName();
        String fieldFirstUpper = VariableUtils.firstCharToUpper(fieldName);
        String readMethodName = GET_PREFIX + fieldFirstUpper;
        Method readMethod = internalFindMethod(clazz, readMethodName, methods, 0, null);
        if (readMethod == null) {
            Class<?> type = field.getType();
            if (type == boolean.class || type == null) {
                readMethodName = IS_PREFIX + fieldFirstUpper;
                readMethod = internalFindMethod(clazz, readMethodName, methods, 0, null);
            }
        }
        return readMethod;
    }

    private Method getWriteMethod(Field field, Class clazz, Method[] methods){
        String fieldName = field.getName();
        Class<?> type = field.getType();
        String fieldFirstUpper = VariableUtils.firstCharToUpper(fieldName);
        String writeMethodName = SET_PREFIX + fieldFirstUpper;
        Class<?>[] args = (type == null) ? null : new Class<?>[] { type };
        Method writeMethod = internalFindMethod(clazz, writeMethodName, methods, 1, args);
        return writeMethod;
    }

    private Method internalFindMethod(Class<?> start, String methodName, Method[] methods,
                                             int argCount, Class args[]) {
        // For overriden methods we need to find the most derived version.
        // So we start with the given class and walk up the superclass chain.

        Method method = null;

        for (Class<?> cl = start; cl != null; cl = cl.getSuperclass()) {
            if(methods == null) {
                methods = cl.getMethods();
            }
            for (int i = 0; i < methods.length; i++) {
                method = methods[i];
                if (method == null) {
                    continue;
                }

                // make sure method signature matches.
                if (method.getName().equals(methodName)) {
                    Type[] params = method.getGenericParameterTypes();
                    if (params.length == argCount) {
                        if (args != null) {
                            boolean different = false;
                            if (argCount > 0) {
                                for (int j = 0; j < argCount; j++) {
                                    if (TypeResolver.erase(TypeResolver.resolveInClass(start, params[j])) != args[j]) {
                                        different = true;
                                        continue;
                                    }
                                }
                                if (different) {
                                    continue;
                                }
                            }
                        }
                        return method;
                    }
                }
            }
        }
        method = null;

        // Now check any inherited interfaces.  This is necessary both when
        // the argument class is itself an interface, and when the argument
        // class is an abstract class.
        Class ifcs[] = start.getInterfaces();
        for (int i = 0 ; i < ifcs.length; i++) {
            // Note: The original implementation had both methods calling
            // the 3 arg method. This is preserved but perhaps it should
            // pass the args array instead of null.
            method = internalFindMethod(ifcs[i], methodName, null, argCount, null);
            if (method != null) {
                break;
            }
        }
        return method;
    }

    /**
     * 获取 函数 第一个参数的泛型类
     * @param method
     * @return
     */
    private Class getGenericClassOfListByGenericParameterType(Method method){
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Class genericClass = null;
        if(genericParameterTypes.length > 0){
            Type genericParameterType = genericParameterTypes[0];
            if(genericParameterType instanceof ParameterizedTypeImpl){
                ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) genericParameterType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if(actualTypeArguments.length > 0){
                    Type actualTypeArgument = actualTypeArguments[0];
                    if(actualTypeArgument instanceof Class){
                        genericClass = (Class) actualTypeArgument;
                    }
                }
            }
        }
        return genericClass;
    }

    private Class getGenericClassOfListByGenericReturnType(Method method){
        Type genericParameterType = method.getGenericReturnType();
        Class genericClass = null;
        if(genericParameterType instanceof ParameterizedTypeImpl){
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) genericParameterType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if(actualTypeArguments.length > 0){
                Type actualTypeArgument = actualTypeArguments[0];
                if(actualTypeArgument instanceof Class){
                    genericClass = (Class) actualTypeArgument;
                }
            }
        }
        return genericClass;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }
}
