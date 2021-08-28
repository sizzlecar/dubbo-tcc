package com.carl.geek.common.generator;

import org.mybatis.generator.codegen.mybatis3.IntrospectedTableMyBatis3Impl;

/**
 * 通过设置 xml 配置文件中 targetRuntime属性 = "com.voyageone.retail.common.generator.ExtIntrospectedTable"
 * 可以将此类注入至 generator 上下文
 */
public class ExtIntrospectedTable extends IntrospectedTableMyBatis3Impl {



}
