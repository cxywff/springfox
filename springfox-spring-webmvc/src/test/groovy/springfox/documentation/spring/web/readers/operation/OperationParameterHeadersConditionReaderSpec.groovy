/*
 *
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package springfox.documentation.spring.web.readers.operation

import com.fasterxml.classmate.TypeResolver
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import spock.lang.Unroll
import springfox.documentation.builders.ModelSpecificationBuilder
import springfox.documentation.builders.SimpleParameterSpecificationBuilder
import springfox.documentation.schema.ScalarType
import springfox.documentation.service.Parameter
import springfox.documentation.service.ParameterSpecification
import springfox.documentation.service.RequestParameter
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.OperationContext
import springfox.documentation.spring.web.mixins.RequestMappingSupport
import springfox.documentation.spring.web.plugins.DocumentationContextSpec

class OperationParameterHeadersConditionReaderSpec extends DocumentationContextSpec implements RequestMappingSupport {

  OperationParameterHeadersConditionReader sut = new OperationParameterHeadersConditionReader(new TypeResolver())

  def "(Deprecated) Should read a parameter given a parameter request condition"() {
    given:
    HandlerMethod handlerMethod = dummyHandlerMethod('methodWithParameterRequestCondition')
    HeadersRequestCondition headersCondition = new HeadersRequestCondition("test=testValue")
    RequestMappingInfo requestMappingInfo = requestMappingInfo('/parameter-conditions',
        ["headersCondition": headersCondition])
    OperationContext operationContext = operationContext(
        documentationContext(),
        handlerMethod,
        0,
        requestMappingInfo)

    when:
    sut.apply(operationContext)
    def operation = operationContext.operationBuilder().build()

    then:
    sut.supports(DocumentationType.SPRING_WEB)
    sut.supports(DocumentationType.SWAGGER_12)
    sut.supports(DocumentationType.SWAGGER_2)
    sut.supports(DocumentationType.OAS_30)

    and:
    Parameter parameter = operation.parameters[0]
    parameter."$property" == expectedValue

    where:
    property        | expectedValue
    'name'          | 'test'
    'defaultValue'  | 'testValue'
    'description'   | null
    'required'      | true
    'allowMultiple' | false
    'paramType'     | "header"
  }

  @Unroll
  def "Should read a parameter given a parameter request condition for #property"() {
    given:
    HandlerMethod handlerMethod = dummyHandlerMethod('methodWithParameterRequestCondition')
    HeadersRequestCondition headersCondition = new HeadersRequestCondition("test=testValue")
    RequestMappingInfo requestMappingInfo = requestMappingInfo('/parameter-conditions',
        ["headersCondition": headersCondition])
    OperationContext operationContext = operationContext(
        documentationContext(),
        handlerMethod,
        0,
        requestMappingInfo)

    when:
    sut.apply(operationContext)
    def operation = operationContext.operationBuilder().build()

    then:
    sut.supports(DocumentationType.SPRING_WEB)
    sut.supports(DocumentationType.SWAGGER_12)
    sut.supports(DocumentationType.SWAGGER_2)
    sut.supports(DocumentationType.OAS_30)


    and:
    RequestParameter requestParameter = operation.requestParameters[0]
    requestParameter."$property" == expectedValue

    where:
    property                 | expectedValue
    'name'                   | 'test'
    'paramType'              | "header"
    'required'               | true
    'description'            | null
    'parameterSpecification' | simpleParameter()
  }

  private ParameterSpecification simpleParameter() {
    new ParameterSpecification(
        new SimpleParameterSpecificationBuilder(null)
            .style(null)
            .allowReserved(false)
            .defaultValue('testValue')
            .explode(false)
            .enumerationFacet {e ->
              e.allowedValues(['testValue'])
            }
            .model(
                new ModelSpecificationBuilder()
                    .scalarModel(ScalarType.STRING)
                    .name("test")
                    .build())
            .build(),
        null)
  }

  def "Should ignore a negated parameter in a parameter request condition"() {
    given:
    HandlerMethod handlerMethod = dummyHandlerMethod('methodWithParameterRequestCondition')
    HeadersRequestCondition headersCondition = new HeadersRequestCondition("!test")
    RequestMappingInfo requestMappingInfo = requestMappingInfo('/parameter-conditions',
        ["headersCondition": headersCondition])
    OperationContext operationContext = operationContext(documentationContext(), handlerMethod, 0, requestMappingInfo)

    when:
    sut.apply(operationContext)
    def operation = operationContext.operationBuilder().build()

    then:
    0 == operation.parameters.size()
    0 == operation.requestParameters.size()
  }

  def "Should ignore a parameter request condition expression that is already present in the parameters"() {
    given:
    HandlerMethod handlerMethod = dummyHandlerMethod('methodWithParameterRequestCondition')
    HeadersRequestCondition paramCondition = new HeadersRequestCondition("test=testValue", "test=3")
    OperationContext operationContext = operationContext(documentationContext(),
        handlerMethod,
        0,
        requestMappingInfo('/parameter-conditions', ["headersCondition": paramCondition]))

    when:
    sut.apply(operationContext)


    then:
    def built = operationContext.operationBuilder().build()
    1 == built.parameters.size()
    1 == built.requestParameters.size()
  }
}
