/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.mapper.source;

import org.apache.lucene.document.Fieldable;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.MapperTests;
import org.elasticsearch.index.mapper.ParsedDocument;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author kimchy (shay.banon)
 */
public class DefaultSourceMappingTests {

    @Test public void testIncludeExclude() throws Exception {
        String mapping = XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("_source").field("includes", new String[]{"path1*"}).endObject()
                .endObject().endObject().string();

        DocumentMapper documentMapper = MapperTests.newParser().parse(mapping);

        ParsedDocument doc = documentMapper.parse("type", "1", XContentFactory.jsonBuilder().startObject()
                .startObject("path1").field("field1", "value1").endObject()
                .startObject("path2").field("field2", "value2").endObject()
                .endObject().copiedBytes());

        Fieldable sourceField = doc.rootDoc().getFieldable("_source");
        Map<String, Object> sourceAsMap = XContentFactory.xContent(XContentType.JSON).createParser(sourceField.getBinaryValue(), sourceField.getBinaryOffset(), sourceField.getBinaryLength()).mapAndClose();
        assertThat(sourceAsMap.containsKey("path1"), equalTo(true));
        assertThat(sourceAsMap.containsKey("path2"), equalTo(false));
    }

    @Test public void testDefaultMappingAndNoMapping() throws Exception {
        String defaultMapping = XContentFactory.jsonBuilder().startObject().startObject(MapperService.DEFAULT_MAPPING)
                .startObject("_source").field("enabled", false).endObject()
                .endObject().endObject().string();

        DocumentMapper mapper = MapperTests.newParser().parse("my_type", null, defaultMapping);
        assertThat(mapper.type(), equalTo("my_type"));
        assertThat(mapper.sourceMapper().enabled(), equalTo(false));

        try {
            mapper = MapperTests.newParser().parse(null, null, defaultMapping);
            assertThat(mapper.type(), equalTo("my_type"));
            assertThat(mapper.sourceMapper().enabled(), equalTo(false));
            assert false;
        } catch (MapperParsingException e) {
            // all is well
        }
    }

    @Test public void testDefaultMappingAndWithMappingOverride() throws Exception {
        String defaultMapping = XContentFactory.jsonBuilder().startObject().startObject(MapperService.DEFAULT_MAPPING)
                .startObject("_source").field("enabled", false).endObject()
                .endObject().endObject().string();

        String mapping = XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("_source").field("enabled", true).endObject()
                .endObject().endObject().string();

        DocumentMapper mapper = MapperTests.newParser().parse("my_type", mapping, defaultMapping);
        assertThat(mapper.type(), equalTo("my_type"));
        assertThat(mapper.sourceMapper().enabled(), equalTo(true));
    }

    @Test public void testDefaultMappingAndNoMappingWithMapperService() throws Exception {
        String defaultMapping = XContentFactory.jsonBuilder().startObject().startObject(MapperService.DEFAULT_MAPPING)
                .startObject("_source").field("enabled", false).endObject()
                .endObject().endObject().string();

        MapperService mapperService = MapperTests.newMapperService();
        mapperService.add(MapperService.DEFAULT_MAPPING, defaultMapping);

        DocumentMapper mapper = mapperService.documentMapperWithAutoCreate("my_type");
        assertThat(mapper.type(), equalTo("my_type"));
        assertThat(mapper.sourceMapper().enabled(), equalTo(false));
    }

    @Test public void testDefaultMappingAndWithMappingOverrideWithMapperService() throws Exception {
        String defaultMapping = XContentFactory.jsonBuilder().startObject().startObject(MapperService.DEFAULT_MAPPING)
                .startObject("_source").field("enabled", false).endObject()
                .endObject().endObject().string();

        MapperService mapperService = MapperTests.newMapperService();
        mapperService.add(MapperService.DEFAULT_MAPPING, defaultMapping);

        String mapping = XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("_source").field("enabled", true).endObject()
                .endObject().endObject().string();
        mapperService.add("my_type", mapping);

        DocumentMapper mapper = mapperService.documentMapper("my_type");
        assertThat(mapper.type(), equalTo("my_type"));
        assertThat(mapper.sourceMapper().enabled(), equalTo(true));
    }
}
