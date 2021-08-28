package com.carl.geek.common.generator;

import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.codegen.mybatis3.javamapper.JavaMapperGenerator;

import java.util.List;
import java.util.stream.Collectors;

public class ExtJavaMapperGenerator extends JavaMapperGenerator {

    @Override
    public List<CompilationUnit> getCompilationUnits() {
        List<CompilationUnit> compilationUnits = super.getCompilationUnits();
        return compilationUnits.stream()
                .filter(compilationUnit -> compilationUnit instanceof Interface)
                .peek(compilationUnit -> {
                    Interface anInterface = (Interface)compilationUnit;
                    anInterface.addAnnotation("@Repository");
                    anInterface.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Repository"));
                }).collect(Collectors.toList());
    }



}
