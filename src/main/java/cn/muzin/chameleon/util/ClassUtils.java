package cn.muzin.chameleon.util;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;


/**
 * ClassUtils
 * @since 1.1
 */
public abstract class ClassUtils {

	/**
	 * Suffix for array class names: {@code "[]"}.
	 */
	public static final String ARRAY_SUFFIX = "[]";

	/**
	 * Prefix for internal array class names: {@code "["}.
	 */
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/**
	 * Prefix for internal non-primitive array class names: {@code "[L"}.
	 */
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/**
	 * The package separator character: {@code '.'}.
	 */
	private static final char PACKAGE_SEPARATOR = '.';

	/**
	 * The path separator character: {@code '/'}.
	 */
	private static final char PATH_SEPARATOR = '/';

	/**
	 * The inner class separator character: {@code '$'}.
	 */
	private static final char INNER_CLASS_SEPARATOR = '$';

	/**
	 * The CGLIB class separator: {@code "$$"}.
	 */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/**
	 * The ".class" file suffix.
	 */
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * Map with primitive wrapper type as key and corresponding primitive
	 * type as value, for example: Integer.class -> int.class.
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(8);

	/**
	 * Map with primitive type as key and corresponding wrapper
	 * type as value, for example: int.class -> Integer.class.
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(8);

	/**
	 * Map with primitive type name as key and corresponding primitive
	 * type as value, for example: "int" -> "int.class".
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * Map with common Java language class name as key and corresponding Class as value.
	 * Primarily for efficient deserialization of remote invocations.
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * Common Java language interfaces which are supposed to be ignored
	 * when searching for 'primary' user-level interfaces.
	 */
	private static final Set<Class<?>> javaLanguageInterfaces;


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);

		// Map entry iteration is less expensive to initialize than forEach with lambdas
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		primitiveTypes.add(void.class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
	}

	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() returning null indicates the bootstrap ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				} catch (Throwable ex) {
					// Cannot access system ClassLoader - oh well, maybe the caller can live with null...
				}
			}
		}
		return cl;
	}

	public static ClassLoader overrideThreadContextClassLoader(ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		} else {
			return null;
		}
	}


	public static Class<?> forName(String name, ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" style arrays
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" style arrays
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" or "[[Ljava.lang.String;" style arrays
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return Class.forName(name, false, clToUse);
		} catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String innerClassName =
						name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return Class.forName(innerClassName, false, clToUse);
				} catch (ClassNotFoundException ex2) {
					// Swallow - let original exception get through
				}
			}
			throw ex;
		}
	}


	public static Class<?> resolveClassName(String className, ClassLoader classLoader)
			throws IllegalArgumentException {

		try {
			return forName(className, classLoader);
		} catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		} catch (LinkageError err) {
			throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
		}
	}

	public static boolean isPresent(String className, ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		} catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		} catch (Throwable ex) {
			// Typically ClassNotFoundException or NoClassDefFoundError...
			return false;
		}
	}

	public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			if (clazz.getClassLoader() == classLoader) {
				return true;
			}
		} catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Visible if same Class can be loaded from given ClassLoader
		return isLoadable(clazz, classLoader);
	}

	public static boolean isCacheSafe(Class<?> clazz, ClassLoader classLoader) {
		try {
			ClassLoader target = clazz.getClassLoader();
			// Common cases
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// Check for match in ancestors -> positive
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// Check for match in children -> negative
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		} catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Fallback for ClassLoaders without parent/child relationship:
		// safe if same Class can be loaded from given ClassLoader
		return (classLoader != null && isLoadable(clazz, classLoader));
	}

	private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// Else: different class with same name found
		} catch (ClassNotFoundException ex) {
			// No corresponding class found at all
			return false;
		}
	}


	public static Class<?> resolvePrimitiveClassName(String name) {
		Class<?> result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if (name != null && name.length() <= 8) {
			// Could be a primitive - likely.
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		return primitiveWrapperTypeMap.containsKey(clazz);
	}


	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}


	public static boolean isPrimitiveArray(Class<?> clazz) {
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}


	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}


	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		if (lhsType == null || rhsType == null) {
			return false;
		}
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			if (lhsType == resolvedPrimitive) {
				return true;
			}
		} else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			if (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)) {
				return true;
			}
		}
		return false;
	}

	public static List<Class<?>> getClassesFromPackage(String packageName) {
		return getClassesWithAnnotationFromPackage(packageName, null);
	}

	public static List<Class<?>> getClassesWithAnnotationFromPackage(String packageName,
																	 Class<? extends Annotation> annotation) {
		return getClassesWithAnnotationFromPackage(packageName, null, annotation);
	}

	public static List<Class<?>> getClassesWithAnnotationFromPackage(String packageName,
																	 ClassLoader classLoader,
																	 Class<? extends Annotation> annotation) {

		if(packageName == null){ packageName = ""; }

		List<Class<?>> classList = new ArrayList<Class<?>>();
		String packageDirName = packageName.replace('.', '/');
		Enumeration<URL> dirs = null;

		ClassLoader defaultClassLoader = null;

		if(classLoader != null){
			defaultClassLoader = classLoader;
		}else{
			defaultClassLoader = Thread.currentThread().getContextClassLoader();
		}

		try {
			dirs = defaultClassLoader.getResources(packageDirName);
		} catch (IOException e) {
			System.err.println("Failed to get resource" + e);
			return null;
		}

		while (dirs.hasMoreElements()) {
			URL url = dirs.nextElement();       //file:/D:/E/workspaceGitub/springboot/JSONDemo/target/classes/com/yq/controller
			String protocol = url.getProtocol();//file

			//https://docs.oracle.com/javase/7/docs/api/java/net/URL.html
			//http, https, ftp, file, and jar
			//本文只需要处理file和jar
			if ("file".equals(protocol) ) {
				String filePath = null;

				if(isWindows()) {
					if(url.getPath().startsWith("/")){
						filePath = url.getPath().substring(1);
					}else{
						filePath = url.getPath();
					}

				}else{
					try {
						filePath = URLDecoder.decode(url.getFile(), "UTF-8");///D:/E/workspaceGitub/springboot/JSONDemo/target/classes/com/yq/controller
					} catch (UnsupportedEncodingException e) {
						System.err.println("Failed to decode class file" + e);
					}
				}

				getClassesWithAnnotationFromFilePath(packageName, filePath, defaultClassLoader, annotation, classList);
			} else if ("jar".equals(protocol)) {
				JarFile jar = null;
				try {
					jar = ((JarURLConnection) url.openConnection()).getJarFile();
					//扫描jar包文件 并添加到集合中
				}
				catch (Exception e) {
					System.err.println("Failed to decode class jar" + e);
				}

				List<Class<?>> alClassList = new ArrayList<Class<?>>();
				findClassesByJar(packageName, jar, defaultClassLoader, alClassList);
				getClassesWithAnnotationFromAllClasses(alClassList, annotation, classList);
			}
			else {
				System.err.println("can't process the protocol= " + protocol);
			}
		}

		return classList;
	}

	public static void findClassesByJar(String pkgName, JarFile jar, List<Class<?>> classes){
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		findClassesByJar(pkgName, jar, contextClassLoader, classes);
	}

	public static void findClassesByJar(String pkgName, JarFile jar, ClassLoader classLoader, List<Class<?>> classes) {
		String pkgDir = pkgName.replace(".", "/");
		Enumeration<JarEntry> entry = jar.entries();

		while (entry.hasMoreElements()) {
			// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文
			JarEntry jarEntry = entry.nextElement();
			String name = jarEntry.getName();
			// 如果是以/开头的
			if (name.charAt(0) == '/') {
				// 获取后面的字符串
				name = name.substring(1);
			}

			if (jarEntry.isDirectory() || !name.startsWith(pkgDir) || !name.endsWith(".class")) {
				continue;
			}
			//如果是一个.class文件 而且不是目录
			// 去掉后面的".class" 获取真正的类名
			String className = name.substring(0, name.length() - 6);
			Class<?> tempClass = loadClass(className.replace("/", "."), classLoader);
			// 添加到集合中去
			if (tempClass != null) {
				classes.add(tempClass);
			}
		}
	}

	/**
	 * 加载类
	 * @param fullClsName 类全名
	 * @return class
	 */
	public static Class<?> loadClass(String fullClsName) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Class<?> aClass = loadClass(fullClsName, contextClassLoader);
		return aClass;
	}

	public static Class<?> loadClass(String fullClsName, ClassLoader classLoader) {
		try {
			return classLoader.loadClass(fullClsName);
		} catch (ClassNotFoundException e) {
			System.err.println("PkgClsPath loadClass" + e);
		}
		return null;
	}

	public static List<Class<?>> getClassesWithAnnotationFromFilePath(String packageName,
																	  String filePath,
																	  Class<? extends Annotation> annotation) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		List<Class<?>> classes = getClassesWithAnnotationFromFilePath(packageName, filePath, contextClassLoader, annotation);
		return classes;
	}

	public static List<Class<?>> getClassesWithAnnotationFromFilePath(String packageName,
																	  String filePath,
																	  ClassLoader classLoader,
																	  Class<? extends Annotation> annotation) {
		List<Class<?>> result = new ArrayList<>();
		getClassesWithAnnotationFromFilePath(packageName, filePath, classLoader, annotation, result);
		return result;
	}

	//filePath is like this 'D:/E/workspaceGitub/springboot/JSONDemo/target/classes/com/yq/controller'
	public static void getClassesWithAnnotationFromFilePath(String packageName, String filePath,
															ClassLoader classLoader,
															Class<? extends Annotation> annotation, List<Class<?>> classList) {
		Path dir = Paths.get(filePath);//D:\E\workspaceGitub\springboot\JSONDemo\target\classes\com\yq\controller

		try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {

			for(Path path : stream) {
				String fileName = String.valueOf(path.getFileName()); // for current dir , it is 'helloworld'
				//如果path是目录的话， 此处需要递归，
				boolean isDir = Files.isDirectory(path);
				if(isDir) {
					getClassesWithAnnotationFromFilePath(packageName + "." + fileName , path.toString(), classLoader, annotation, classList);
				}
				else  {
					String className = fileName.substring(0, fileName.length() - 6);

					Class<?> classes = null;
					String fullClassPath = packageName + "." + className;
					try {
						classes = classLoader.loadClass(fullClassPath);
					}catch (ClassNotFoundException e) {
						System.err.println("Failed to find class= " + fullClassPath + e);
					}

					if (null != classes && (annotation != null ? (null != classes.getAnnotation(annotation)) : true)) {
						classList.add(classes);
					}else if(annotation == null){
						classList.add(classes);
					}

				}
			}
		}
		catch (IOException e) {
			System.err.println("Failed to read class file" + e);
		}
	}

	public static void getClassesWithAnnotationFromAllClasses(List<Class<?>> inClassList,
															  Class<? extends Annotation> annotation,
															  List<Class<?>> outClassList) {
		for(Class<?> myClasss : inClassList) {
			if (null != myClasss && (annotation != null ? (null != myClasss.getAnnotation(annotation)) : true)) {
				outClassList.add(myClasss);
			}else if(annotation == null){
				outClassList.add(myClasss);
			}
		}
	}

	public static boolean isWindows(){
		return System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
	}

	public static Map<String, Class> scanClasses(String basePackage, Class<? extends Annotation> annotationClass) {
		return scanClasses(new String[]{basePackage}, annotationClass);
	}

	public static Map<String, Class> scanClasses(String[] basePackages, Class<? extends Annotation> annotationClass) {
		return null;
	}

}