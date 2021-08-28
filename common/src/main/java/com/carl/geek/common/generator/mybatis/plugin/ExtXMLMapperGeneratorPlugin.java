package com.carl.geek.common.generator.mybatis.plugin;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.XmlConstants;
import org.mybatis.generator.codegen.mybatis3.IntrospectedTableMyBatis3Impl;

import java.util.ArrayList;
import java.util.List;

public class ExtXMLMapperGeneratorPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public List<GeneratedXmlFile> contextGenerateAdditionalXmlFiles(IntrospectedTable introspectedTable) {
        IntrospectedTableMyBatis3Impl introspectedTableMyBatis3 = (IntrospectedTableMyBatis3Impl) introspectedTable;
        List<GeneratedXmlFile> answer = new ArrayList<>();
        GeneratedXmlFile gxfExt = generatedExtXmlFile(introspectedTableMyBatis3);
        if (!Utils.checkFileExist(gxfExt)) {
            answer.add(gxfExt);
        }
        return answer;
    }

    private GeneratedXmlFile generatedExtXmlFile(IntrospectedTableMyBatis3Impl introspectedTable) {
        Document documentExt = getExtDocument(introspectedTable);
        String extFileName = introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "ExtMapperCamel.xml";
        return new GeneratedXmlFile(documentExt,
                extFileName, introspectedTable.getMyBatis3XmlMapperPackage(),
                context.getSqlMapGeneratorConfiguration().getTargetProject(),
                false, context.getXmlFormatter());
    }

    private Document getExtDocument(IntrospectedTableMyBatis3Impl introspectedTable) {
        Document document = new Document(
                XmlConstants.MYBATIS3_MAPPER_PUBLIC_ID,
                XmlConstants.MYBATIS3_MAPPER_SYSTEM_ID);
        document.setRootElement(getExtSqlMapElement(introspectedTable));
        return document;
    }

    private XmlElement getExtSqlMapElement(IntrospectedTableMyBatis3Impl introspectedTable) {
        XmlElement answer = new XmlElement("mapper");
        String namespace = introspectedTable.getMyBatis3SqlMapNamespace();
        answer.addAttribute(new Attribute("namespace", namespace + "Ext"));
        answer.addElement(new TextElement("\n"));
        return answer;
    }
}
