package cn.muzin.chameleon.trainer.code;

import cn.muzin.chameleon.util.VariableUtils;
import com.sun.beans.TypeResolver;
import javassist.Modifier;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.muzin.chameleon.trainer.TrainerConstant.*;

/**
 * @author sirius
 * @since 2021/12/7
 */
public class ClassReaderUtil {

    public static Map<String, Field> fieldsToFieldMap(List<Field> fields){
        HashMap<String, Field> map = new HashMap<>();
        for(Field field : fields){
            String fieldName = field.getName();
            if(!map.containsKey(fieldName)) {
                map.put(fieldName, field);
            }
        }
        return map;
    }

    public static List<Field> getAllFields(Class cls){
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

    public static List<Method> getAllMethods(Class cls){
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

    public static Method[] filterPublicMethods(Method[] methods){
        List<Method> allMethods = new ArrayList<Method>();
        for(Method method : methods){
            if(Modifier.isPublic(method.getModifiers())){
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[0]);
    }

    public static Method getReadMethod(Field field, Class clazz, Method[] methods){
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

    public static Method getWriteMethod(Field field, Class clazz, Method[] methods){
        String fieldName = field.getName();
        Class<?> type = field.getType();
        String fieldFirstUpper = VariableUtils.firstCharToUpper(fieldName);
        String writeMethodName = SET_PREFIX + fieldFirstUpper;
        Class<?>[] args = (type == null) ? null : new Class<?>[] { type };
        Method writeMethod = internalFindMethod(clazz, writeMethodName, methods, 1, args);
        return writeMethod;
    }

    public static Method internalFindMethod(Class<?> start, String methodName, Method[] methods,
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
    public static Class getGenericClassOfListByGenericParameterType(Method method){
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

    public static Class getGenericClassOfListByGenericReturnType(Method method){
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

    public static boolean isExtends(Class clazz, Class extendz){
        Class<?>[] interfacesArray = clazz.getInterfaces();//获取这个类的所以接口类数组
        boolean result = false;
        for (Class<?> item : interfacesArray) {
            if (item == extendz) { //判断是否有继承的接口
               result = true;
               break;
            }
        }
        return result;
    }

}
