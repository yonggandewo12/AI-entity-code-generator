package org.example.demotest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract tests for prompt-building or prompt-oriented service behavior.
 *
 * @author Liang.Xu
 */
class EntityGenerationPromptServiceContractTest {

    private static final String BASE_PACKAGE = "org.example.demotest";
    private static final String RESOURCE_PATH = "org/example/demotest";
    private static final String SAMPLE_SCHEMA = "{\"entities\":[{\"name\":\"Order\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"createdAt\",\"type\":\"datetime\"}]}]}";
    private static final String SAMPLE_ENTITY_NAME = "Order";

    @Test
    @DisplayName("Prompt/service output includes Java-specific generation guidance")
    void javaPromptMentionsJavaRequirements() throws Exception {
        PromptOutputs outputs = resolvePromptOutputs();
        String javaOutput = outputs.javaOutput.toLowerCase(Locale.ROOT);

        assertFalse(javaOutput.trim().isEmpty(), outputs.describe("Java output should not be blank"));
        assertTrue(
                javaOutput.contains("java")
                        || javaOutput.contains("class")
                        || javaOutput.contains("pojo")
                        || javaOutput.contains("getter")
                        || javaOutput.contains("setter"),
                outputs.describe("Expected Java-specific guidance in the resolved prompt/service output")
        );
    }

    @Test
    @DisplayName("Prompt/service output includes Go-specific generation guidance")
    void goPromptMentionsGoRequirements() throws Exception {
        PromptOutputs outputs = resolvePromptOutputs();
        String goOutput = outputs.goOutput.toLowerCase(Locale.ROOT);

        assertFalse(goOutput.trim().isEmpty(), outputs.describe("Go output should not be blank"));
        assertTrue(
                goOutput.contains("go")
                        || goOutput.contains("golang")
                        || goOutput.contains("struct")
                        || goOutput.contains("package")
                        || goOutput.contains("json tag"),
                outputs.describe("Expected Go-specific guidance in the resolved prompt/service output")
        );
    }

    @Test
    @DisplayName("Prompt/service output differs between Java and Go requests")
    void promptOrServiceOutputDiffersByTargetLanguage() throws Exception {
        PromptOutputs outputs = resolvePromptOutputs();

        assertNotEquals(
                outputs.javaOutput,
                outputs.goOutput,
                outputs.describe("Expected Java and Go prompt/service output to differ")
        );
    }

    private PromptOutputs resolvePromptOutputs() throws Exception {
        List<Class<?>> candidates = discoverApplicationClasses();
        if (candidates.isEmpty()) {
            fail("No application classes were found under " + BASE_PACKAGE + ".");
        }

        for (Class<?> candidate : candidates) {
            List<Method> methods = promptLikeMethods(candidate);
            if (methods.isEmpty()) {
                continue;
            }

            Object targetInstance = requiresInstance(methods) ? createInstance(candidate) : null;
            for (Method method : methods) {
                String javaOutput = invokeWithVariants(targetInstance, method, "java");
                String goOutput = invokeWithVariants(targetInstance, method, "go");
                if (javaOutput != null && goOutput != null) {
                    return new PromptOutputs(candidate, method, javaOutput, goOutput);
                }
            }
        }

        fail("Unable to locate a prompt-oriented method returning String under " + BASE_PACKAGE
                + ". Expose prompt-building logic through a prompt/service method so Java-vs-Go rules can be verified without invoking a real LLM API.");
        return null;
    }

