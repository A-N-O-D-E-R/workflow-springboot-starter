package com.anode.workflow.spring.autoconfigure.registry;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.spring.autoconfigure.util.BeanNameUtils;

public class WorkflowComponentFactoryRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String DEFAULT_BASE_PACKAGE = "com.anode";
    private static final String SCAN_PACKAGE_PROPERTY = "workflow.component-factory.scan-base-package";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();

            // Get base package from system property or use default
            String basePackage = System.getProperty(SCAN_PACKAGE_PROPERTY, DEFAULT_BASE_PACKAGE);
            String searchPath = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";

            // Scan for annotated classes with more specific scope
            var resources = resolver.getResources(searchPath);

            for (var resource : resources) {
                if (!resource.isReadable()) continue;

                var reader = readerFactory.getMetadataReader(resource);
                var anns = reader.getAnnotationMetadata().getAnnotationTypes();

                if (!anns.contains("com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory"))
                    continue;

                String className = reader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(className);

                // Must implement WorkflowComponantFactory
                if (!WorkflowComponantFactory.class.isAssignableFrom(clazz))
                    continue;

                GenericBeanDefinition bd = new GenericBeanDefinition();
                bd.setBeanClass(clazz);

                String beanName = BeanNameUtils.deriveBeanName(clazz);

                registry.registerBeanDefinition(beanName, bd);
            }

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to scan classpath for @WorkflowComponentFactory classes", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load @WorkflowComponentFactory class", e);
        }
    }
}
