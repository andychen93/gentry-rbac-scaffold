package com.gentry.rbac.user.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperXmlTest {

    @Test
    void mapperXml_statementIdsDoNotRedeclareBaseMapperOrAnnotatedMethods() throws Exception {
        Set<String> xmlStatementIds = xmlStatementIds();
        Set<String> reservedStatementIds = new HashSet<>();
        Arrays.stream(BaseMapper.class.getMethods())
                .map(Method::getName)
                .forEach(reservedStatementIds::add);
        Arrays.stream(UserMapper.class.getDeclaredMethods())
                .filter(UserMapperXmlTest::hasSqlAnnotation)
                .map(Method::getName)
                .forEach(reservedStatementIds::add);

        Set<String> duplicates = new HashSet<>(xmlStatementIds);
        duplicates.retainAll(reservedStatementIds);

        assertTrue(duplicates.isEmpty(),
                "UserMapper.xml must not redeclare BaseMapper or annotated statements: " + duplicates);
    }

    private static boolean hasSqlAnnotation(Method method) {
        return Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(type -> type == Select.class
                        || type == Insert.class
                        || type == Update.class
                        || type == Delete.class
                        || type == SelectProvider.class
                        || type == InsertProvider.class
                        || type == UpdateProvider.class
                        || type == DeleteProvider.class);
    }

    private static Set<String> xmlStatementIds() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document document = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(Files.readString(mapperXml()))));
        Set<String> statementIds = new HashSet<>();
        Element mapper = document.getDocumentElement();
        for (int i = 0; i < mapper.getChildNodes().getLength(); i++) {
            if (mapper.getChildNodes().item(i) instanceof Element element
                    && isStatementElement(element.getTagName())) {
                statementIds.add(element.getAttribute("id"));
            }
        }
        return statementIds;
    }

    private static boolean isStatementElement(String tagName) {
        return "select".equals(tagName)
                || "insert".equals(tagName)
                || "update".equals(tagName)
                || "delete".equals(tagName);
    }

    private static Path mapperXml() {
        Path rootPath = Path.of(System.getProperty("user.dir"));
        Path modulePath = rootPath.resolve("src/main/resources/mapper/UserMapper.xml");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return rootPath.resolve("backend/gentry-business/src/main/resources/mapper/UserMapper.xml");
    }
}