    private List<Class<?>> discoverApplicationClasses() throws Exception {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_PATH);
        Set<String> classNames = new LinkedHashSet<String>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }

            Path root = Paths.get(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8.name()));
            if (!Files.exists(root)) {
                continue;
            }

            try (Stream<Path> classFiles = Files.walk(root)) {
                classFiles
                        .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                        .forEach(path -> classNames.add(toClassName(root, path)));
            }
        }

        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (String className : classNames) {
            if (className.contains("$") || className.endsWith("Test") || className.endsWith("Tests")) {
                continue;
            }
            Class<?> candidate = Class.forName(className);
            if (candidate.isInterface() || Modifier.isAbstract(candidate.getModifiers()) || candidate.isEnum()) {
                continue;
            }
            classes.add(candidate);
        }

        Collections.sort(classes, Comparator.comparingInt(this::classPriority).thenComparing(Class::getName));
        return classes;
    }

    private String toClassName(Path root, Path classFile) {
        String relativePath = root.relativize(classFile).toString();
        String withoutSuffix = relativePath.substring(0, relativePath.length() - ".class".length());
        return BASE_PACKAGE + "." + withoutSuffix.replace(File.separatorChar, '.');
    }

    private int classPriority(Class<?> type) {
        String simpleName = type.getSimpleName().toLowerCase(Locale.ROOT);
        int priority = 100;
        if (simpleName.contains("entity")) {
            priority -= 20;
        }
        if (simpleName.contains("service")) {
            priority -= 20;
        }
        if (simpleName.contains("prompt") || simpleName.contains("template")) {
            priority -= 40;
        }
        return priority;
    }

    private List<Method> promptLikeMethods(Class<?> candidate) {
        List<Method> methods = new ArrayList<Method>();
        for (Method method : candidate.getDeclaredMethods()) {
            if (method.getReturnType() != String.class) {
                continue;
            }
            String methodName = method.getName().toLowerCase(Locale.ROOT);
            if (methodName.contains("prompt")
                    || methodName.contains("build")
                    || methodName.contains("compose")
                    || methodName.contains("instruction")
                    || methodName.contains("template")
                    || methodName.contains("message")) {
                methods.add(method);
            }
        }

        Collections.sort(methods, Comparator.comparingInt(this::methodPriority).thenComparing(Method::getName));
        return methods;
    }

    private int methodPriority(Method method) {
        String methodName = method.getName().toLowerCase(Locale.ROOT);
        int priority = 100;
        if (methodName.contains("prompt")) {
            priority -= 50;
        }
        if (methodName.contains("build") || methodName.contains("compose")) {
            priority -= 20;
        }
        if (methodName.contains("instruction") || methodName.contains("template") || methodName.contains("message")) {
            priority -= 10;
        }
        return priority;
    }

    private boolean requiresInstance(List<Method> methods) {
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private Object createInstance(Class<?> candidate) throws Exception {
        Constructor<?>[] constructors = candidate.getDeclaredConstructors();
        Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));

        for (Constructor<?> constructor : constructors) {
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(buildConstructorArguments(constructor.getParameterTypes()));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        fail("Unable to instantiate candidate class for prompt/service verification: " + candidate.getName());
        return null;
    }

    private Object[] buildConstructorArguments(Class<?>[] parameterTypes) {
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            arguments[i] = defaultValue(parameterTypes[i]);
        }
        return arguments;
    }

    private String invokeWithVariants(Object targetInstance, Method method, String targetLanguage) {
        for (StringStrategy strategy : StringStrategy.values()) {
            try {
                Object[] arguments = buildMethodArguments(method, targetLanguage, strategy);
                method.setAccessible(true);
                Object result = method.invoke(Modifier.isStatic(method.getModifiers()) ? null : targetInstance, arguments);
                if (result instanceof String) {
                    String text = ((String) result).trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return null;
    }

    private Object[] buildMethodArguments(Method method, String targetLanguage, StringStrategy strategy) {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        int stringIndex = 0;
        int stringCount = countStringParameters(parameters);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if (parameterType == String.class || parameterType == CharSequence.class) {
                arguments[i] = resolveStringArgument(parameter, stringIndex, stringCount, targetLanguage, strategy);
                stringIndex++;
            } else if (MultipartFile.class.isAssignableFrom(parameterType)) {
                arguments[i] = new MockMultipartFile(
                        "file",
                        "entity-schema.json",
                        "application/json",
                        SAMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8)
                );
            } else if (parameterType == byte[].class) {
                arguments[i] = SAMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8);
            } else if (parameterType == InputStream.class) {
                arguments[i] = new ByteArrayInputStream(SAMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));
            } else if (Map.class.isAssignableFrom(parameterType)) {
                arguments[i] = Collections.emptyMap();
            } else if (Collection.class.isAssignableFrom(parameterType) || List.class.isAssignableFrom(parameterType)) {
                arguments[i] = Collections.emptyList();
            } else if (Set.class.isAssignableFrom(parameterType)) {
                arguments[i] = Collections.emptySet();
            } else if (parameterType.isEnum()) {
                arguments[i] = enumValue(parameterType, targetLanguage);
            } else {
                arguments[i] = defaultValue(parameterType);
            }
        }
        return arguments;
    }

    private int countStringParameters(Parameter[] parameters) {
        int count = 0;
        for (Parameter parameter : parameters) {
            Class<?> type = parameter.getType();
            if (type == String.class || type == CharSequence.class) {
                count++;
            }
        }
        return count;
    }

    private String resolveStringArgument(
            Parameter parameter,
            int stringIndex,
            int stringCount,
            String targetLanguage,
            StringStrategy strategy) {
        String parameterName = parameter.getName().toLowerCase(Locale.ROOT);
        if (parameterName.contains("language") || parameterName.contains("target")) {
            return targetLanguage;
        }
        if (parameterName.contains("schema")
                || parameterName.contains("content")
                || parameterName.contains("input")
                || parameterName.contains("json")
                || parameterName.contains("source")) {
            return SAMPLE_SCHEMA;
        }
        if (parameterName.contains("entity") || parameterName.contains("name")) {
            return SAMPLE_ENTITY_NAME;
        }

        switch (strategy) {
            case SCHEMA_THEN_LANGUAGE:
                return stringIndex == 0 ? SAMPLE_SCHEMA : targetLanguage;
            case LANGUAGE_THEN_SCHEMA:
                return stringIndex == 0 ? targetLanguage : SAMPLE_SCHEMA;
            case ALL_LANGUAGE:
                return targetLanguage;
            case ALL_SCHEMA_LAST_LANGUAGE:
            default:
                return stringIndex == stringCount - 1 ? targetLanguage : SAMPLE_SCHEMA;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object enumValue(Class<?> enumType, String targetLanguage) {
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        for (Object constant : constants) {
            if (((Enum) constant).name().equalsIgnoreCase(targetLanguage)) {
                return constant;
            }
        }
        return constants[0];
    }

    private Object defaultValue(Class<?> type) {
        if (type == String.class || type == CharSequence.class) {
            return "";
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) 0;
        }
        if (type == short.class || type == Short.class) {
            return (short) 0;
        }
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == long.class || type == Long.class) {
            return 0L;
        }
        if (type == float.class || type == Float.class) {
            return 0F;
        }
        if (type == double.class || type == Double.class) {
            return 0D;
        }
        if (type == char.class || type == Character.class) {
            return '\0';
        }
        if (type == byte[].class) {
            return new byte[0];
        }
        if (MultipartFile.class.isAssignableFrom(type)) {
            return new MockMultipartFile("file", new byte[0]);
        }
        if (InputStream.class.isAssignableFrom(type)) {
            return new ByteArrayInputStream(new byte[0]);
        }
        if (Map.class.isAssignableFrom(type)) {
            return Collections.emptyMap();
        }
        if (Collection.class.isAssignableFrom(type) || List.class.isAssignableFrom(type)) {
            return Collections.emptyList();
        }
        if (Set.class.isAssignableFrom(type)) {
            return Collections.emptySet();
        }
        if (type.isEnum()) {
            return enumValue(type, "java");
        }
        if (type.isInterface() || !Modifier.isFinal(type.getModifiers())) {
            return Mockito.mock(type, invocation -> defaultValue(invocation.getMethod().getReturnType()));
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private enum StringStrategy {
        SCHEMA_THEN_LANGUAGE,
        LANGUAGE_THEN_SCHEMA,
        ALL_SCHEMA_LAST_LANGUAGE,
        ALL_LANGUAGE
    }

    private static final class PromptOutputs {
        private final Class<?> sourceClass;
        private final Method sourceMethod;
        private final String javaOutput;
        private final String goOutput;

        private PromptOutputs(Class<?> sourceClass, Method sourceMethod, String javaOutput, String goOutput) {
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.javaOutput = javaOutput;
            this.goOutput = goOutput;
        }

        private String describe(String message) {
            return message
                    + " [class=" + sourceClass.getName()
                    + ", method=" + sourceMethod.getName()
                    + ", javaOutput=" + javaOutput
                    + ", goOutput=" + goOutput + "]";
        }
    }
}
